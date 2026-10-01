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

class MissingRecentBackupRuleTest {

    private final MissingRecentBackupRule rule = new MissingRecentBackupRule();

    @Test
    @DisplayName("백업이 아예 없으면(마지막 백업일 null) BAK-01 발견")
    void detectsNoBackup() {
        List<Finding> findings = rule.evaluate(backupContext(
                List.of(backup(DB, null, false, true, BEFORE)), PLANNED_DATE));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).params())
                .containsEntry("asset", "db01")
                .containsEntry("lastBackup", "없음");
    }

    @Test
    @DisplayName("백업이 오래되면(임계 초과) BAK-01 발견")
    void detectsStaleBackup() {
        assertThat(rule.evaluate(backupContext(
                List.of(backup(DB, PLANNED_DATE.minusDays(30), true, true, BEFORE)),
                PLANNED_DATE))).hasSize(1);
    }

    @Test
    @DisplayName("최근 백업이면 해당 없음")
    void ignoresRecentBackup() {
        assertThat(rule.evaluate(backupContext(
                List.of(backup(DB, PLANNED_DATE.minusDays(2), false, true, BEFORE)),
                PLANNED_DATE))).isEmpty();
    }
}
