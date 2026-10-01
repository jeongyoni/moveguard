package com.moveguard.diagnosis.rule;

import static com.moveguard.asset.AssetIp.IpType.PRIVATE;
import static com.moveguard.asset.AssetIp.IpType.PUBLIC;
import static com.moveguard.asset.Phase.AFTER;
import static com.moveguard.asset.Phase.BEFORE;
import static com.moveguard.diagnosis.rule.Fixtures.DB;
import static com.moveguard.diagnosis.rule.Fixtures.WEB;
import static com.moveguard.diagnosis.rule.Fixtures.context;
import static com.moveguard.diagnosis.rule.Fixtures.ip;
import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.diagnosis.Finding;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MissingAfterIpRuleTest {

    private final MissingAfterIpRule rule = new MissingAfterIpRule();

    @Test
    @DisplayName("이전 전 IP만 있고 이전 후 IP가 없으면 IP-05 발견")
    void detectsMissingAfterIp() {
        List<Finding> findings = rule.evaluate(context(
                List.of(ip(WEB, "10.10.1.11", PRIVATE, BEFORE),
                        ip(WEB, "203.0.113.11", PUBLIC, BEFORE)),
                List.of()));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).params()).containsEntry("asset", "web01");
    }

    @Test
    @DisplayName("이전 후 IP가 등록되어 있으면 해당 없음")
    void ignoresWhenAfterIpExists() {
        assertThat(rule.evaluate(context(
                List.of(ip(WEB, "10.10.1.11", PRIVATE, BEFORE),
                        ip(WEB, "172.31.10.11", PRIVATE, AFTER)),
                List.of()))).isEmpty();
    }

    @Test
    @DisplayName("자산이 여럿일 때 이전 후 IP 없는 자산만 탐지")
    void detectsOnlyAssetsMissingAfter() {
        List<Finding> findings = rule.evaluate(context(
                List.of(ip(WEB, "10.10.1.11", PRIVATE, BEFORE),
                        ip(WEB, "172.31.10.11", PRIVATE, AFTER),
                        ip(DB, "10.10.1.21", PRIVATE, BEFORE)),
                List.of()));

        assertThat(findings).extracting(f -> f.params().get("asset")).containsExactly("db01");
    }
}
