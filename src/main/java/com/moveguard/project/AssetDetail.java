package com.moveguard.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 상세 화면의 편집용 자산 행 (os 포함, id 포함). */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AssetDetail {

    private Long assetId;
    private Long projectId;
    private String name;
    private String assetType;
    private String role;
    private String osName;
    private String osVersion;
}
