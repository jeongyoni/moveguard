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
 * 시드 기반으로 가상 이전사업을 생성한다. 앱 서버 1~3대 + 공유 DB 1대로 토폴로지를 다양화한다.
 * 특징은 고정폭 집계값(numAppServers + 공유 DB 지표 + any* 집계)으로 표현해 데이터셋 컬럼을 일정하게 유지한다.
 * 같은 Random 스트림이면 같은 시나리오가 나온다(결정론적).
 */
@Component
public class ScenarioGenerator {

    private static final LocalDate PLANNED_DATE = LocalDate.of(2026, 10, 24);
    private static final String PUB_DB_BEFORE = "203.0.113.21";
    private static final String PUB_DB_AFTER = "198.51.100.21";
    private static final String DB_DOMAIN = "db.internal.example.com";

    public record Scenario(Map<String, String> features, DiagnosisContext context) {
    }

    public Scenario generate(Random rnd, List<CompatRelease> releases) {
        int numApps = 1 + rnd.nextInt(3); // 1..3

        // 공유 DB 관련 파라미터
        boolean ipChanges = rnd.nextBoolean();
        boolean whitelisted = rnd.nextBoolean();
        boolean dnsAfter = rnd.nextBoolean();
        boolean certExpiring = rnd.nextBoolean();
        boolean certAfter = rnd.nextBoolean();
        String dbFrom = pick(rnd, "5.7", "8.0");
        String dbTo = pick(rnd, "8.4", "9.6");

        long dbId = 100L;
        Asset db = new Asset(dbId, 1L, "db01", "SERVER", "DB");

        List<Asset> assets = new ArrayList<>();
        assets.add(db);
        List<AssetIp> ips = new ArrayList<>();
        List<Dependency> deps = new ArrayList<>();
        List<AssetSoftware> software = new ArrayList<>();

        // DB IP: 이전 후 공인IP가 바뀌면 새 주소, 유지면 그대로
        ips.add(new AssetIp(null, dbId, "10.10.1.21", IpType.PRIVATE, Phase.BEFORE, false));
        ips.add(new AssetIp(null, dbId, PUB_DB_BEFORE, IpType.PUBLIC, Phase.BEFORE, whitelisted));
        ips.add(new AssetIp(null, dbId, "172.31.20.21", IpType.PRIVATE, Phase.AFTER, false));
        ips.add(new AssetIp(null, dbId, ipChanges ? PUB_DB_AFTER : PUB_DB_BEFORE,
                IpType.PUBLIC, Phase.AFTER, false));
        software.add(sw(dbId, "mysql", dbFrom, Phase.BEFORE));
        software.add(sw(dbId, "mysql", dbTo, Phase.AFTER));

        boolean anyHardcoded = false;
        boolean anyPlaintext = false;
        boolean anyJavaxJump = false;
        boolean anyJavaBelowMin = false;

        for (int i = 0; i < numApps; i++) {
            long appId = 1L + i;
            Asset app = new Asset(appId, 1L, "web0" + (i + 1), "SERVER", "WEB");
            assets.add(app);

            boolean hardcoded = rnd.nextBoolean();
            boolean plaintext = rnd.nextBoolean();
            String tomcatFrom = pick(rnd, "9.0", "10.1");
            String tomcatTo = pick(rnd, "10.1", "11.0");
            String javaAfter = pick(rnd, "8", "11", "17");

            ips.add(new AssetIp(null, appId, "10.10.1.1" + i, IpType.PRIVATE, Phase.BEFORE, false));
            ips.add(new AssetIp(null, appId, "172.31.10.1" + i, IpType.PRIVATE, Phase.AFTER, false));

            String target = hardcoded ? PUB_DB_BEFORE : DB_DOMAIN;
            deps.add(new Dependency(10L + i, 1L, appId, dbId, target, 3306,
                    plaintext ? "HTTP" : "JDBC", "application.yml spring.datasource.url"));

            software.add(sw(appId, "tomcat", tomcatFrom, Phase.BEFORE));
            software.add(sw(appId, "tomcat", tomcatTo, Phase.AFTER));
            software.add(sw(appId, "java", "8", Phase.BEFORE));
            software.add(sw(appId, "java", javaAfter, Phase.AFTER));

            anyHardcoded |= hardcoded;
            anyPlaintext |= plaintext;
            anyJavaxJump |= major(tomcatFrom) <= 9 && major(tomcatTo) >= 10;
            int requiredJava = "11.0".equals(tomcatTo) ? 17 : 11;
            anyJavaBelowMin |= major(javaAfter) < requiredJava;
        }

        List<DnsRecord> dns = new ArrayList<>();
        if (ipChanges) {
            dns.add(new DnsRecord(null, 1L, "shop.example.com", "A", PUB_DB_BEFORE, 3600, Phase.BEFORE));
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

        DiagnosisContext context = new DiagnosisContext(1L, assets, ips, deps, dns, certs,
                PLANNED_DATE, software, releases);

        Map<String, String> features = new LinkedHashMap<>();
        features.put("numAppServers", String.valueOf(numApps));
        features.put("ipChanges", String.valueOf(ipChanges));
        features.put("whitelisted", String.valueOf(whitelisted));
        features.put("dnsAfter", String.valueOf(dnsAfter));
        features.put("certExpiring", String.valueOf(certExpiring));
        features.put("certAfter", String.valueOf(certAfter));
        features.put("dbFrom", dbFrom);
        features.put("dbTo", dbTo);
        features.put("anyHardcoded", String.valueOf(anyHardcoded));
        features.put("anyPlaintext", String.valueOf(anyPlaintext));
        features.put("anyJavaxJump", String.valueOf(anyJavaxJump));
        features.put("anyJavaBelowMin", String.valueOf(anyJavaBelowMin));

        return new Scenario(features, context);
    }

    private static AssetSoftware sw(long assetId, String product, String releaseLine, Phase phase) {
        return new AssetSoftware(null, assetId, product, null, releaseLine, releaseLine, phase);
    }

    private static int major(String releaseLine) {
        return Integer.parseInt(releaseLine.split("\\.")[0]);
    }

    @SafeVarargs
    private static <T> T pick(Random rnd, T... options) {
        return options[rnd.nextInt(options.length)];
    }
}
