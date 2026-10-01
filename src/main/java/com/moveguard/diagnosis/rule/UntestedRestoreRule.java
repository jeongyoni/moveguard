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
 * BAK-02: 백업의 복구 테스트가 수행되지 않음.
 * 백업이 있어도 실제로 복구되는지 검증하지 않으면 롤백을 신뢰할 수 없다.
 */
@Component
public class UntestedRestoreRule implements RiskRuleEvaluator {

    public static final String CODE = "BAK-02";

    @Override
    public String ruleCode() {
        return CODE;
    }

    @Override
    public List<Finding> evaluate(DiagnosisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (BackupPlan backup : context.backups(Phase.BEFORE)) {
            if (backup.isRestoreTested()) {
                continue;
            }
            String assetName = context.asset(backup.getAssetId()).map(Asset::getName).orElse("-");
            findings.add(new Finding(CODE, backup.getAssetId(), null, Map.of("asset", assetName)));
        }
        return findings;
    }
}
