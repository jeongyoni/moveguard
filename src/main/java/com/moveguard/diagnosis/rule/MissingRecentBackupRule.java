package com.moveguard.diagnosis.rule;

import com.moveguard.asset.Asset;
import com.moveguard.asset.BackupPlan;
import com.moveguard.asset.Phase;
import com.moveguard.diagnosis.DiagnosisContext;
import com.moveguard.diagnosis.Finding;
import com.moveguard.diagnosis.RiskRuleEvaluator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * BAK-01: 이전 전 최근 백업이 없음(백업 미수행 또는 오래됨).
 * 전환 실패 시 롤백할 백업이 없으면 복구가 불가능하다.
 */
@Component
public class MissingRecentBackupRule implements RiskRuleEvaluator {

    public static final String CODE = "BAK-01";
    static final int STALE_DAYS = 7;

    @Override
    public String ruleCode() {
        return CODE;
    }

    @Override
    public List<Finding> evaluate(DiagnosisContext context) {
        LocalDate planned = context.plannedDate().orElse(null);
        List<Finding> findings = new ArrayList<>();

        for (BackupPlan backup : context.backups(Phase.BEFORE)) {
            LocalDate last = backup.getLastBackupAt();
            boolean stale = last == null
                    || (planned != null && last.isBefore(planned.minusDays(STALE_DAYS)));
            if (!stale) {
                continue;
            }

            String assetName = context.asset(backup.getAssetId()).map(Asset::getName).orElse("-");
            findings.add(new Finding(CODE, backup.getAssetId(), null, Map.of(
                    "asset", assetName,
                    "lastBackup", Objects.toString(last, "없음"))));
        }
        return findings;
    }
}
