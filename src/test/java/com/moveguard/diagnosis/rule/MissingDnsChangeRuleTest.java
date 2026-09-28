package com.moveguard.diagnosis.rule;

import static com.moveguard.asset.AssetIp.IpType.PUBLIC;
import static com.moveguard.asset.Phase.AFTER;
import static com.moveguard.asset.Phase.BEFORE;
import static com.moveguard.diagnosis.rule.Fixtures.WEB;
import static com.moveguard.diagnosis.rule.Fixtures.aRecord;
import static com.moveguard.diagnosis.rule.Fixtures.context;
import static com.moveguard.diagnosis.rule.Fixtures.ip;
import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.asset.AssetIp;
import com.moveguard.diagnosis.Finding;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MissingDnsChangeRuleTest {

    private final MissingDnsChangeRule rule = new MissingDnsChangeRule();

    private final List<AssetIp> changingIp = List.of(
            ip(WEB, "203.0.113.11", PUBLIC, BEFORE),
            ip(WEB, "198.51.100.21", PUBLIC, AFTER));

    @Test
    @DisplayName("변경되는 공인IP 레코드에 이전 후 레코드가 없으면 DNS-02 발견")
    void detectsMissingAfterRecord() {
        List<Finding> findings = rule.evaluate(context(changingIp, List.of(),
                List.of(aRecord("shop.example.com", "203.0.113.11", 300, BEFORE))));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).params())
                .containsEntry("domain", "shop.example.com")
                .containsEntry("address", "203.0.113.11");
    }

    @Test
    @DisplayName("이전 후 레코드가 등록되어 있으면 해당 없음")
    void ignoresWhenAfterRecordExists() {
        assertThat(rule.evaluate(context(changingIp, List.of(), List.of(
                aRecord("shop.example.com", "203.0.113.11", 300, BEFORE),
                aRecord("shop.example.com", "198.51.100.21", 300, AFTER))))).isEmpty();
    }

    @Test
    @DisplayName("변경되지 않는 공인IP 레코드는 해당 없음")
    void ignoresStableIp() {
        List<AssetIp> keptIp = List.of(
                ip(WEB, "203.0.113.11", PUBLIC, BEFORE),
                ip(WEB, "203.0.113.11", PUBLIC, AFTER));
        assertThat(rule.evaluate(context(keptIp, List.of(),
                List.of(aRecord("shop.example.com", "203.0.113.11", 300, BEFORE))))).isEmpty();
    }
}
