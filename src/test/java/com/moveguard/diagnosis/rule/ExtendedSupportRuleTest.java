package com.moveguard.diagnosis.rule;

import static com.moveguard.asset.Phase.AFTER;
import static com.moveguard.diagnosis.rule.Fixtures.DB;
import static com.moveguard.diagnosis.rule.Fixtures.PLANNED_DATE;
import static com.moveguard.diagnosis.rule.Fixtures.compatContext;
import static com.moveguard.diagnosis.rule.Fixtures.releaseExt;
import static com.moveguard.diagnosis.rule.Fixtures.sw;
import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.diagnosis.Finding;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExtendedSupportRuleTest {

    private final ExtendedSupportRule rule = new ExtendedSupportRule();

    @Test
    @DisplayName("활성 지원은 끝났지만 연장 지원 구간이면 CMP-06 발견")
    void detectsExtendedSupportWindow() {
        List<Finding> findings = rule.evaluate(compatContext(
                List.of(sw(DB, "oracle-database", "19.0.0", "19", AFTER)),
                List.of(releaseExt("oracle-database", "19", true,
                        LocalDate.of(2024, 4, 30), LocalDate.of(2027, 4, 30))),
                PLANNED_DATE));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).params())
                .containsEntry("asset", "db01")
                .containsEntry("product", "oracle-database")
                .containsEntry("version", "19.0.0")
                .containsEntry("extSupport", "2027-04-30");
    }

    @Test
    @DisplayName("연장 지원까지 끝난(완전 EOL) 버전은 해당 없음 (CMP-01 소관)")
    void ignoresFullyEol() {
        assertThat(rule.evaluate(compatContext(
                List.of(sw(DB, "oracle-database", "12.2.0", "12.2", AFTER)),
                List.of(releaseExt("oracle-database", "12.2", true,
                        LocalDate.of(2020, 3, 31), LocalDate.of(2022, 3, 31))),
                PLANNED_DATE))).isEmpty();
    }

    @Test
    @DisplayName("활성 지원 중인 버전은 해당 없음")
    void ignoresActivelyMaintained() {
        assertThat(rule.evaluate(compatContext(
                List.of(sw(DB, "oracle-database", "19.0.0", "19", AFTER)),
                List.of(releaseExt("oracle-database", "19", false,
                        LocalDate.of(2029, 12, 31), LocalDate.of(2032, 12, 31))),
                PLANNED_DATE))).isEmpty();
    }

    @Test
    @DisplayName("전환 예정일이 없으면 해당 없음")
    void ignoresWhenNoPlannedDate() {
        assertThat(rule.evaluate(compatContext(
                List.of(sw(DB, "oracle-database", "19.0.0", "19", AFTER)),
                List.of(releaseExt("oracle-database", "19", true,
                        LocalDate.of(2024, 4, 30), LocalDate.of(2027, 4, 30))),
                null))).isEmpty();
    }
}
