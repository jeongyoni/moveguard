package com.moveguard.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 자산에 설치된 소프트웨어 (이전 전/후) */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AssetSoftware {

    private Long softwareId;
    private Long assetId;
    private String product;
    private String role;
    private String version;
    private String releaseLine;
    private Phase phase;
}
