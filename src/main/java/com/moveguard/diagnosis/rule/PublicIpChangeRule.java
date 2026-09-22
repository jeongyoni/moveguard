package com.moveguard.diagnosis.rule;

import com.moveguard.asset.Asset;
import com.moveguard.asset.Dependency;
import com.moveguard.diagnosis.DiagnosisContext;
import com.moveguard.diagnosis.Finding;
import com.moveguard.diagnosis.RiskRuleEvaluator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * IP-01: 공인IP로 접속 중인 대상의 공인IP가 이전 후 변경됨.
 * 설정 파일의 접속 주소를 수정하지 않으면 전환 직후 연결이 끊긴다.
 */
@Component
public class PublicIpChangeRule implements RiskRuleEvaluator {

    public static final String CODE = "IP-01";

    @Override
    public String ruleCode() {
        return CODE;
    }

    @Override
    public List<Finding> evaluate(DiagnosisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (Dependency dep : context.dependencies()) {
            if (dep.getToAssetId() == null) {
                continue;
            }
            if (!context.isChangingPublicIp(dep.getToAssetId(), dep.getTargetAddress())) {
                continue;
            }

            String from = context.asset(dep.getFromAssetId()).map(Asset::getName).orElse("-");
            String to = context.asset(dep.getToAssetId()).map(Asset::getName).orElse("-");

            findings.add(new Finding(CODE, dep.getFromAssetId(), dep.getDependencyId(), Map.of(
                    "asset", from,
                    "target", to,
                    "address", dep.getTargetAddress(),
                    "port", String.valueOf(dep.getTargetPort()),
                    "config", Objects.toString(dep.getConfigLocation(), "-"))));
        }
        return findings;
    }
}
