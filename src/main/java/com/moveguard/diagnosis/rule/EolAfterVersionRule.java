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
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * CMP-01: 이전 후 소프트웨어 버전이 이미 지원 종료(EOL)됨.
 * 보안 패치를 받을 수 없는 버전으로 이전하는 것을 막는다.
 */
@Component
public class EolAfterVersionRule implements RiskRuleEvaluator {

    public static final String CODE = "CMP-01";

    @Override
    public String ruleCode() {
        return CODE;
    }

    @Override
    public List<Finding> evaluate(DiagnosisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (AssetSoftware sw : context.software(Phase.AFTER)) {
            CompatRelease release = context.release(sw.getProduct(), sw.getReleaseLine()).orElse(null);
            if (release == null || !release.isEol()) {
                continue;
            }

            String assetName = context.asset(sw.getAssetId()).map(Asset::getName).orElse("-");
            findings.add(new Finding(CODE, sw.getAssetId(), null, Map.of(
                    "asset", assetName,
                    "product", sw.getProduct(),
                    "version", sw.getVersion(),
                    "eol", Objects.toString(release.getEolDate(), "지원 종료"))));
        }
        return findings;
    }
}
