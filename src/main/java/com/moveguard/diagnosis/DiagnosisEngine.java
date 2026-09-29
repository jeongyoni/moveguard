package com.moveguard.diagnosis;

import com.moveguard.diagnosis.RiskScorer.RiskScore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 진단 컨텍스트에 규칙을 적용해 위험 판정을 산출한다 (저장·조회와 분리).
 * 실사업 진단(DiagnosisService)과 시뮬레이션이 공유한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiagnosisEngine {

    private final List<RiskRuleEvaluator> evaluators;
    private final RiskScorer riskScorer;

    /** 평가 결과(RPN 내림차순 정렬된 findings)와 산정된 위험 점수 */
    public record Assessment(List<EvaluatedFinding> findings, RiskScore score) {
    }

    public Assessment assess(DiagnosisContext context, Map<String, RuleDefinition> rules) {
        List<EvaluatedFinding> evaluated = new ArrayList<>();
        for (RiskRuleEvaluator evaluator : evaluators) {
            RuleDefinition rule = rules.get(evaluator.ruleCode());
            if (rule == null) {
                log.warn("미등록 또는 비활성 규칙이라 건너뜀: {}", evaluator.ruleCode());
                continue;
            }
            for (Finding finding : evaluator.evaluate(context)) {
                evaluated.add(new EvaluatedFinding(finding, rule, rule.renderMessage(finding.params())));
            }
        }
        evaluated.sort(Comparator.comparingInt(EvaluatedFinding::rpn).reversed());

        return new Assessment(evaluated, riskScorer.score(evaluated));
    }
}
