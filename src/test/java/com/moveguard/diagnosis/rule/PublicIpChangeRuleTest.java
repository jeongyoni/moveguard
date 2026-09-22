package com.moveguard.diagnosis.rule;

import static com.moveguard.asset.AssetIp.IpType.PRIVATE;
import static com.moveguard.asset.AssetIp.IpType.PUBLIC;
import static com.moveguard.asset.Phase.AFTER;
import static com.moveguard.asset.Phase.BEFORE;
import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.asset.Asset;
import com.moveguard.asset.AssetIp;
import com.moveguard.asset.AssetIp.IpType;
import com.moveguard.asset.Dependency;
import com.moveguard.asset.Phase;
import com.moveguard.diagnosis.DiagnosisContext;
import com.moveguard.diagnosis.Finding;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PublicIpChangeRuleTest {

    private static final Asset WEB = new Asset(1L, 1L, "web01", "SERVER", "WEB");
    private static final Asset DB = new Asset(2L, 1L, "db01", "SERVER", "DB");

    private final PublicIpChangeRule rule = new PublicIpChangeRule();

    @Test
    @DisplayName("DB 공인IP로 접속 중이고 이전 후 그 IP가 사라지면 IP-01 발견")
    void detectsWhenPublicIpDisappears() {
        List<AssetIp> ips = List.of(
                ip(DB, "203.0.113.21", PUBLIC, BEFORE),
                ip(DB, "172.31.20.21", PRIVATE, AFTER));

        List<Finding> findings = rule.evaluate(context(ips, dependency("203.0.113.21")));

        assertThat(findings).hasSize(1);
        Finding finding = findings.get(0);
        assertThat(finding.ruleCode()).isEqualTo("IP-01");
        assertThat(finding.dependencyId()).isEqualTo(10L);
        assertThat(finding.params())
                .containsEntry("asset", "web01")
                .containsEntry("target", "db01")
                .containsEntry("address", "203.0.113.21")
                .containsEntry("port", "3306");
    }

    @Test
    @DisplayName("이전 후에도 같은 공인IP를 유지하면 해당 없음")
    void ignoresWhenPublicIpKept() {
        List<AssetIp> ips = List.of(
                ip(DB, "203.0.113.21", PUBLIC, BEFORE),
                ip(DB, "203.0.113.21", PUBLIC, AFTER));

        assertThat(rule.evaluate(context(ips, dependency("203.0.113.21")))).isEmpty();
    }

    @Test
    @DisplayName("도메인으로 접속하면 해당 없음")
    void ignoresDomainTarget() {
        List<AssetIp> ips = List.of(ip(DB, "203.0.113.21", PUBLIC, BEFORE));

        assertThat(rule.evaluate(context(ips, dependency("db.internal.example.com")))).isEmpty();
    }

    @Test
    @DisplayName("사설IP로 접속하면 해당 없음")
    void ignoresPrivateIpTarget() {
        List<AssetIp> ips = List.of(
                ip(DB, "10.10.1.21", PRIVATE, BEFORE),
                ip(DB, "172.31.20.21", PRIVATE, AFTER));

        assertThat(rule.evaluate(context(ips, dependency("10.10.1.21")))).isEmpty();
    }

    private static AssetIp ip(Asset asset, String address, IpType type, Phase phase) {
        return new AssetIp(null, asset.getAssetId(), address, type, phase, false);
    }

    private static Dependency dependency(String targetAddress) {
        return new Dependency(10L, 1L, WEB.getAssetId(), DB.getAssetId(),
                targetAddress, 3306, "JDBC", "application.yml spring.datasource.url");
    }

    private static DiagnosisContext context(List<AssetIp> ips, Dependency dependency) {
        return new DiagnosisContext(1L, List.of(WEB, DB), ips, List.of(dependency));
    }
}
