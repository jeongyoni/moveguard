package com.moveguard.diagnosis;

import com.moveguard.project.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 진단 결과를 사람이 보는 화면. 시연·발표에서 "무엇이 왜 위험한지"를 보여주는 용도다.
 * 같은 결과의 JSON은 {@link DiagnosisController}가 제공한다.
 */
@Controller
@RequiredArgsConstructor
public class DiagnosisViewController {

    private final ProjectService projectService;
    private final DiagnosisPage diagnosisPage;
    private final DashboardService dashboardService;
    private final DiagnosisMapper diagnosisMapper;

    @GetMapping("/")
    public String projects(Model model) {
        model.addAttribute("dash", dashboardService.build());
        return "index";
    }

    @PostMapping("/projects/{projectId}/diagnose")
    public String diagnose(@PathVariable Long projectId, Model model) {
        return diagnosisPage.render(projectId, model);
    }

    /** 진단 이력·추이 화면 */
    @GetMapping("/projects/{projectId}/history")
    public String history(@PathVariable Long projectId, Model model) {
        var project = projectService.findProject(projectId).orElse(null);
        if (project == null) {
            return "redirect:/";
        }
        model.addAttribute("project", project);
        model.addAttribute("runs", diagnosisMapper.findRunsByProject(projectId));
        return "history";
    }
}
