package com.moveguard.diagnosis.rule;

import com.moveguard.asset.Asset;
import com.moveguard.asset.AssetIp;
import com.moveguard.asset.AssetIp.IpType;
import com.moveguard.asset.Dependency;
import com.moveguard.asset.DnsRecord;
import com.moveguard.asset.Phase;
import com.moveguard.diagnosis.DiagnosisContext;
import java.util.List;

/** 규칙 단위 테스트용 공통 데이터 */
final class Fixtures {

    static final Asset WEB = new Asset(1L, 1L, "web01", "SERVER", "WEB");
    static final Asset DB = new Asset(2L, 1L, "db01", "SERVER", "DB");
    static final Asset PG = new Asset(3L, 1L, "pg-api", "EXTERNAL", null);

    private Fixtures() {
    }

    static AssetIp ip(Asset asset, String address, IpType type, Phase phase) {
        return new AssetIp(null, asset.getAssetId(), address, type, phase, false);
    }

    static AssetIp whitelistedPublicIp(Asset asset, String address, Phase phase) {
        return new AssetIp(null, asset.getAssetId(), address, IpType.PUBLIC, phase, true);
    }

    static Dependency dependency(Asset from, Asset to, String targetAddress) {
        return new Dependency(10L, 1L, from.getAssetId(), to.getAssetId(),
                targetAddress, 3306, "JDBC", "application.yml spring.datasource.url");
    }

    static DnsRecord aRecord(String domain, String value, int ttl, Phase phase) {
        return new DnsRecord(null, 1L, domain, "A", value, ttl, phase);
    }

    static DiagnosisContext context(List<AssetIp> ips, List<Dependency> dependencies) {
        return context(ips, dependencies, List.of());
    }

    static DiagnosisContext context(List<AssetIp> ips, List<Dependency> dependencies,
                                    List<DnsRecord> dnsRecords) {
        return new DiagnosisContext(1L, List.of(WEB, DB, PG), ips, dependencies, dnsRecords);
    }
}
