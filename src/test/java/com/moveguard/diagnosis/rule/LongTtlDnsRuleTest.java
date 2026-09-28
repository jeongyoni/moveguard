package com.moveguard.diagnosis.rule;

import static com.moveguard.asset.AssetIp.IpType.PUBLIC;
import static com.moveguard.asset.Phase.AFTER;
import static com.moveguard.asset.Phase.BEFORE;
import static com.moveguard.diagnosis.rule.Fixtures.WEB;
import static com.moveguard.diagnosis.rule.Fixtures.aRecord;
import static com.moveguard.diagnosis.rule.Fixtures.context;
import static com.moveguard.diagnosis.rule.Fixtures.ip;
import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.diagnosis.Finding;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LongTtlDnsRuleTest {

    private final LongTtlDnsRule rule = new LongTtlDnsRule();

    private final List<com.moveguard.asset.AssetIp> changingIp = List.of(
            ip(WEB, "203.0.113.11", PUBLIC, BEFORE),
            ip(WEB, "198.51.100.21", PUBLIC, AFTER));

    @Test
    @DisplayName("변경되는 공인IP를 가리키는 A 레코드의 TTL이 길면 DNS-01 발견")
    void detectsLongTtl() {
        List<Finding> findings = rule.evaluate(context(changingIp, List.of(),
                List.of(aRecord("shop.example.com", "203.0.113.11", 3600, BEFORE))));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).params())
                .containsEntry("domain", "shop.example.com")
                .containsEntry("address", "203.0.113.11")
                .containsEntry("ttl", "3600");
    }

    @Test
    @DisplayName("TTL이 임계값 이하면 해당 없음")
    void ignoresShortTtl() {
        assertThat(rule.evaluate(context(changingIp, List.of(),
                List.of(aRecord("shop.example.com", "203.0.113.11", 300, BEFORE))))).isEmpty();
    }

    @Test
    @DisplayName("변경되지 않는 공인IP를 가리키면 해당 없음")
    void ignoresStableIp() {
        List<com.moveguard.asset.AssetIp> keptIp = List.of(
                ip(WEB, "203.0.113.11", PUBLIC, BEFORE),
                ip(WEB, "203.0.113.11", PUBLIC, AFTER));
        assertThat(rule.evaluate(context(keptIp, List.of(),
                List.of(aRecord("shop.example.com", "203.0.113.11", 3600, BEFORE))))).isEmpty();
    }

    @Test
    @DisplayName("이전 후(AFTER) 레코드는 검사 대상이 아니다")
    void ignoresAfterPhaseRecord() {
        assertThat(rule.evaluate(context(changingIp, List.of(),
                List.of(aRecord("shop.example.com", "198.51.100.21", 3600, AFTER))))).isEmpty();
    }
}
