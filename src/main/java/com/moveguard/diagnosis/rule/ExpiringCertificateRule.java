package com.moveguard.diagnosis.rule;

import com.moveguard.asset.Certificate;
import com.moveguard.asset.Phase;
import com.moveguard.diagnosis.DiagnosisContext;
import com.moveguard.diagnosis.Finding;
import com.moveguard.diagnosis.RiskRuleEvaluator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * CERT-01: 이전 전 인증서 만료일이 전환 예정일과 근접(또는 그 이전).
 * 전환 전후로 인증서가 만료되면 HTTPS 접속이 실패해 서비스가 중단될 수 있다.
 */
@Component
public class ExpiringCertificateRule implements RiskRuleEvaluator {

    public static final String CODE = "CERT-01";

    /** 전환 예정일 기준 이 기간 안에 만료되면 임박으로 본다 */
    static final int IMMINENT_DAYS = 30;

    @Override
    public String ruleCode() {
        return CODE;
    }

    @Override
    public List<Finding> evaluate(DiagnosisContext context) {
        Optional<LocalDate> plannedDate = context.plannedDate();
        if (plannedDate.isEmpty()) {
            return List.of();
        }
        LocalDate threshold = plannedDate.get().plusDays(IMMINENT_DAYS);

        List<Finding> findings = new ArrayList<>();
        for (Certificate cert : context.certificates()) {
            if (cert.getPhase() != Phase.BEFORE) {
                continue;
            }
            if (cert.getNotAfter().isAfter(threshold)) {
                continue;
            }

            findings.add(new Finding(CODE, null, null, Map.of(
                    "domain", cert.getDomain(),
                    "expiry", cert.getNotAfter().toString(),
                    "planned", plannedDate.get().toString())));
        }
        return findings;
    }
}
