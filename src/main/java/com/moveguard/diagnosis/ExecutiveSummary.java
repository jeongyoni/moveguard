package com.moveguard.diagnosis;

import com.moveguard.diagnosis.DiagnosisPage.FactorSummary;
import com.moveguard.project.Project;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 진단 결과를 고객 보고용 "경영 요약" 문장으로 작성한다.
 * 기본은 로컬 LLM(Ollama)로 자연스러운 서술을 생성하고, LLM이 미가용·타임아웃·오류일 때는
 * 규칙·점수 기반으로 결정적으로 생성한다. 어느 경로든 외부 AI API는 호출하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ExecutiveSummary {

    private final LlmClient llm;

    public record Summary(String grade, String headline, List<String> body, List<String> actions, boolean llm) {
    }

    public Summary generate(Project project, DiagnosisResult result, List<FactorSummary> factors) {
        Summary base = ruleBased(project, result, factors);
        if (llm.isEnabled()) {
            Optional<String> text = llm.generate(buildPrompt(project, result, factors));
            if (text.isPresent()) {
                List<String> body = Arrays.stream(text.get().split("\\n+"))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList();
                if (!body.isEmpty()) {
                    // 사실(헤드라인·권고 조치)은 규칙 기반 유지, 서술 본문만 LLM 생성
                    return new Summary(base.grade(), base.headline(), body, base.actions(), true);
                }
            }
        }
        return base;
    }

    /** LLM 프롬프트 — 위 수치를 그대로 쓰도록 사실을 명시. */
    private String buildPrompt(Project project, DiagnosisResult result, List<FactorSummary> factors) {
        String gradeKo = gradeKo(result.riskLevel().name());
        long urgent = result.findings().stream().filter(DiagnosisResult.Item::blocking).count();
        StringBuilder p = new StringBuilder();
        p.append("당신은 시스원 MSP 사업부의 기술 컨설턴트입니다. ")
                .append("아래 운영 환경 자동 진단 결과를 바탕으로 고객 보고용 '경영 요약'을 작성하세요.\n\n");
        p.append("- 환경: ").append(project.getName())
                .append(" (고객 ").append(project.getCustomerName())
                .append(", ").append(project.getTargetEnv()).append(")\n");
        p.append("- 종합 위험 등급: ").append(gradeKo).append("\n");
        p.append("- 발견 리스크: 총 ").append(result.findings().size())
                .append("건 (긴급 ").append(urgent).append("건), 최고 위험점수 RPN ")
                .append(result.maxRpn()).append("\n");
        if (!factors.isEmpty()) {
            p.append("- 영역별: ").append(factors.stream()
                    .map(f -> "%s %d건(최고 RPN %d)".formatted(f.name(), f.count(), f.maxRpn()))
                    .collect(Collectors.joining(", "))).append("\n");
        }
        p.append("\n주요 발견 항목(위험 순):\n");
        result.findings().stream().limit(6).forEach(it ->
                p.append("- (").append(it.ruleCode()).append(", RPN ").append(it.rpn())
                        .append(", ").append(it.factorName()).append(it.blocking() ? ", 긴급" : "")
                        .append(") ").append(it.title()).append(": ").append(it.message()).append("\n"));
        p.append("\n지침:\n");
        p.append("- 3~4문장의 자연스러운 한국어 서술(문단)으로만 작성. 마크다운·제목·불릿·번호 금지.\n");
        p.append("- 가장 시급한 위험과 그것이 왜 위험한지, 방치 시 영향을 설명.\n");
        p.append("- 발견된 리스크는 시스원 MSP의 운영·보안·백업·DR 서비스로 해결 가능함을 언급하며 마무리.\n");
        p.append("- 과장 없이 사실 기반으로, 숫자는 위 수치를 그대로 사용. 인사말·서론 없이 본론만.\n");
        return p.toString();
    }

    private static String gradeKo(String level) {
        return switch (level) {
            case "HIGH" -> "높음 (즉시 조치 필요)";
            case "MEDIUM" -> "보통 (조치 권고)";
            default -> "양호";
        };
    }

    /** 규칙·점수 기반 결정적 생성 (LLM 폴백). */
    private Summary ruleBased(Project project, DiagnosisResult result, List<FactorSummary> factors) {
        String gradeKo = gradeKo(result.riskLevel().name());
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
        return new Summary(gradeKo, headline, body, actions, false);
    }
}
