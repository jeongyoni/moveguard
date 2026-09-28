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

class ExpiringCertificateRuleTest {

    private final ExpiringCertificateRule rule = new ExpiringCertificateRule();

    @Test
    @DisplayName("이전 전 인증서가 전환 예정일 30일 이내에 만료되면 CERT-01 발견")
    void detectsImminentExpiry() {
        List<Finding> findings = rule.evaluate(certContext(
                List.of(cert("shop.example.com", PLANNED_DATE.plusDays(10), BEFORE)), PLANNED_DATE));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).params())
                .containsEntry("domain", "shop.example.com")
                .containsEntry("expiry", PLANNED_DATE.plusDays(10).toString())
                .containsEntry("planned", PLANNED_DATE.toString());
    }

    @Test
    @DisplayName("전환 예정일보다 이전에 만료돼도 발견")
    void detectsExpiryBeforeMigration() {
        assertThat(rule.evaluate(certContext(
                List.of(cert("shop.example.com", PLANNED_DATE.minusDays(5), BEFORE)),
                PLANNED_DATE))).hasSize(1);
    }

    @Test
    @DisplayName("만료일이 충분히 남은 인증서는 해당 없음")
    void ignoresDistantExpiry() {
        assertThat(rule.evaluate(certContext(
                List.of(cert("shop.example.com", PLANNED_DATE.plusDays(120), BEFORE)),
                PLANNED_DATE))).isEmpty();
    }

    @Test
    @DisplayName("이전 후(AFTER) 인증서는 검사 대상이 아니다")
    void ignoresAfterCertificate() {
        assertThat(rule.evaluate(certContext(
                List.of(cert("shop.example.com", PLANNED_DATE.plusDays(10), AFTER)),
                PLANNED_DATE))).isEmpty();
    }

    @Test
    @DisplayName("전환 예정일이 없으면 해당 없음")
    void ignoresWhenNoPlannedDate() {
        assertThat(rule.evaluate(certContext(
                List.of(cert("shop.example.com", PLANNED_DATE.plusDays(10), BEFORE)),
                null))).isEmpty();
    }
}
