package com.moveguard.diagnosis.rule;

import com.moveguard.asset.Asset;
import com.moveguard.asset.AssetIp;
import com.moveguard.asset.AssetIp.IpType;
import com.moveguard.asset.Phase;
import com.moveguard.diagnosis.DiagnosisContext;
import com.moveguard.diagnosis.Finding;
import com.moveguard.diagnosis.RiskRuleEvaluator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * IP-04: 외부 기관 방화벽 허용목록에 등록된 공인IP가 이전 후 변경됨.
 * 외부 기관의 등록 처리 기간이 필요해 전환 일정 자체에 영향을 준다.
 */
@Component
public class WhitelistedIpChangeRule implements RiskRuleEvaluator {

    public static final String CODE = "IP-04";

    @Override
    public String ruleCode() {
        return CODE;
    }

    @Override
    public List<Finding> evaluate(DiagnosisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (AssetIp ip : context.ips()) {
            if (ip.getPhase() != Phase.BEFORE || ip.getIpType() != IpType.PUBLIC || !ip.isExtWhitelisted()) {
                continue;
            }
            if (!context.isChangingPublicIp(ip.getAssetId(), ip.getAddress())) {
                continue;
            }
            String name = context.asset(ip.getAssetId()).map(Asset::getName).orElse("-");

            findings.add(new Finding(CODE, ip.getAssetId(), null, Map.of(
                    "asset", name,
                    "address", ip.getAddress())));
        }
        return findings;
    }
}
