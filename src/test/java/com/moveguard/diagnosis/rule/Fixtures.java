package com.moveguard.diagnosis.rule;

import com.moveguard.asset.Asset;
import com.moveguard.asset.AssetIp;
import com.moveguard.asset.AssetIp.IpType;
import com.moveguard.asset.AssetSoftware;
import com.moveguard.asset.BackupPlan;
import com.moveguard.asset.Certificate;
import com.moveguard.asset.Dependency;
import com.moveguard.asset.DnsRecord;
import com.moveguard.asset.Phase;
import com.moveguard.compat.CompatRelease;
import com.moveguard.compat.DriverRequirement;
import com.moveguard.diagnosis.DiagnosisContext;
import java.time.LocalDate;
import java.util.List;

/** 규칙 단위 테스트용 공통 데이터 */
final class Fixtures {

    static final Asset WEB = new Asset(1L, 1L, "web01", "SERVER", "WEB");
    static final Asset DB = new Asset(2L, 1L, "db01", "SERVER", "DB");
    static final Asset PG = new Asset(3L, 1L, "pg-api", "EXTERNAL", null);

    static final LocalDate PLANNED_DATE = LocalDate.of(2026, 10, 24);

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

    static Certificate cert(String domain, LocalDate notAfter, Phase phase) {
        return new Certificate(null, 1L, domain, "Test CA", notAfter, phase);
    }

    static DiagnosisContext context(List<AssetIp> ips, List<Dependency> dependencies) {
        return context(ips, dependencies, List.of());
    }

    static DiagnosisContext context(List<AssetIp> ips, List<Dependency> dependencies,
                                    List<DnsRecord> dnsRecords) {
        return new DiagnosisContext(1L, List.of(WEB, DB, PG), ips, dependencies, dnsRecords,
                List.of(), PLANNED_DATE);
    }

    static DiagnosisContext certContext(List<Certificate> certificates, LocalDate plannedDate) {
        return new DiagnosisContext(1L, List.of(WEB, DB, PG), List.of(), List.of(), List.of(),
                certificates, plannedDate);
    }

    static AssetSoftware sw(Asset asset, String product, String version, String releaseLine, Phase phase) {
        return new AssetSoftware(null, asset.getAssetId(), product, null, version, releaseLine, phase);
    }

    /** eol 여부·만료일·최소 Java만 지정하는 간이 릴리스 */
    static CompatRelease release(String product, String version, boolean eol, LocalDate eolDate,
                                 String minJavaVersion) {
        return new CompatRelease(null, product, version, version, null, false, eol, eolDate,
                null, !eol, null, minJavaVersion, false);
    }

    /** 연장 지원 종료일(extSupportDate)까지 지정하는 릴리스 (Oracle 등) */
    static CompatRelease releaseExt(String product, String version, boolean eol, LocalDate eolDate,
                                    LocalDate extSupportDate) {
        return new CompatRelease(null, product, version, version, null, false, eol, eolDate,
                extSupportDate, !eol, null, null, false);
    }

    static DiagnosisContext compatContext(List<AssetSoftware> software, List<CompatRelease> releases,
                                          LocalDate plannedDate) {
        return new DiagnosisContext(1L, List.of(WEB, DB, PG), List.of(), List.of(), List.of(),
                List.of(), plannedDate, software, releases);
    }

    static DriverRequirement driverReq(String dbProduct, String dbReleaseLine,
                                       String driverProduct, String minVersion) {
        return new DriverRequirement(null, dbProduct, dbReleaseLine, driverProduct, minVersion, null, true);
    }

    static DiagnosisContext driverContext(List<AssetSoftware> software,
                                          List<DriverRequirement> requirements) {
        return new DiagnosisContext(1L, List.of(WEB, DB, PG), List.of(), List.of(), List.of(),
                List.of(), PLANNED_DATE, software, List.of(), List.of(), requirements);
    }

    static BackupPlan backup(Asset asset, LocalDate lastBackupAt, boolean restoreTested,
                             boolean offsite, Phase phase) {
        return new BackupPlan(null, asset.getAssetId(), lastBackupAt, restoreTested, offsite, phase);
    }

    static DiagnosisContext backupContext(List<BackupPlan> backups, LocalDate plannedDate) {
        return new DiagnosisContext(1L, List.of(WEB, DB, PG), List.of(), List.of(), List.of(),
                List.of(), plannedDate, List.of(), List.of(), backups);
    }
}
