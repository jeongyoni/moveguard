package com.moveguard.diagnosis.rule;

import com.moveguard.asset.Asset;
import com.moveguard.asset.AssetIp;
import com.moveguard.asset.Phase;
import com.moveguard.diagnosis.DiagnosisContext;
import com.moveguard.diagnosis.Finding;
import com.moveguard.diagnosis.RiskRuleEvaluator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * IP-05: 이전 전 IP는 있으나 이전 후(AFTER) IP가 등록되지 않은 자산.
 * 신규 환경의 주소가 미확정이면 접속 설정·방화벽·DNS 등 후속 진단을 진행할 수 없다.
 */
@Component
public class MissingAfterIpRule implements RiskRuleEvaluator {

    public static final String CODE = "IP-05";

    @Override
    public String ruleCode() {
        return CODE;
    }

    @Override
    public List<Finding> evaluate(DiagnosisContext context) {
        Set<Long> beforeAssets = new LinkedHashSet<>();
        for (AssetIp ip : context.ips()) {
            if (ip.getPhase() == Phase.BEFORE) {
                beforeAssets.add(ip.getAssetId());
            }
        }

        List<Finding> findings = new ArrayList<>();
        for (Long assetId : beforeAssets) {
            boolean hasAfter = context.ips().stream().anyMatch(ip ->
                    Objects.equals(ip.getAssetId(), assetId) && ip.getPhase() == Phase.AFTER);
            if (hasAfter) {
                continue;
            }
            String name = context.asset(assetId).map(Asset::getName).orElse("-");
            findings.add(new Finding(CODE, assetId, null, Map.of("asset", name)));
        }
        return findings;
    }
}
