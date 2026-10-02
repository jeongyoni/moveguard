package com.moveguard.diagnosis;

import com.moveguard.project.Project;
import com.moveguard.project.ProjectService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final DiagnosisService diagnosisService;
    private final DiagnosisContextLoader contextLoader;

    @GetMapping("/")
    public String projects(Model model) {
        model.addAttribute("projects", projectService.findProjects());
        return "index";
    }

    @PostMapping("/projects/{projectId}/diagnose")
    public String diagnose(@PathVariable Long projectId, Model model) {
        Project project = projectService.findProject(projectId).orElse(null);
        if (project == null) {
            return "redirect:/";
        }
        DiagnosisResult result = diagnosisService.diagnose(projectId).orElse(null);
        if (result == null) {
            return "redirect:/";
        }

        model.addAttribute("project", project);
        model.addAttribute("result", result);
        model.addAttribute("factors", summarize(result.findings()));
        // "무엇을 보고 판단했는지"를 같이 보여준다 — 결과만으로는 근거가 안 보인다
        model.addAttribute("input", DiagnosisInput.of(contextLoader.load(projectId)));
        return "diagnosis";
    }

    /**
     * 위험요인별 발견 건수와 최고 RPN — 어느 영역이 위험한지 한눈에 보기 위한 집계.
     * findings가 RPN 내림차순이므로, 요인도 가장 위험한 것부터 나온다.
     */
    private List<FactorSummary> summarize(List<DiagnosisResult.Item> findings) {
        Map<String, FactorSummary> byCode = new LinkedHashMap<>();
        for (DiagnosisResult.Item item : findings) {
            FactorSummary prev = byCode.get(item.factorCode());
            byCode.put(item.factorCode(), prev == null
                    ? new FactorSummary(item.factorCode(), item.factorName(), 1, item.rpn())
                    : new FactorSummary(prev.code(), prev.name(), prev.count() + 1,
                            Math.max(prev.maxRpn(), item.rpn())));
        }
        return new ArrayList<>(byCode.values());
    }

    public record FactorSummary(String code, String name, int count, int maxRpn) {
    }
}
