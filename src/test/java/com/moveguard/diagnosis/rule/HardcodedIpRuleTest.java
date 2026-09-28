package com.moveguard.diagnosis.rule;

import static com.moveguard.diagnosis.rule.Fixtures.DB;
import static com.moveguard.diagnosis.rule.Fixtures.WEB;
import static com.moveguard.diagnosis.rule.Fixtures.context;
import static com.moveguard.diagnosis.rule.Fixtures.dependency;
import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.diagnosis.Finding;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HardcodedIpRuleTest {

    private final HardcodedIpRule rule = new HardcodedIpRule();

    @Test
    @DisplayName("접속 주소가 IPv4로 적혀 있으면 IP-03 발견")
    void detectsIpv4() {
        List<Finding> findings = rule.evaluate(context(
                List.of(), List.of(dependency(WEB, DB, "203.0.113.21"))));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).params())
                .containsEntry("asset", "web01")
                .containsEntry("config", "application.yml spring.datasource.url");
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.10.1.21", "2001:db8::10"})
    @DisplayName("사설IP와 IPv6도 하드코딩으로 판정")
    void detectsPrivateAndIpv6(String address) {
        assertThat(rule.evaluate(context(
                List.of(), List.of(dependency(WEB, DB, address))))).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"db.internal.example.com", "999.1.1.1", "localhost"})
    @DisplayName("도메인이나 올바르지 않은 주소는 해당 없음")
    void ignoresNonIp(String address) {
        assertThat(rule.evaluate(context(
                List.of(), List.of(dependency(WEB, DB, address))))).isEmpty();
    }
}
