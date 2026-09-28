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
 * DNS-01: 변경되는 공인IP를 가리키는 A 레코드의 TTL이 김.
 * TTL이 길면 IP 변경 후에도 캐시된 옛 주소로 접속이 몰려 전파 지연이 커진다.
 */
@Component
public class LongTtlDnsRule implements RiskRuleEvaluator {

    public static final String CODE = "DNS-01";

    /** 전환 시 권장 TTL(초). 이 값을 넘으면 캐시 전파 지연 위험으로 본다 */
    static final int TTL_THRESHOLD_SECONDS = 300;

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
            if (record.getTtl() <= TTL_THRESHOLD_SECONDS) {
                continue;
            }
            if (!context.isChangingPublicIpAddress(record.getValue())) {
                continue;
            }

            findings.add(new Finding(CODE, null, null, Map.of(
                    "domain", record.getDomain(),
                    "address", record.getValue(),
                    "ttl", String.valueOf(record.getTtl()))));
        }
        return findings;
    }
}
