package com.moveguard.compat;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 제품 릴리스별 지원 정보 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CompatRelease {

    private Long releaseId;
    private String product;
    private String version;
    private String label;
    private LocalDate releaseDate;
    private boolean lts;
    private boolean eol;
    private LocalDate eolDate;
    private LocalDate extSupportDate;
    private boolean maintained;
    private String latestVersion;
    private String minJavaVersion;
    private boolean manual;
}
