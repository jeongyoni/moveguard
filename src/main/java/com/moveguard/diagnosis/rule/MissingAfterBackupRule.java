package com.moveguard.diagnosis.rule;

import com.moveguard.asset.Asset;
import com.moveguard.asset.BackupPlan;
import com.moveguard.asset.Phase;
import com.moveguard.diagnosis.DiagnosisContext;
import com.moveguard.diagnosis.Finding;
import com.moveguard.diagnosis.RiskRuleEvaluator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * BAK-03: 이전 전 백업은 있으나 이전 후(AFTER) 백업 체계가 구성되지 않음.
 * 전환 후 신규 환경에 백업 규칙이 없으면 운영 중 장애 시 복구 수단이 없다.
 */
@Component
public class MissingAfterBackupRule implements RiskRuleEvaluator {

    public static final String CODE = "BAK-03";

    @Override
    public String ruleCode() {
        return CODE;
    }

    @Override
    public List<Finding> evaluate(DiagnosisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (BackupPlan backup : context.backups(Phase.BEFORE)) {
            if (context.hasBackup(backup.getAssetId(), Phase.AFTER)) {
                continue;
            }
            String assetName = context.asset(backup.getAssetId()).map(Asset::getName).orElse("-");
            findings.add(new Finding(CODE, backup.getAssetId(), null, Map.of("asset", assetName)));
        }
        return findings;
    }
}
