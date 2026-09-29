package com.moveguard.diagnosis.rule;

import static com.moveguard.asset.Phase.AFTER;
import static com.moveguard.diagnosis.rule.Fixtures.DB;
import static com.moveguard.diagnosis.rule.Fixtures.PLANNED_DATE;
import static com.moveguard.diagnosis.rule.Fixtures.compatContext;
import static com.moveguard.diagnosis.rule.Fixtures.release;
import static com.moveguard.diagnosis.rule.Fixtures.sw;
import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.diagnosis.Finding;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImminentEolRuleTest {

    private final ImminentEolRule rule = new ImminentEolRule();

    @Test
    @DisplayName("이전 후 버전의 EOL이 전환 예정일 1년 이내면 CMP-04 발견")
    void detectsImminentEol() {
        List<Finding> findings = rule.evaluate(compatContext(
                List.of(sw(DB, "mysql", "8.0.40", "8.0", AFTER)),
                List.of(release("mysql", "8.0", false, PLANNED_DATE.plusMonths(6), null)),
                PLANNED_DATE));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).params())
                .containsEntry("product", "mysql")
                .containsEntry("eol", PLANNED_DATE.plusMonths(6).toString());
    }

    @Test
    @DisplayName("EOL이 1년보다 멀면 해당 없음")
    void ignoresDistantEol() {
        assertThat(rule.evaluate(compatContext(
                List.of(sw(DB, "mysql", "8.4.0", "8.4", AFTER)),
                List.of(release("mysql", "8.4", false, PLANNED_DATE.plusYears(5), null)),
                PLANNED_DATE))).isEmpty();
    }

    @Test
    @DisplayName("이미 EOL이면 CMP-04가 아니라 CMP-01 소관 → 해당 없음")
    void ignoresAlreadyEol() {
        assertThat(rule.evaluate(compatContext(
                List.of(sw(DB, "mysql", "9.6.1", "9.6", AFTER)),
                List.of(release("mysql", "9.6", true, PLANNED_DATE.minusMonths(6), null)),
                PLANNED_DATE))).isEmpty();
    }
}
