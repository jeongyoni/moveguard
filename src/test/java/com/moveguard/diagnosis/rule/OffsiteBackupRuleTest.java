package com.moveguard.diagnosis.rule;

import static com.moveguard.asset.Phase.BEFORE;
import static com.moveguard.diagnosis.rule.Fixtures.DB;
import static com.moveguard.diagnosis.rule.Fixtures.PLANNED_DATE;
import static com.moveguard.diagnosis.rule.Fixtures.backup;
import static com.moveguard.diagnosis.rule.Fixtures.backupContext;
import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.diagnosis.Finding;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OffsiteBackupRuleTest {

    private final OffsiteBackupRule rule = new OffsiteBackupRule();

    @Test
    @DisplayName("백업이 오프사이트가 아니면 BAK-04 발견")
    void detectsNonOffsite() {
        List<Finding> findings = rule.evaluate(backupContext(
                List.of(backup(DB, PLANNED_DATE.minusDays(2), true, false, BEFORE)), PLANNED_DATE));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).params()).containsEntry("asset", "db01");
    }

    @Test
    @DisplayName("오프사이트 보관이면 해당 없음")
    void ignoresOffsite() {
        assertThat(rule.evaluate(backupContext(
                List.of(backup(DB, PLANNED_DATE.minusDays(2), true, true, BEFORE)),
                PLANNED_DATE))).isEmpty();
    }
}
