package com.moveguard.diagnosis.rule;

import com.moveguard.asset.DnsRecord;
import com.moveguard.asset.Phase;
import com.moveguard.diagnosis.DiagnosisContext;
import com.moveguard.diagnosis.Finding;
import com.moveguard.diagnosis.RiskRuleEvaluator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * DNS-02: 변경되는 공인IP를 가리키는 A 레코드가 있으나 이전 후(AFTER) 레코드가 없음.
 * 변경 후 값·시점이 정해지지 않아 전환 계획 자체에 공백이 생긴다.
 */
@Component
public class MissingDnsChangeRule implements RiskRuleEvaluator {

    public static final String CODE = "DNS-02";

    @Override
    public String ruleCode() {
        return CODE;
    }

    @Override
    public List<Finding> evaluate(DiagnosisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (DnsRecord record : context.dnsRecords()) {
            if (record.getPhase() != Phase.BEFORE || !"A".equalsIgnoreCase(record.getRecordType())) {
                continue;
            }
            if (!context.isChangingPublicIpAddress(record.getValue())) {
                continue;
            }
            if (context.hasDnsRecord(record.getDomain(), Phase.AFTER)) {
                continue;
            }

            findings.add(new Finding(CODE, null, null, Map.of(
                    "domain", record.getDomain(),
                    "address", record.getValue())));
        }
        return findings;
    }
}
