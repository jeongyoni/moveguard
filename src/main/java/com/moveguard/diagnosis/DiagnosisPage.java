package com.moveguard.diagnosis;

import com.moveguard.project.Project;
import com.moveguard.project.ProjectService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * 진단 결과 화면(diagnosis.html)의 모델을 구성한다.
 * 폼 진단(DiagnosisViewController)과 엑셀 업로드 후 진단이 같은 화면을 공유한다.
 */
@Component
@RequiredArgsConstructor
public class DiagnosisPage {

    /** 핵심 점검(전환이 직접 중단되는 위험). 나머지는 확장 점검(보안·운영 안정성). */
    public static final Set<String> CORE_FACTORS = Set.of("NETWORK_IP", "COMPAT");

    public static boolean isCore(String factorCode) {
        return CORE_FACTORS.contains(factorCode);
    }

    private final ProjectService projectService;
    private final DiagnosisService diagnosisService;
    private final DiagnosisContextLoader contextLoader;

    /** 진단을 실행하고 모델을 채운다. 성공 시 "diagnosis", 실패 시 "redirect:/". */
    public String render(Long projectId, Model model) {
        Project project = projectService.findProject(projectId).orElse(null);
        if (project == null) {
            return "redirect:/";
        }
        DiagnosisResult result = diagnosisService.diagnose(projectId).orElse(null);
        if (result == null) {
            return "redirect:/";
        }

        List<FactorSummary> factors = summarize(result.findings());
        model.addAttribute("project", project);
        model.addAttribute("result", result);
        model.addAttribute("factors", factors);
        model.addAttribute("coreFactors", factors.stream().filter(f -> isCore(f.code())).toList());
        model.addAttribute("extFactors", factors.stream().filter(f -> !isCore(f.code())).toList());
        model.addAttribute("input", DiagnosisInput.of(contextLoader.load(projectId)));
        return "diagnosis";
    }

    /** 위험요인별 발견 건수와 최고 RPN (findings는 RPN 내림차순). */
    public static List<FactorSummary> summarize(List<DiagnosisResult.Item> findings) {
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
