package com.moveguard.diagnosis.rule;

import com.moveguard.asset.Asset;
import com.moveguard.asset.Dependency;
import com.moveguard.diagnosis.DiagnosisContext;
import com.moveguard.diagnosis.Finding;
import com.moveguard.diagnosis.RiskRuleEvaluator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * PORT-01: 민감 포트(DB·관리 등)가 공인IP로 노출됨.
 * 공인IP로 이런 포트에 접근 가능하면 외부 공격 표면이 되고, 전환 시 보안그룹 재설계가 필요하다.
 */
@Component
public class SensitivePortExposureRule implements RiskRuleEvaluator {

    public static final String CODE = "PORT-01";

    /** DB·캐시·관리 접속 등 외부에 열려선 안 되는 포트 */
    static final Set<Integer> SENSITIVE_PORTS = Set.of(
            22,    // SSH
            23,    // Telnet
            3389,  // RDP
            3306,  // MySQL
            5432,  // PostgreSQL
            1433,  // SQL Server
            1521,  // Oracle
            6379,  // Redis
            27017, // MongoDB
            9200); // Elasticsearch

    @Override
    public String ruleCode() {
        return CODE;
    }

    @Override
    public List<Finding> evaluate(DiagnosisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (Dependency dep : context.dependencies()) {
            if (dep.getTargetPort() == null || !SENSITIVE_PORTS.contains(dep.getTargetPort())) {
                continue;
            }
            if (!context.isPublicIp(dep.getTargetAddress())) {
                continue;
            }

            String from = context.asset(dep.getFromAssetId()).map(Asset::getName).orElse("-");
            String to = context.asset(dep.getToAssetId()).map(Asset::getName).orElse("-");

            findings.add(new Finding(CODE, dep.getFromAssetId(), dep.getDependencyId(), Map.of(
                    "asset", from,
                    "target", to,
                    "port", String.valueOf(dep.getTargetPort()),
                    "address", dep.getTargetAddress())));
        }
        return findings;
    }
}
