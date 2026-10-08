package com.moveguard.diagnosis.rule;

import com.moveguard.asset.Asset;
import com.moveguard.asset.AssetSoftware;
import com.moveguard.asset.Phase;
import com.moveguard.compat.CompatRelease;
import com.moveguard.diagnosis.DiagnosisContext;
import com.moveguard.diagnosis.Finding;
import com.moveguard.diagnosis.RiskRuleEvaluator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * CMP-06: 이전 후 버전이 연장 지원(Extended Support) 구간.
 * 활성(일반) 지원은 끝났지만 전환 예정일 기준으로 연장 지원은 아직 유효한 상태.
 * 기술 지원은 되지만 추가 비용·제약이 따르므로 경고한다. (완전 EOL이면 CMP-01)
 */
@Component
public class ExtendedSupportRule implements RiskRuleEvaluator {

    public static final String CODE = "CMP-06";

    @Override
    public String ruleCode() {
        return CODE;
    }

    @Override
    public List<Finding> evaluate(DiagnosisContext context) {
        Optional<LocalDate> plannedDate = context.plannedDate();
        if (plannedDate.isEmpty()) {
            return List.of();
        }
        LocalDate asOf = plannedDate.get();

        List<Finding> findings = new ArrayList<>();
        for (AssetSoftware sw : context.software(Phase.AFTER)) {
            CompatRelease release = context.release(sw.getProduct(), sw.getReleaseLine()).orElse(null);
            if (release == null || !release.inExtendedSupport(asOf)) {
                continue;
            }

            String assetName = context.asset(sw.getAssetId()).map(Asset::getName).orElse("-");
            findings.add(new Finding(CODE, sw.getAssetId(), null, Map.of(
                    "asset", assetName,
                    "product", sw.getProduct(),
                    "version", sw.getVersion(),
                    "extSupport", release.getExtSupportDate().toString())));
        }
        return findings;
    }
}
