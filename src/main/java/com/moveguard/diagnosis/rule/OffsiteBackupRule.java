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
 * BAK-04: 백업이 원본과 같은 환경에 보관됨(오프사이트 아님).
 * 원본과 백업이 함께 장애를 겪으면 복구가 불가능하다.
 */
@Component
public class OffsiteBackupRule implements RiskRuleEvaluator {

    public static final String CODE = "BAK-04";

    @Override
    public String ruleCode() {
        return CODE;
    }

    @Override
    public List<Finding> evaluate(DiagnosisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (BackupPlan backup : context.backups(Phase.BEFORE)) {
            if (backup.isOffsite()) {
                continue;
            }
            String assetName = context.asset(backup.getAssetId()).map(Asset::getName).orElse("-");
            findings.add(new Finding(CODE, backup.getAssetId(), null, Map.of("asset", assetName)));
        }
        return findings;
    }
}
