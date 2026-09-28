package com.moveguard.diagnosis.rule;

import com.moveguard.asset.Certificate;
import com.moveguard.asset.Phase;
import com.moveguard.diagnosis.DiagnosisContext;
import com.moveguard.diagnosis.Finding;
import com.moveguard.diagnosis.RiskRuleEvaluator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * CERT-02: 이전 전 인증서가 있으나 이전 후(AFTER) 갱신·이관 계획이 없음.
 * 신규 환경에 배포할 인증서가 정해지지 않아 전환 후 HTTPS 접속이 실패할 수 있다.
 */
@Component
public class MissingCertificateRenewalRule implements RiskRuleEvaluator {

    public static final String CODE = "CERT-02";

    @Override
    public String ruleCode() {
        return CODE;
    }

    @Override
    public List<Finding> evaluate(DiagnosisContext context) {
        List<Finding> findings = new ArrayList<>();

        for (Certificate cert : context.certificates()) {
            if (cert.getPhase() != Phase.BEFORE) {
                continue;
            }
            if (context.hasCertificate(cert.getDomain(), Phase.AFTER)) {
                continue;
            }

            findings.add(new Finding(CODE, null, null, Map.of("domain", cert.getDomain())));
        }
        return findings;
    }
}
