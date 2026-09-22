package com.moveguard.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Dependency {

    private Long dependencyId;
    private Long projectId;
    private Long fromAssetId;
    private Long toAssetId;
    private String targetAddress;
    private Integer targetPort;
    private String protocol;
    private String configLocation;
}
