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
 * CMP-04: 이전 후 버전의 지원 종료가 전환 예정일 기준 임박(1년 이내).
 * 아직 EOL은 아니지만 전환 직후 재업그레이드가 필요한 상태를 경고한다. (이미 EOL이면 CMP-01)
 */
@Component
public class ImminentEolRule implements RiskRuleEvaluator {

    public static final String CODE = "CMP-04";
    private static final int IMMINENT_DAYS = 365;

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
        LocalDate threshold = plannedDate.get().plusDays(IMMINENT_DAYS);

        List<Finding> findings = new ArrayList<>();
        for (AssetSoftware sw : context.software(Phase.AFTER)) {
            CompatRelease release = context.release(sw.getProduct(), sw.getReleaseLine()).orElse(null);
            if (release == null || release.isEol() || release.getEolDate() == null) {
                continue;
            }
            if (release.getEolDate().isAfter(threshold)) {
                continue;
            }

            String assetName = context.asset(sw.getAssetId()).map(Asset::getName).orElse("-");
            findings.add(new Finding(CODE, sw.getAssetId(), null, Map.of(
                    "asset", assetName,
                    "product", sw.getProduct(),
                    "version", sw.getVersion(),
                    "eol", release.getEolDate().toString())));
        }
        return findings;
    }
}
