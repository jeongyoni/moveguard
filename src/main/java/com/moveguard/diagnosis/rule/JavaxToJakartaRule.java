package com.moveguard.diagnosis.rule;

import com.moveguard.asset.Asset;
import com.moveguard.asset.AssetSoftware;
import com.moveguard.asset.Phase;
import com.moveguard.diagnosis.DiagnosisContext;
import com.moveguard.diagnosis.Finding;
import com.moveguard.diagnosis.RiskRuleEvaluator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * CMP-03: Tomcat 9 이하 → 10 이상 이전. Tomcat 10부터 서블릿 패키지가 javax → jakarta로 바뀌어,
 * 애플리케이션 수정 없이는 기동되지 않는다.
 */
@Component
public class JavaxToJakartaRule implements RiskRuleEvaluator {

    public static final String CODE = "CMP-03";
    private static final String TOMCAT = "tomcat";

    @Override
    public String ruleCode() {
        return CODE;
    }

    @Override
    public List<Finding> evaluate(DiagnosisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (AssetSoftware after : context.software(Phase.AFTER)) {
            if (!TOMCAT.equals(after.getProduct())) {
                continue;
            }
            Optional<AssetSoftware> before = context.software(after.getAssetId(), TOMCAT, Phase.BEFORE);
            if (before.isEmpty()) {
                continue;
            }
            Integer fromMajor = Versions.major(before.get().getReleaseLine());
            Integer toMajor = Versions.major(after.getReleaseLine());
            if (fromMajor == null || toMajor == null || fromMajor > 9 || toMajor < 10) {
                continue;
            }

            String assetName = context.asset(after.getAssetId()).map(Asset::getName).orElse("-");
            findings.add(new Finding(CODE, after.getAssetId(), null, Map.of(
                    "asset", assetName,
                    "product", after.getProduct(),
                    "from", before.get().getVersion(),
                    "to", after.getVersion())));
        }
        return findings;
    }
}
