package com.moveguard.diagnosis;

import java.util.List;

/**
 * 진단 규칙의 공통 인터페이스.
 * 구현체에 @Component를 붙이면 진단 서비스가 자동으로 수집한다.
 */
public interface RiskRuleEvaluator {

    /** risk_rule.rule_code와 일치해야 한다 */
    String ruleCode();

    List<Finding> evaluate(DiagnosisContext context);
}
