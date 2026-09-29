package com.moveguard.diagnosis.rule;

import static com.moveguard.asset.Phase.AFTER;
import static com.moveguard.asset.Phase.BEFORE;
import static com.moveguard.diagnosis.rule.Fixtures.DB;
import static com.moveguard.diagnosis.rule.Fixtures.PLANNED_DATE;
import static com.moveguard.diagnosis.rule.Fixtures.compatContext;
import static com.moveguard.diagnosis.rule.Fixtures.release;
import static com.moveguard.diagnosis.rule.Fixtures.sw;
import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.diagnosis.Finding;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EolAfterVersionRuleTest {

    private final EolAfterVersionRule rule = new EolAfterVersionRule();

    @Test
    @DisplayName("이전 후 버전이 EOL이면 CMP-01 발견")
    void detectsEolAfterVersion() {
        List<Finding> findings = rule.evaluate(compatContext(
                List.of(sw(DB, "mysql", "9.6.1", "9.6", AFTER)),
                List.of(release("mysql", "9.6", true, LocalDate.of(2026, 4, 21), null)),
                PLANNED_DATE));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).params())
                .containsEntry("asset", "db01")
                .containsEntry("product", "mysql")
                .containsEntry("version", "9.6.1")
                .containsEntry("eol", "2026-04-21");
    }

    @Test
    @DisplayName("이전 후 버전이 지원 중이면 해당 없음")
    void ignoresMaintainedVersion() {
        assertThat(rule.evaluate(compatContext(
                List.of(sw(DB, "mysql", "8.4.0", "8.4", AFTER)),
                List.of(release("mysql", "8.4", false, LocalDate.of(2032, 4, 30), null)),
                PLANNED_DATE))).isEmpty();
    }

    @Test
    @DisplayName("이전 전(BEFORE) 버전은 검사 대상이 아니다")
    void ignoresBeforePhase() {
        assertThat(rule.evaluate(compatContext(
                List.of(sw(DB, "mysql", "5.7.44", "5.7", BEFORE)),
                List.of(release("mysql", "5.7", true, LocalDate.of(2023, 10, 31), null)),
                PLANNED_DATE))).isEmpty();
    }

    @Test
    @DisplayName("기준 데이터가 없는 제품은 해당 없음")
    void ignoresWhenNoReleaseData() {
        assertThat(rule.evaluate(compatContext(
                List.of(sw(DB, "java", "11.0.24", "11", AFTER)),
                List.of(),
                PLANNED_DATE))).isEmpty();
    }
}
