package com.moveguard.diagnosis;

import com.moveguard.asset.Asset;
import com.moveguard.asset.AssetIp;
import com.moveguard.asset.AssetSoftware;
import com.moveguard.asset.BackupPlan;
import com.moveguard.asset.Phase;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 진단이 무엇을 보고 판단했는지 화면에 보여주기 위한 입력 요약.
 * 진단은 자산을 스캔하는 게 아니라 <b>이전 전(BEFORE)과 이전 후(AFTER) 구성을 비교</b>하므로,
 * 전/후를 나란히 놓는 형태로 정리한다.
 */
public record DiagnosisInput(
        List<AssetInput> assets,
        List<DependencyInput> dependencies,
        List<DnsInput> dnsRecords,
        List<CertInput> certificates,
        List<BackupInput> backups) {

    public record AssetInput(String name, String assetType, String role,
                             List<String> beforeIps, List<String> afterIps,
                             List<Change> software) {
    }

    /** 제품 하나의 전/후 버전. changed면 화면에서 강조한다. */
    public record Change(String product, String before, String after, boolean changed) {
    }

    public record DependencyInput(String from, String to, String address, Integer port,
                                  String protocol, String configLocation) {
    }

    public record DnsInput(String domain, String recordType, String phase, String value, int ttl) {
    }

    public record CertInput(String domain, String issuer, LocalDate notAfter, String phase) {
    }

    public record BackupInput(String assetName, String phase, LocalDate lastBackupAt,
                              boolean restoreTested, boolean offsite) {
    }

    public static DiagnosisInput of(DiagnosisContext context) {
        List<AssetInput> assets = context.assets().stream()
                .map(asset -> new AssetInput(
                        asset.getName(), asset.getAssetType(), asset.getRole(),
                        describeIps(context, asset, Phase.BEFORE),
                        describeIps(context, asset, Phase.AFTER),
                        softwareChanges(context, asset)))
                .toList();

        List<DependencyInput> dependencies = context.dependencies().stream()
                .map(d -> new DependencyInput(
                        assetName(context, d.getFromAssetId()),
                        assetName(context, d.getToAssetId()),
                        d.getTargetAddress(), d.getTargetPort(), d.getProtocol(),
                        d.getConfigLocation()))
                .toList();

        List<DnsInput> dns = context.dnsRecords().stream()
                .map(r -> new DnsInput(r.getDomain(), r.getRecordType(), phaseLabel(r.getPhase()),
                        r.getValue(), r.getTtl()))
                .toList();

        List<CertInput> certificates = context.certificates().stream()
                .map(c -> new CertInput(c.getDomain(), c.getIssuer(), c.getNotAfter(),
                        phaseLabel(c.getPhase())))
                .toList();

        List<BackupInput> backups = new ArrayList<>();
        for (Phase phase : Phase.values()) {
            for (BackupPlan b : context.backups(phase)) {
                backups.add(new BackupInput(assetName(context, b.getAssetId()), phaseLabel(phase),
                        b.getLastBackupAt(), b.isRestoreTested(), b.isOffsite()));
            }
        }

        return new DiagnosisInput(assets, dependencies, dns, certificates, backups);
    }

    /** "203.0.113.11 공인 · 외부 허용목록" 형태로 한 줄씩 */
    private static List<String> describeIps(DiagnosisContext context, Asset asset, Phase phase) {
        List<String> described = new ArrayList<>();
        for (AssetIp ip : context.ips()) {
            if (!Objects.equals(ip.getAssetId(), asset.getAssetId()) || ip.getPhase() != phase) {
                continue;
            }
            String kind = ip.getIpType() == AssetIp.IpType.PUBLIC ? "공인" : "사설";
            described.add(ip.getAddress() + " " + kind
                    + (ip.isExtWhitelisted() ? " · 외부 허용목록" : ""));
        }
        return described;
    }

    /** 제품별로 전/후 버전을 짝지어 비교 */
    private static List<Change> softwareChanges(DiagnosisContext context, Asset asset) {
        Set<String> products = new LinkedHashSet<>();
        for (AssetSoftware s : context.software()) {
            if (Objects.equals(s.getAssetId(), asset.getAssetId())) {
                products.add(s.getProduct());
            }
        }

        List<Change> changes = new ArrayList<>();
        for (String product : products) {
            String before = version(context, asset, product, Phase.BEFORE);
            String after = version(context, asset, product, Phase.AFTER);
            changes.add(new Change(product, before, after, !Objects.equals(before, after)));
        }
        return changes;
    }

    private static String version(DiagnosisContext context, Asset asset, String product,
                                  Phase phase) {
        return context.software(asset.getAssetId(), product, phase)
                .map(AssetSoftware::getVersion)
                .orElse("-");
    }

    private static String assetName(DiagnosisContext context, Long assetId) {
        return context.asset(assetId).map(Asset::getName).orElse("-");
    }

    private static String phaseLabel(Phase phase) {
        return phase == Phase.BEFORE ? "이전 전" : "이전 후";
    }
}
