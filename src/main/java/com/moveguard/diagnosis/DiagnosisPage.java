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
    private final ExecutiveSummary executiveSummary;

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
        model.addAttribute("exec", executiveSummary.generate(project, result, factors));
        model.addAttribute("groups", groupFindings(result.findings()));
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

    /** 같은 규칙이 여러 자산에서 발견되면 하나로 묶는다(예: EOL이 서버 여럿). findings는 RPN 내림차순. */
    public static List<FindingGroup> groupFindings(List<DiagnosisResult.Item> findings) {
        Map<String, List<DiagnosisResult.Item>> byKey = new LinkedHashMap<>();
        for (DiagnosisResult.Item item : findings) {
            byKey.computeIfAbsent(item.ruleCode() + "|" + item.title(), k -> new ArrayList<>()).add(item);
        }
        List<FindingGroup> groups = new ArrayList<>();
        for (List<DiagnosisResult.Item> items : byKey.values()) {
            DiagnosisResult.Item first = items.get(0);
            int maxRpn = items.stream().mapToInt(DiagnosisResult.Item::rpn).max().orElse(first.rpn());
            groups.add(new FindingGroup(first.ruleCode(), first.factorCode(), first.factorName(),
                    first.title(), first.mitigation(), first.blocking(), maxRpn, items.size(),
                    items.stream().map(DiagnosisResult.Item::message).toList()));
        }
        return groups;
    }

    public record FindingGroup(String ruleCode, String factorCode, String factorName, String title,
                               String mitigation, boolean blocking, int rpn, int count,
                               List<String> messages) {
    }
}
