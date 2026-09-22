package com.moveguard.diagnosis;

import com.moveguard.asset.Asset;
import com.moveguard.asset.AssetIp;
import com.moveguard.asset.AssetIp.IpType;
import com.moveguard.asset.Dependency;
import com.moveguard.asset.Phase;
import java.util.List;
import java.util.Map;
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

    public DiagnosisContext(Long projectId, List<Asset> assets,
                            List<AssetIp> ips, List<Dependency> dependencies) {
        this.projectId = projectId;
        this.assets = assets.stream()
                .collect(Collectors.toMap(Asset::getAssetId, Function.identity()));
        this.ips = List.copyOf(ips);
        this.dependencies = List.copyOf(dependencies);
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

    /** 이전 전 공인IP였던 주소가 이전 후 같은 자산에 남아 있지 않으면 true */
    public boolean isChangingPublicIp(Long assetId, String address) {
        boolean wasPublic = ips.stream().anyMatch(ip ->
                ip.getAssetId().equals(assetId)
                        && ip.getPhase() == Phase.BEFORE
                        && ip.getIpType() == IpType.PUBLIC
                        && ip.getAddress().equals(address));

        boolean keptAfter = ips.stream().anyMatch(ip ->
                ip.getAssetId().equals(assetId)
                        && ip.getPhase() == Phase.AFTER
                        && ip.getAddress().equals(address));

        return wasPublic && !keptAfter;
    }
}
