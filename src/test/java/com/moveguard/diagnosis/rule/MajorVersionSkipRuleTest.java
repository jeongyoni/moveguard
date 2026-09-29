package com.moveguard.diagnosis.rule;

import static com.moveguard.asset.Phase.AFTER;
import static com.moveguard.asset.Phase.BEFORE;
import static com.moveguard.diagnosis.rule.Fixtures.DB;
import static com.moveguard.diagnosis.rule.Fixtures.PLANNED_DATE;
import static com.moveguard.diagnosis.rule.Fixtures.compatContext;
import static com.moveguard.diagnosis.rule.Fixtures.sw;
import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.diagnosis.Finding;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MajorVersionSkipRuleTest {

    private final MajorVersionSkipRule rule = new MajorVersionSkipRule();

    @Test
    @DisplayName("MySQL 5 → 9 처럼 메이저를 2단계 이상 건너뛰면 CMP-05 발견")
    void detectsMajorSkip() {
        List<Finding> findings = rule.evaluate(compatContext(
                List.of(sw(DB, "mysql", "5.7.44", "5.7", BEFORE),
                        sw(DB, "mysql", "9.6.1", "9.6", AFTER)),
                List.of(), PLANNED_DATE));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).params())
                .containsEntry("product", "mysql")
                .containsEntry("from", "5.7.44")
                .containsEntry("to", "9.6.1");
    }

    @Test
    @DisplayName("한 단계 업그레이드(8 → 9)는 해당 없음")
    void ignoresSingleMajor() {
        assertThat(rule.evaluate(compatContext(
                List.of(sw(DB, "rocky-linux", "8.9", "8", BEFORE),
                        sw(DB, "rocky-linux", "9.4", "9", AFTER)),
                List.of(), PLANNED_DATE))).isEmpty();
    }

    @Test
    @DisplayName("이전 전 정보가 없으면 해당 없음")
    void ignoresWhenNoBefore() {
        assertThat(rule.evaluate(compatContext(
                List.of(sw(DB, "mysql", "9.6.1", "9.6", AFTER)),
                List.of(), PLANNED_DATE))).isEmpty();
    }
}
