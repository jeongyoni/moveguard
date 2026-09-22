package com.moveguard.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Asset {

    private Long assetId;
    private Long projectId;
    private String name;
    private String assetType;
    private String role;

    public boolean isServer() {
        return "SERVER".equals(assetType);
    }
}
