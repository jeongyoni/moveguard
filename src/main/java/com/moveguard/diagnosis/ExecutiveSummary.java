package com.moveguard.diagnosis;

import com.moveguard.diagnosis.DiagnosisPage.FactorSummary;
import com.moveguard.project.Project;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 진단 결과를 고객 보고용 "경영 요약" 문장으로 작성한다.
 * 규칙·점수 기반으로 결정적으로 생성하므로 외부 AI를 호출하지 않고,
 * 사내 LLM이 연결되면 같은 입력으로 문장만 다듬도록 확장할 수 있다.
 */
@Component
public class ExecutiveSummary {

    public record Summary(String grade, String headline, List<String> body, List<String> actions) {
    }

    public Summary generate(Project project, DiagnosisResult result, List<FactorSummary> factors) {
        String level = result.riskLevel().name();
        String gradeKo = switch (level) {
            case "HIGH" -> "높음 (즉시 조치 필요)";
            case "MEDIUM" -> "보통 (조치 권고)";
            default -> "양호";
        };
        int total = result.findings().size();
        long urgent = result.findings().stream().filter(DiagnosisResult.Item::blocking).count();

        String headline = "%s 운영 환경의 종합 위험 등급은 '%s'이며, 총 %d건의 리스크(긴급 %d건)가 발견되었습니다."
                .formatted(project.getName(), gradeKo, total, urgent);

        List<String> body = new ArrayList<>();
        if (!result.findings().isEmpty()) {
            DiagnosisResult.Item top = result.findings().get(0);
            body.add("가장 시급한 항목은 '%s'(%s · RPN %d)로, %s"
                    .formatted(top.title(), top.factorName(), top.rpn(), top.message()));
        }
        if (!factors.isEmpty()) {
            String breakdown = factors.stream()
                    .map(f -> "%s %d건(최고 RPN %d)".formatted(f.name(), f.count(), f.maxRpn()))
                    .collect(Collectors.joining(", "));
            body.add("영역별로는 " + breakdown + " 입니다.");
        }
        if (result.previous() != null) {
            int prev = result.previous().getMaxRpn();
            int now = result.maxRpn();
            if (now < prev) {
                body.add("직전 진단 대비 최고 위험점수가 %d에서 %d로 낮아져 조치 효과가 확인됩니다.".formatted(prev, now));
            } else if (now > prev) {
                body.add("직전 진단 대비 최고 위험점수가 %d에서 %d로 높아져 리스크가 악화되었습니다.".formatted(prev, now));
            }
        }
        if (result.blocked()) {
            body.add("즉시 조치가 필요한 긴급 항목이 있어, 선제 조치 없이는 보안·장애 리스크에 노출됩니다.");
        } else {
            body.add("현재 즉시 조치가 필요한 긴급 항목은 없으나, 지속적인 운영 점검을 권고합니다.");
        }

        List<String> actions = new ArrayList<>();
        boolean compat = factors.stream().anyMatch(f -> "COMPAT".equals(f.code()));
        boolean security = factors.stream().anyMatch(f -> "SECURITY".equals(f.code()));
        boolean backup = factors.stream().anyMatch(f -> "BACKUP".equals(f.code()));
        if (compat) {
            actions.add("지원 종료(EOL) OS·DB·드라이버를 지원 버전으로 업그레이드 — MSP 패치·업그레이드·운영대행");
        }
        if (security) {
            actions.add("인증서 갱신·포트 노출 차단 등 보안 조치 — MSP 보안·운영 서비스");
        }
        if (backup) {
            actions.add("정기 복구 테스트·오프사이트·DR 구성 — MSP 백업·DR 서비스");
        }
        if (actions.isEmpty()) {
            actions.add("현 수준 유지를 위한 정기 운영 점검 — MSP 운영대행");
        }
        return new Summary(gradeKo, headline, body, actions);
    }
}
