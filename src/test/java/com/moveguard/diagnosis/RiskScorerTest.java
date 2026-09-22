package com.moveguard.diagnosis;

import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.diagnosis.RiskScorer.RiskScore;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RiskScorerTest {

    private final RiskScorer scorer = new RiskScorer();

    @Test
    @DisplayName("RPN 432 차단 규칙 1건 → HIGH, 전환 차단, 종합 15.12")
    void highAndBlocked() {
        RiskScore score = scorer.score(List.of(
                finding("IP-01", "NETWORK_IP", "0.350", 9, 8, 6, true)));

        assertThat(score.level()).isEqualTo(RiskLevel.HIGH);
        assertThat(score.blocked()).isTrue();
        assertThat(score.maxRpn()).isEqualTo(432);
        assertThat(score.totalScore()).isEqualByComparingTo("15.12");
    }

    @Test
    @DisplayName("RPN 180 비차단 규칙 → MEDIUM, 차단 없음")
    void mediumNotBlocked() {
        RiskScore score = scorer.score(List.of(
                finding("IP-02", "NETWORK_IP", "0.350", 6, 6, 5, false)));

        assertThat(score.level()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(score.blocked()).isFalse();
    }

    @Test
    @DisplayName("같은 요인은 최고 RPN만 반영하고, 요인별 가중치로 합산")
    void usesWorstRpnPerFactor() {
        RiskScore score = scorer.score(List.of(
                finding("IP-01", "NETWORK_IP", "0.350", 9, 8, 6, true),
                finding("IP-03", "NETWORK_IP", "0.350", 5, 7, 6, false),
                finding("DNS-01", "DNS", "0.150", 6, 7, 5, false)));

        // 432/10 x 0.35 + 210/10 x 0.15 = 15.12 + 3.15
        assertThat(score.totalScore()).isEqualByComparingTo("18.27");
    }

    @Test
    @DisplayName("발견 항목이 없으면 LOW, 0점, 차단 없음")
    void emptyIsLow() {
        RiskScore score = scorer.score(List.of());

        assertThat(score.level()).isEqualTo(RiskLevel.LOW);
        assertThat(score.blocked()).isFalse();
        assertThat(score.totalScore()).isEqualByComparingTo("0");
    }

    private static EvaluatedFinding finding(String code, String factor, String weight,
                                            int s, int o, int d, boolean blocking) {
        RuleDefinition rule = new RuleDefinition(1L, code, factor, new BigDecimal(weight),
                code, s, o, d, blocking, "", "");
        return new EvaluatedFinding(new Finding(code, null, null, Map.of()), rule, "");
    }
}
