package com.moveguard.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AssetIp {

    public enum IpType { PUBLIC, PRIVATE }

    private Long ipId;
    private Long assetId;
    private String address;
    private IpType ipType;
    private Phase phase;
    private boolean extWhitelisted;
}
