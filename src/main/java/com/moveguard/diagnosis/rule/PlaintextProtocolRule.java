package com.moveguard.diagnosis.rule;

import com.moveguard.asset.Asset;
import com.moveguard.asset.Dependency;
import com.moveguard.diagnosis.DiagnosisContext;
import com.moveguard.diagnosis.Finding;
import com.moveguard.diagnosis.RiskRuleEvaluator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * PORT-02: 자산 간 통신에 평문 프로토콜을 사용.
 * 전송 구간이 암호화되지 않아 자격 증명·데이터가 노출될 수 있다.
 */
@Component
public class PlaintextProtocolRule implements RiskRuleEvaluator {

    public static final String CODE = "PORT-02";

    /** 전송 구간을 암호화하지 않는 대표 프로토콜 */
    static final Set<String> PLAINTEXT_PROTOCOLS = Set.of("HTTP", "FTP", "TELNET", "TCP");

    @Override
    public String ruleCode() {
        return CODE;
    }

    @Override
    public List<Finding> evaluate(DiagnosisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (Dependency dep : context.dependencies()) {
            if (!isPlaintext(dep.getProtocol())) {
                continue;
            }

            String from = context.asset(dep.getFromAssetId()).map(Asset::getName).orElse("-");
            String to = context.asset(dep.getToAssetId()).map(Asset::getName).orElse("-");

            findings.add(new Finding(CODE, dep.getFromAssetId(), dep.getDependencyId(), Map.of(
                    "asset", from,
                    "target", to,
                    "protocol", dep.getProtocol())));
        }
        return findings;
    }

    static boolean isPlaintext(String protocol) {
        return protocol != null
                && PLAINTEXT_PROTOCOLS.contains(protocol.trim().toUpperCase(Locale.ROOT));
    }
}
