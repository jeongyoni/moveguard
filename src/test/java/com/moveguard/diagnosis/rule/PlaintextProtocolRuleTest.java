package com.moveguard.diagnosis.rule;

import static com.moveguard.diagnosis.rule.Fixtures.DB;
import static com.moveguard.diagnosis.rule.Fixtures.WEB;
import static com.moveguard.diagnosis.rule.Fixtures.context;
import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.asset.Dependency;
import com.moveguard.diagnosis.Finding;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlaintextProtocolRuleTest {

    private final PlaintextProtocolRule rule = new PlaintextProtocolRule();

    private static Dependency dep(String protocol, int port) {
        return new Dependency(10L, 1L, WEB.getAssetId(), DB.getAssetId(),
                "10.10.1.21", port, protocol, "application.yml");
    }

    @Test
    @DisplayName("평문 프로토콜(HTTP)로 통신하면 PORT-02 발견")
    void detectsPlaintextProtocol() {
        List<Finding> findings = rule.evaluate(context(List.of(), List.of(dep("HTTP", 80))));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).params())
                .containsEntry("asset", "web01")
                .containsEntry("target", "db01")
                .containsEntry("protocol", "HTTP");
    }

    @Test
    @DisplayName("프로토콜 판정은 대소문자를 무시한다")
    void isCaseInsensitive() {
        assertThat(rule.evaluate(context(List.of(), List.of(dep("tcp", 9000))))).hasSize(1);
    }

    @Test
    @DisplayName("암호화 프로토콜(HTTPS)은 해당 없음")
    void ignoresEncryptedProtocol() {
        assertThat(rule.evaluate(context(List.of(), List.of(dep("HTTPS", 443))))).isEmpty();
    }

    @Test
    @DisplayName("JDBC는 평문 목록에 없어 해당 없음")
    void ignoresJdbc() {
        assertThat(rule.evaluate(context(List.of(), List.of(dep("JDBC", 3306))))).isEmpty();
    }
}
