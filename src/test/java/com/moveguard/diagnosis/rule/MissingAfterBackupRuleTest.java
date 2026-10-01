package com.moveguard.diagnosis.rule;

import static com.moveguard.asset.Phase.AFTER;
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

class MissingAfterBackupRuleTest {

    private final MissingAfterBackupRule rule = new MissingAfterBackupRule();

    @Test
    @DisplayName("이전 전 백업만 있고 이전 후 백업이 없으면 BAK-03 발견")
    void detectsMissingAfterBackup() {
        List<Finding> findings = rule.evaluate(backupContext(
                List.of(backup(DB, PLANNED_DATE.minusDays(2), true, true, BEFORE)), PLANNED_DATE));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).params()).containsEntry("asset", "db01");
    }

    @Test
    @DisplayName("이전 후 백업이 구성되어 있으면 해당 없음")
    void ignoresWhenAfterBackupExists() {
        assertThat(rule.evaluate(backupContext(List.of(
                backup(DB, PLANNED_DATE.minusDays(2), true, true, BEFORE),
                backup(DB, PLANNED_DATE.plusDays(2), true, true, AFTER)),
                PLANNED_DATE))).isEmpty();
    }
}
