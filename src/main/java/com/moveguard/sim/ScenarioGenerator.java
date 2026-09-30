package com.moveguard.sim;

import com.moveguard.asset.Asset;
import com.moveguard.asset.AssetIp;
import com.moveguard.asset.AssetIp.IpType;
import com.moveguard.asset.AssetSoftware;
import com.moveguard.asset.Certificate;
import com.moveguard.asset.Dependency;
import com.moveguard.asset.DnsRecord;
import com.moveguard.asset.Phase;
import com.moveguard.compat.CompatRelease;
import com.moveguard.diagnosis.DiagnosisContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * 시드 기반으로 가상 이전사업(web·db 2자산)을 생성한다.
 * 생성에 사용한 파라미터(knobs)를 features로 함께 반환해 데이터셋의 입력 특징으로 쓴다.
 * 같은 (seed, index)면 같은 시나리오가 나온다(결정론적).
 */
@Component
public class ScenarioGenerator {

    private static final LocalDate PLANNED_DATE = LocalDate.of(2026, 10, 24);
    private static final String PUB_DB_BEFORE = "203.0.113.21";
    private static final String PUB_DB_AFTER = "198.51.100.21";
    private static final String DB_DOMAIN = "db.internal.example.com";

    private static final Asset WEB = new Asset(1L, 1L, "web01", "SERVER", "WEB");
    private static final Asset DB = new Asset(2L, 1L, "db01", "SERVER", "DB");

    public record Scenario(Map<String, String> features, DiagnosisContext context) {
    }

    public Scenario generate(Random rnd, List<CompatRelease> releases) {
        boolean ipChanges = rnd.nextBoolean();
        boolean whitelisted = rnd.nextBoolean();
        boolean hardcoded = rnd.nextBoolean();
        String protocol = pick(rnd, "JDBC", "HTTP");
        int dnsTtl = pick(rnd, 300, 3600);
        boolean dnsAfter = rnd.nextBoolean();
        boolean certExpiring = rnd.nextBoolean();
        boolean certAfter = rnd.nextBoolean();
        String tomcatFrom = pick(rnd, "9.0", "10.1");
        String tomcatTo = pick(rnd, "10.1", "11.0");
        String javaAfter = pick(rnd, "8", "11", "17");
        String dbFrom = pick(rnd, "5.7", "8.0");
        String dbTo = pick(rnd, "8.4", "9.6");

        List<AssetIp> ips = new ArrayList<>();
        ips.add(new AssetIp(null, WEB.getAssetId(), "10.10.1.11", IpType.PRIVATE, Phase.BEFORE, false));
        ips.add(new AssetIp(null, DB.getAssetId(), "10.10.1.21", IpType.PRIVATE, Phase.BEFORE, false));
        ips.add(new AssetIp(null, DB.getAssetId(), PUB_DB_BEFORE, IpType.PUBLIC, Phase.BEFORE, whitelisted));
        ips.add(new AssetIp(null, WEB.getAssetId(), "172.31.10.11", IpType.PRIVATE, Phase.AFTER, false));
        ips.add(new AssetIp(null, DB.getAssetId(), "172.31.20.21", IpType.PRIVATE, Phase.AFTER, false));
        // 이전 후 db 공인IP: 바뀌면 새 주소, 유지면 이전 주소 그대로
        ips.add(new AssetIp(null, DB.getAssetId(), ipChanges ? PUB_DB_AFTER : PUB_DB_BEFORE,
                IpType.PUBLIC, Phase.AFTER, false));

        String target = hardcoded ? PUB_DB_BEFORE : DB_DOMAIN;
        Dependency dep = new Dependency(10L, 1L, WEB.getAssetId(), DB.getAssetId(),
                target, 3306, protocol, "application.yml spring.datasource.url");

        List<DnsRecord> dns = new ArrayList<>();
        if (ipChanges) {
            dns.add(new DnsRecord(null, 1L, "shop.example.com", "A", PUB_DB_BEFORE, dnsTtl, Phase.BEFORE));
            if (dnsAfter) {
                dns.add(new DnsRecord(null, 1L, "shop.example.com", "A", PUB_DB_AFTER, 300, Phase.AFTER));
            }
        }

        List<Certificate> certs = new ArrayList<>();
        certs.add(new Certificate(null, 1L, "shop.example.com", "Test CA",
                certExpiring ? PLANNED_DATE.plusDays(10) : PLANNED_DATE.plusDays(400), Phase.BEFORE));
        if (certAfter) {
            certs.add(new Certificate(null, 1L, "shop.example.com", "Test CA",
                    PLANNED_DATE.plusDays(400), Phase.AFTER));
        }

        List<AssetSoftware> software = new ArrayList<>();
        software.add(sw(WEB, "tomcat", tomcatFrom, Phase.BEFORE));
        software.add(sw(WEB, "tomcat", tomcatTo, Phase.AFTER));
        software.add(sw(WEB, "java", "8", Phase.BEFORE));
        software.add(sw(WEB, "java", javaAfter, Phase.AFTER));
        software.add(sw(DB, "mysql", dbFrom, Phase.BEFORE));
        software.add(sw(DB, "mysql", dbTo, Phase.AFTER));

        DiagnosisContext context = new DiagnosisContext(1L, List.of(WEB, DB),
                ips, List.of(dep), dns, certs, PLANNED_DATE, software, releases);

        Map<String, String> features = new LinkedHashMap<>();
        features.put("ipChanges", String.valueOf(ipChanges));
        features.put("whitelisted", String.valueOf(whitelisted));
        features.put("hardcoded", String.valueOf(hardcoded));
        features.put("protocol", protocol);
        features.put("dnsTtl", String.valueOf(dnsTtl));
        features.put("dnsAfter", String.valueOf(dnsAfter));
        features.put("certExpiring", String.valueOf(certExpiring));
        features.put("certAfter", String.valueOf(certAfter));
        features.put("tomcatFrom", tomcatFrom);
        features.put("tomcatTo", tomcatTo);
        features.put("javaAfter", javaAfter);
        features.put("dbFrom", dbFrom);
        features.put("dbTo", dbTo);

        return new Scenario(features, context);
    }

    private static AssetSoftware sw(Asset asset, String product, String releaseLine, Phase phase) {
        return new AssetSoftware(null, asset.getAssetId(), product, null, releaseLine, releaseLine, phase);
    }

    @SafeVarargs
    private static <T> T pick(Random rnd, T... options) {
        return options[rnd.nextInt(options.length)];
    }
}
