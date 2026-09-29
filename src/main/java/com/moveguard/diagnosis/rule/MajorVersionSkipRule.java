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
 * CMP-05: 같은 제품의 메이저 버전을 2단계 이상 건너뛰어 이전.
 * 중간 버전을 건너뛰는 업그레이드는 데이터 변환·문법 변경 위험이 있어 권장 경로 확인이 필요하다.
 */
@Component
public class MajorVersionSkipRule implements RiskRuleEvaluator {

    public static final String CODE = "CMP-05";
    private static final int SKIP_THRESHOLD = 2;

    @Override
    public String ruleCode() {
        return CODE;
    }

    @Override
    public List<Finding> evaluate(DiagnosisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (AssetSoftware after : context.software(Phase.AFTER)) {
            Optional<AssetSoftware> before =
                    context.software(after.getAssetId(), after.getProduct(), Phase.BEFORE);
            if (before.isEmpty()) {
                continue;
            }
            Integer fromMajor = Versions.major(before.get().getReleaseLine());
            Integer toMajor = Versions.major(after.getReleaseLine());
            if (fromMajor == null || toMajor == null || toMajor - fromMajor < SKIP_THRESHOLD) {
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
