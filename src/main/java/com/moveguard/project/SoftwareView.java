package com.moveguard.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 상세 화면의 편집용 소프트웨어 행 (자산명 포함). */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SoftwareView {

    private Long softwareId;
    private Long assetId;
    private String assetName;
    private String product;
    private String role;
    private String version;
    private String releaseLine;
    private String phase;
}
