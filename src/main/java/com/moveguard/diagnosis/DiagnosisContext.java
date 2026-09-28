package com.moveguard.diagnosis;

import com.moveguard.asset.Asset;
import com.moveguard.asset.AssetIp;
import com.moveguard.asset.AssetIp.IpType;
import com.moveguard.asset.Dependency;
import com.moveguard.asset.Phase;
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
}
