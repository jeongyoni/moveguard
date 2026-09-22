package com.moveguard.diagnosis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 위험 등급: 최고 RPN 기준 (가중 평균으로 정하면 치명적 항목이 희석됨)
 * 종합 점수: 요인별 최고 RPN을 0~100으로 환산해 요인 가중치로 합산 (재진단 추이 비교용)
 * 전환 차단: 차단 규칙이 1건 이상이면 등급과 무관하게 차단
 */
@Component
public class RiskScorer {

    static final int HIGH_RPN = 400;
    static final int MEDIUM_RPN = 150;

    public RiskScore score(List<EvaluatedFinding> findings) {
        int maxRpn = findings.stream().mapToInt(EvaluatedFinding::rpn).max().orElse(0);
        boolean blocked = findings.stream().anyMatch(f -> f.rule().isBlocking());

        Map<String, Integer> worstRpnByFactor = new HashMap<>();
        Map<String, BigDecimal> weightByFactor = new HashMap<>();
        for (EvaluatedFinding f : findings) {
            String factor = f.rule().getFactorCode();
            worstRpnByFactor.merge(factor, f.rpn(), Math::max);
            weightByFactor.putIfAbsent(factor, f.rule().getFactorWeight());
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> entry : worstRpnByFactor.entrySet()) {
            BigDecimal factorScore = BigDecimal.valueOf(entry.getValue()).divide(BigDecimal.TEN);
            total = total.add(factorScore.multiply(weightByFactor.get(entry.getKey())));
        }

        return new RiskScore(total.setScale(2, RoundingMode.HALF_UP), levelOf(maxRpn), blocked, maxRpn);
    }

    static RiskLevel levelOf(int maxRpn) {
        if (maxRpn >= HIGH_RPN) {
            return RiskLevel.HIGH;
        }
        if (maxRpn >= MEDIUM_RPN) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    public record RiskScore(BigDecimal totalScore, RiskLevel level, boolean blocked, int maxRpn) {
    }
}
