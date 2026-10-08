package com.moveguard.compat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** JDBC 드라이버↔DB 요구 버전 (수동 관리 매트릭스). */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DriverRequirement {

    private Long requirementId;
    private String dbProduct;       // 대상 DB 제품
    private String dbReleaseLine;   // 특정 라인(8.4 등), null이면 제품 전체
    private String driverProduct;   // 드라이버 제품명
    private String minVersion;      // 요구 최소 버전
    private String note;
    private boolean manual;
}
