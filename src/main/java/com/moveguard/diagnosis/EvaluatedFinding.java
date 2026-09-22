package com.moveguard.diagnosis;

/** 규칙 판정 결과에 규칙 정의와 완성된 피드백 문구를 결합한 것 */
public record EvaluatedFinding(Finding finding, RuleDefinition rule, String message) {

    public int rpn() {
        return rule.rpn();
    }
}
