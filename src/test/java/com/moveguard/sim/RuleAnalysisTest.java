package com.moveguard.sim;

import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.sim.RuleAnalysis.Observation;
import com.moveguard.sim.RuleAnalysis.RuleStat;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RuleAnalysisTest {

    @Test
    @DisplayName("혼동행렬과 정밀도·재현율·F1·정확도를 계산한다")
    void computesConfusionMatrixAndMetrics() {
        // TP=2, FP=1, FN=1, TN=1
        List<Observation> obs = List.of(
                new Observation(true, true, List.of()),   // TP
                new Observation(true, true, List.of()),   // TP
                new Observation(true, false, List.of()),  // FP
                new Observation(false, true, List.of()),  // FN
                new Observation(false, false, List.of())  // TN
        );

        RuleAnalysis a = RuleAnalysis.compute(obs);

        assertThat(a.total()).isEqualTo(5);
        assertThat(a.truePositive()).isEqualTo(2);
        assertThat(a.falsePositive()).isEqualTo(1);
        assertThat(a.falseNegative()).isEqualTo(1);
        assertThat(a.trueNegative()).isEqualTo(1);
        assertThat(a.precision()).isEqualTo(0.6667); // 2/3
        assertThat(a.recall()).isEqualTo(0.6667);    // 2/3
        assertThat(a.f1()).isEqualTo(0.6667);
        assertThat(a.accuracy()).isEqualTo(0.6);      // 3/5
    }

    @Test
    @DisplayName("규칙별 발생 횟수와 실패율을 실패율 내림차순으로 낸다")
    void computesPerRuleFailRate() {
        List<Observation> obs = List.of(
                new Observation(true, true, List.of("A", "B")),   // A,B fired, FAIL
                new Observation(true, false, List.of("A")),        // A fired, SUCCESS
                new Observation(true, true, List.of("B"))          // B fired, FAIL
        );

        List<RuleStat> stats = RuleAnalysis.compute(obs).ruleStats();

        // A: fired 2, fail 1 → 0.5 / B: fired 2, fail 2 → 1.0 → B가 앞
        assertThat(stats).extracting(RuleStat::code).containsExactly("B", "A");
        assertThat(stats.get(0).failRate()).isEqualTo(1.0);
        assertThat(stats.get(1).failRate()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("분모가 0이면 지표는 0으로 안전하게 처리한다")
    void handlesEmptyGracefully() {
        RuleAnalysis a = RuleAnalysis.compute(List.of());
        assertThat(a.total()).isZero();
        assertThat(a.precision()).isZero();
        assertThat(a.f1()).isZero();
    }
}
