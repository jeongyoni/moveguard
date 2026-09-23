package com.moveguard.diagnosis.rule;

import com.moveguard.asset.Asset;
import com.moveguard.asset.AssetIp.IpType;
import com.moveguard.asset.Dependency;
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
 * IP-02: 내부 서버 간 통신에 공인IP 사용.
 * 외부 노출 경로가 생기고, IP 변경 시 영향 범위가 커진다.
 * 외부 기관(EXTERNAL) 연동은 공인IP 사용이 정상이므로 제외한다.
 */
@Component
public class InternalPublicIpRule implements RiskRuleEvaluator {

    public static final String CODE = "IP-02";

    @Override
    public String ruleCode() {
        return CODE;
    }

    @Override
    public List<Finding> evaluate(DiagnosisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (Dependency dep : context.dependencies()) {
            Optional<Asset> from = context.asset(dep.getFromAssetId());
            Optional<Asset> to = context.asset(dep.getToAssetId());
            if (from.isEmpty() || to.isEmpty()) {
                continue;
            }
            if (!from.get().isServer() || !to.get().isServer()) {
                continue;
            }
            if (!context.hasIp(to.get().getAssetId(), dep.getTargetAddress(), Phase.BEFORE, IpType.PUBLIC)) {
                continue;
            }

            findings.add(new Finding(CODE, from.get().getAssetId(), dep.getDependencyId(), Map.of(
                    "asset", from.get().getName(),
                    "target", to.get().getName(),
                    "address", dep.getTargetAddress())));
        }
        return findings;
    }
}
