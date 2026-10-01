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

class UntestedRestoreRuleTest {

    private final UntestedRestoreRule rule = new UntestedRestoreRule();

    @Test
    @DisplayName("복구 테스트를 안 했으면 BAK-02 발견")
    void detectsUntestedRestore() {
        List<Finding> findings = rule.evaluate(backupContext(
                List.of(backup(DB, PLANNED_DATE.minusDays(2), false, true, BEFORE)), PLANNED_DATE));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).params()).containsEntry("asset", "db01");
    }

    @Test
    @DisplayName("복구 테스트를 했으면 해당 없음")
    void ignoresTestedRestore() {
        assertThat(rule.evaluate(backupContext(
                List.of(backup(DB, PLANNED_DATE.minusDays(2), true, true, BEFORE)),
                PLANNED_DATE))).isEmpty();
    }
}
