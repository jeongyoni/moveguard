package com.moveguard.diagnosis;

import com.moveguard.asset.Asset;
import com.moveguard.asset.AssetIp;
import com.moveguard.asset.AssetIp.IpType;
import com.moveguard.asset.AssetSoftware;
import com.moveguard.asset.Certificate;
import com.moveguard.asset.Dependency;
import com.moveguard.asset.DnsRecord;
import com.moveguard.asset.Phase;
import com.moveguard.compat.CompatRelease;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 한 이전사업의 진단 입력 데이터.
 * 규칙은 DB를 직접 조회하지 않고 이 객체만 참조한다.
 */
public class DiagnosisContext {

    private final Long projectId;
    private final Map<Long, Asset> assets;
    private final List<AssetIp> ips;
    private final List<Dependency> dependencies;
    private final List<DnsRecord> dnsRecords;
    private final List<Certificate> certificates;
    private final LocalDate plannedDate;
    private final List<AssetSoftware> software;
    private final Map<String, CompatRelease> releaseIndex;

    public DiagnosisContext(Long projectId, List<Asset> assets, List<AssetIp> ips,
                            List<Dependency> dependencies, List<DnsRecord> dnsRecords,
                            List<Certificate> certificates, LocalDate plannedDate) {
        this(projectId, assets, ips, dependencies, dnsRecords, certificates, plannedDate,
                List.of(), List.of());
    }

    public DiagnosisContext(Long projectId, List<Asset> assets, List<AssetIp> ips,
                            List<Dependency> dependencies, List<DnsRecord> dnsRecords,
                            List<Certificate> certificates, LocalDate plannedDate,
                            List<AssetSoftware> software, List<CompatRelease> releases) {
        this.projectId = projectId;
        this.assets = assets.stream()
                .collect(Collectors.toMap(Asset::getAssetId, Function.identity()));
        this.ips = List.copyOf(ips);
        this.dependencies = List.copyOf(dependencies);
        this.dnsRecords = List.copyOf(dnsRecords);
        this.certificates = List.copyOf(certificates);
        this.plannedDate = plannedDate;
        this.software = List.copyOf(software);
        this.releaseIndex = releases.stream().collect(Collectors.toMap(
                r -> releaseKey(r.getProduct(), r.getVersion()), Function.identity(),
                (a, b) -> a));
    }

    private static String releaseKey(String product, String releaseLine) {
        return product + "|" + releaseLine;
    }

    public Long projectId() {
        return projectId;
    }

    public Optional<Asset> asset(Long assetId) {
        return assetId == null ? Optional.empty() : Optional.ofNullable(assets.get(assetId));
    }

    public List<AssetIp> ips() {
        return ips;
    }

    public List<Dependency> dependencies() {
        return dependencies;
    }

    public List<DnsRecord> dnsRecords() {
        return dnsRecords;
    }

    public List<Certificate> certificates() {
        return certificates;
    }

    /** 이전 예정일 (미정이면 empty) */
    public Optional<LocalDate> plannedDate() {
        return Optional.ofNullable(plannedDate);
    }

    /** 자산이 해당 단계에서 주어진 주소를 갖고 있는지 */
    public boolean hasIp(Long assetId, String address, Phase phase) {
        return ips.stream().anyMatch(ip ->
                Objects.equals(ip.getAssetId(), assetId)
                        && ip.getPhase() == phase
                        && Objects.equals(ip.getAddress(), address));
    }

    /** 자산이 해당 단계에서 주어진 주소를 특정 종류(공인/사설)로 갖고 있는지 */
    public boolean hasIp(Long assetId, String address, Phase phase, IpType type) {
        return ips.stream().anyMatch(ip ->
                Objects.equals(ip.getAssetId(), assetId)
                        && ip.getPhase() == phase
                        && ip.getIpType() == type
                        && Objects.equals(ip.getAddress(), address));
    }

    /** 이전 전 공인IP였던 주소가 이전 후 같은 자산에 남아 있지 않으면 true */
    public boolean isChangingPublicIp(Long assetId, String address) {
        return hasIp(assetId, address, Phase.BEFORE, IpType.PUBLIC)
                && !hasIp(assetId, address, Phase.AFTER);
    }

    /** 주어진 주소가 어느 단계에서든 공인IP로 등록되어 있으면 true */
    public boolean isPublicIp(String address) {
        return ips.stream().anyMatch(ip ->
                ip.getIpType() == IpType.PUBLIC && Objects.equals(ip.getAddress(), address));
    }

    /** 자산을 특정하지 않고, 주어진 주소가 이전 전 어느 자산의 공인IP였고 이전 후 그 자산에 남아 있지 않으면 true */
    public boolean isChangingPublicIpAddress(String address) {
        return ips.stream()
                .filter(ip -> ip.getPhase() == Phase.BEFORE
                        && ip.getIpType() == IpType.PUBLIC
                        && Objects.equals(ip.getAddress(), address))
                .anyMatch(ip -> !hasIp(ip.getAssetId(), address, Phase.AFTER));
    }

    /** 해당 도메인의 특정 단계 DNS 레코드가 하나라도 있으면 true */
    public boolean hasDnsRecord(String domain, Phase phase) {
        return dnsRecords.stream().anyMatch(record ->
                Objects.equals(record.getDomain(), domain) && record.getPhase() == phase);
    }

    /** 해당 도메인의 특정 단계 인증서가 하나라도 있으면 true */
    public boolean hasCertificate(String domain, Phase phase) {
        return certificates.stream().anyMatch(cert ->
                Objects.equals(cert.getDomain(), domain) && cert.getPhase() == phase);
    }

    public List<AssetSoftware> software() {
        return software;
    }

    /** 특정 단계의 설치 소프트웨어 */
    public List<AssetSoftware> software(Phase phase) {
        return software.stream().filter(s -> s.getPhase() == phase).toList();
    }

    /** 한 자산의 특정 제품·단계 소프트웨어 */
    public Optional<AssetSoftware> software(Long assetId, String product, Phase phase) {
        return software.stream()
                .filter(s -> Objects.equals(s.getAssetId(), assetId)
                        && Objects.equals(s.getProduct(), product)
                        && s.getPhase() == phase)
                .findFirst();
    }

    /** 제품·릴리스 라인에 해당하는 호환성 기준 릴리스 */
    public Optional<CompatRelease> release(String product, String releaseLine) {
        return Optional.ofNullable(releaseIndex.get(releaseKey(product, releaseLine)));
    }
}
