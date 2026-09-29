package com.moveguard.diagnosis.rule;

import com.moveguard.asset.Asset;
import com.moveguard.asset.AssetSoftware;
import com.moveguard.asset.Phase;
import com.moveguard.compat.CompatRelease;
import com.moveguard.diagnosis.DiagnosisContext;
import com.moveguard.diagnosis.Finding;
import com.moveguard.diagnosis.RiskRuleEvaluator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * CMP-02: 이전 후 WAS가 요구하는 최소 Java 버전을, 이전 후 Java 런타임이 충족하지 못함.
 * 최소 Java 미달이면 WAS가 아예 기동되지 않는다.
 */
@Component
public class MinJavaVersionRule implements RiskRuleEvaluator {

    public static final String CODE = "CMP-02";

    @Override
    public String ruleCode() {
        return CODE;
    }

    @Override
    public List<Finding> evaluate(DiagnosisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (AssetSoftware sw : context.software(Phase.AFTER)) {
            CompatRelease release = context.release(sw.getProduct(), sw.getReleaseLine()).orElse(null);
            if (release == null || release.getMinJavaVersion() == null) {
                continue;
            }
            Integer required = Versions.major(release.getMinJavaVersion());
            if (required == null) {
                continue;
            }

            Optional<AssetSoftware> java = context.software(sw.getAssetId(), "java", Phase.AFTER);
            if (java.isEmpty()) {
                continue;
            }
            Integer current = Versions.major(java.get().getReleaseLine());
            if (current == null || current >= required) {
                continue;
            }

            String assetName = context.asset(sw.getAssetId()).map(Asset::getName).orElse("-");
            findings.add(new Finding(CODE, sw.getAssetId(), null, Map.of(
                    "asset", assetName,
                    "product", sw.getProduct(),
                    "version", sw.getVersion(),
                    "required", release.getMinJavaVersion(),
                    "current", java.get().getVersion())));
        }
        return findings;
    }
}
