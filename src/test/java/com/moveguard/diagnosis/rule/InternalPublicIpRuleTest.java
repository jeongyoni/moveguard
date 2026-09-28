package com.moveguard.diagnosis.rule;

import static com.moveguard.asset.AssetIp.IpType.PRIVATE;
import static com.moveguard.asset.AssetIp.IpType.PUBLIC;
import static com.moveguard.asset.Phase.BEFORE;
import static com.moveguard.diagnosis.rule.Fixtures.DB;
import static com.moveguard.diagnosis.rule.Fixtures.PG;
import static com.moveguard.diagnosis.rule.Fixtures.WEB;
import static com.moveguard.diagnosis.rule.Fixtures.context;
import static com.moveguard.diagnosis.rule.Fixtures.dependency;
import static com.moveguard.diagnosis.rule.Fixtures.ip;
import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.diagnosis.Finding;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InternalPublicIpRuleTest {

    private final InternalPublicIpRule rule = new InternalPublicIpRule();

    @Test
    @DisplayName("내부 서버 간 통신이 대상의 공인IP를 경유하면 IP-02 발견")
    void detectsInternalTrafficOverPublicIp() {
        List<Finding> findings = rule.evaluate(context(
                List.of(ip(DB, "203.0.113.21", PUBLIC, BEFORE)),
                List.of(dependency(WEB, DB, "203.0.113.21"))));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).params())
                .containsEntry("asset", "web01")
                .containsEntry("target", "db01")
                .containsEntry("address", "203.0.113.21");
    }

    @Test
    @DisplayName("사설IP로 통신하면 해당 없음")
    void ignoresPrivateIp() {
        assertThat(rule.evaluate(context(
                List.of(ip(DB, "10.10.1.21", PRIVATE, BEFORE)),
                List.of(dependency(WEB, DB, "10.10.1.21"))))).isEmpty();
    }

    @Test
    @DisplayName("외부 기관을 공인IP로 호출하는 것은 정상이므로 해당 없음")
    void ignoresExternalTarget() {
        assertThat(rule.evaluate(context(
                List.of(ip(PG, "198.51.100.50", PUBLIC, BEFORE)),
                List.of(dependency(WEB, PG, "198.51.100.50"))))).isEmpty();
    }
}
