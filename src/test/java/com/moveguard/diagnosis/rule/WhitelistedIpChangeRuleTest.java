package com.moveguard.diagnosis.rule;

import static com.moveguard.asset.AssetIp.IpType.PUBLIC;
import static com.moveguard.asset.Phase.AFTER;
import static com.moveguard.asset.Phase.BEFORE;
import static com.moveguard.diagnosis.rule.Fixtures.WEB;
import static com.moveguard.diagnosis.rule.Fixtures.context;
import static com.moveguard.diagnosis.rule.Fixtures.ip;
import static com.moveguard.diagnosis.rule.Fixtures.whitelistedPublicIp;
import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.diagnosis.Finding;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WhitelistedIpChangeRuleTest {

    private final WhitelistedIpChangeRule rule = new WhitelistedIpChangeRule();

    @Test
    @DisplayName("허용목록에 등록된 공인IP가 이전 후 바뀌면 IP-04 발견")
    void detectsWhitelistedIpChange() {
        List<Finding> findings = rule.evaluate(context(List.of(
                whitelistedPublicIp(WEB, "203.0.113.11", BEFORE),
                ip(WEB, "198.51.100.21", PUBLIC, AFTER)), List.of()));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).assetId()).isEqualTo(WEB.getAssetId());
        assertThat(findings.get(0).params())
                .containsEntry("asset", "web01")
                .containsEntry("address", "203.0.113.11");
    }

    @Test
    @DisplayName("허용목록 IP를 이전 후에도 유지하면 해당 없음")
    void ignoresWhenKept() {
        assertThat(rule.evaluate(context(List.of(
                whitelistedPublicIp(WEB, "203.0.113.11", BEFORE),
                ip(WEB, "203.0.113.11", PUBLIC, AFTER)), List.of()))).isEmpty();
    }

    @Test
    @DisplayName("허용목록에 없는 공인IP 변경은 해당 없음")
    void ignoresNotWhitelisted() {
        assertThat(rule.evaluate(context(List.of(
                ip(WEB, "203.0.113.11", PUBLIC, BEFORE),
                ip(WEB, "198.51.100.21", PUBLIC, AFTER)), List.of()))).isEmpty();
    }
}
