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
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * IP-03: 접속 주소가 IP로 하드코딩됨.
 * 도메인으로 접속하면 IP가 바뀌어도 DNS 변경만으로 대응할 수 있다.
 */
@Component
public class HardcodedIpRule implements RiskRuleEvaluator {

    public static final String CODE = "IP-03";

    private static final String OCTET = "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)";
    private static final Pattern IPV4 = Pattern.compile("^" + OCTET + "(\\." + OCTET + "){3}$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F]*:[0-9a-fA-F:.]*$");

    @Override
    public String ruleCode() {
        return CODE;
    }

    @Override
    public List<Finding> evaluate(DiagnosisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (Dependency dep : context.dependencies()) {
            if (!isIpLiteral(dep.getTargetAddress())) {
                continue;
            }
            String from = context.asset(dep.getFromAssetId()).map(Asset::getName).orElse("-");

            findings.add(new Finding(CODE, dep.getFromAssetId(), dep.getDependencyId(), Map.of(
                    "asset", from,
                    "address", dep.getTargetAddress(),
                    "config", Objects.toString(dep.getConfigLocation(), "설정 파일"))));
        }
        return findings;
    }

    static boolean isIpLiteral(String address) {
        if (address == null) {
            return false;
        }
        return IPV4.matcher(address).matches() || IPV6.matcher(address).matches();
    }
}
