package com.moveguard.diagnosis;

import java.util.Map;

/**
 * 규칙 판정 결과 1건.
 * params는 risk_rule.message_template의 {키} 치환에 사용한다.
 */
public record Finding(
        String ruleCode,
        Long assetId,
        Long dependencyId,
        Map<String, String> params) {
}
