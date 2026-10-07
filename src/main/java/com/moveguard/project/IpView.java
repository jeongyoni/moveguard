package com.moveguard.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 상세 화면의 편집용 IP 행 (자산명 포함). */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IpView {

    private Long ipId;
    private Long assetId;
    private String assetName;
    private String address;
    private String ipType;
    private String phase;
    private boolean extWhitelisted;
    private String note;
}
