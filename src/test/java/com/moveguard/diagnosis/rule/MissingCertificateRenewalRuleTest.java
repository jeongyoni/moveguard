package com.moveguard.diagnosis.rule;

import static com.moveguard.asset.Phase.AFTER;
import static com.moveguard.asset.Phase.BEFORE;
import static com.moveguard.diagnosis.rule.Fixtures.PLANNED_DATE;
import static com.moveguard.diagnosis.rule.Fixtures.cert;
import static com.moveguard.diagnosis.rule.Fixtures.certContext;
import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.diagnosis.Finding;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MissingCertificateRenewalRuleTest {

    private final MissingCertificateRenewalRule rule = new MissingCertificateRenewalRule();

    @Test
    @DisplayName("이전 전 인증서만 있고 이전 후 인증서가 없으면 CERT-02 발견")
    void detectsMissingRenewal() {
        List<Finding> findings = rule.evaluate(certContext(
                List.of(cert("shop.example.com", PLANNED_DATE.plusDays(200), BEFORE)), PLANNED_DATE));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).params()).containsEntry("domain", "shop.example.com");
    }

    @Test
    @DisplayName("이전 후 인증서가 등록되어 있으면 해당 없음")
    void ignoresWhenAfterCertificateExists() {
        assertThat(rule.evaluate(certContext(List.of(
                cert("shop.example.com", PLANNED_DATE.plusDays(10), BEFORE),
                cert("shop.example.com", PLANNED_DATE.plusDays(400), AFTER)),
                PLANNED_DATE))).isEmpty();
    }

    @Test
    @DisplayName("이전 후(AFTER) 인증서만 있는 도메인은 검사 대상이 아니다")
    void ignoresAfterOnly() {
        assertThat(rule.evaluate(certContext(
                List.of(cert("shop.example.com", PLANNED_DATE.plusDays(400), AFTER)),
                PLANNED_DATE))).isEmpty();
    }
}
