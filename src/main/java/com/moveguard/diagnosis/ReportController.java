package com.moveguard.diagnosis;

import com.moveguard.project.Project;
import com.moveguard.project.ProjectService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** 제안서 첨부용 사전 위험진단 리포트 (인쇄/PDF 저장). 저장 없이 현재 구성으로 계산한다. */
@Controller
@RequiredArgsConstructor
public class ReportController {

    private final ProjectService projectService;
    private final DiagnosisService diagnosisService;

    @GetMapping("/projects/{projectId}/report")
    public String report(@PathVariable Long projectId, Model model) {
        Project project = projectService.findProject(projectId).orElse(null);
        if (project == null) {
            return "redirect:/";
        }
        DiagnosisResult result = diagnosisService.preview(projectId).orElse(null);
        if (result == null) {
            return "redirect:/projects/" + projectId;
        }

        var factors = DiagnosisPage.summarize(result.findings());
        int maxFactorRpn = factors.stream()
                .mapToInt(DiagnosisPage.FactorSummary::maxRpn).max().orElse(1);

        model.addAttribute("project", project);
        model.addAttribute("result", result);
        model.addAttribute("factors", factors);
        model.addAttribute("maxFactorRpn", maxFactorRpn);
        model.addAttribute("reportDate", LocalDate.now());
        return "report";
    }
}
