package com.moveguard.compat;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 호환성 기준 제품 (동기화 대상) */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CompatProduct {

    private String product;
    private String label;
    private String category;
    private String versionCommand;
    private String sourceUrl;
    private LocalDateTime fetchedAt;
    private String fetchStatus;
}
