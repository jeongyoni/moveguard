package com.moveguard.diagnosis.rule;

import static com.moveguard.asset.AssetIp.IpType.PRIVATE;
import static com.moveguard.asset.AssetIp.IpType.PUBLIC;
import static com.moveguard.asset.Phase.BEFORE;
import static com.moveguard.diagnosis.rule.Fixtures.DB;
import static com.moveguard.diagnosis.rule.Fixtures.WEB;
import static com.moveguard.diagnosis.rule.Fixtures.context;
import static com.moveguard.diagnosis.rule.Fixtures.dependency;
import static com.moveguard.diagnosis.rule.Fixtures.ip;
import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.asset.Dependency;
import com.moveguard.diagnosis.Finding;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SensitivePortExposureRuleTest {

    private final SensitivePortExposureRule rule = new SensitivePortExposureRule();

    @Test
    @DisplayName("민감 포트(3306)를 공인IP로 접속하면 PORT-01 발견")
    void detectsSensitivePortOnPublicIp() {
        List<Finding> findings = rule.evaluate(context(
                List.of(ip(DB, "203.0.113.21", PUBLIC, BEFORE)),
                List.of(dependency(WEB, DB, "203.0.113.21"))));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).params())
                .containsEntry("asset", "web01")
                .containsEntry("target", "db01")
                .containsEntry("port", "3306")
                .containsEntry("address", "203.0.113.21");
    }

    @Test
    @DisplayName("민감 포트라도 사설IP면 해당 없음")
    void ignoresPrivateIp() {
        assertThat(rule.evaluate(context(
                List.of(ip(DB, "10.10.1.21", PRIVATE, BEFORE)),
                List.of(dependency(WEB, DB, "10.10.1.21"))))).isEmpty();
    }

    @Test
    @DisplayName("공인IP라도 민감 포트가 아니면 해당 없음")
    void ignoresNonSensitivePort() {
        Dependency web = new Dependency(11L, 1L, WEB.getAssetId(), DB.getAssetId(),
                "203.0.113.21", 8080, "HTTP", "nginx.conf");
        assertThat(rule.evaluate(context(
                List.of(ip(DB, "203.0.113.21", PUBLIC, BEFORE)),
                List.of(web)))).isEmpty();
    }
}
