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

    /**
     * 기준일({@code asOf}) 시점에 연장 지원(Extended Support) 구간인지.
     * 활성(일반) 지원은 끝났지만(eol) 연장 지원 종료일은 아직 지나지 않은 상태.
     * Oracle 등 연장 지원 체계가 있는 제품에서만 참이 될 수 있다.
     */
    public boolean inExtendedSupport(LocalDate asOf) {
        return eol && extSupportDate != null && asOf != null && !extSupportDate.isBefore(asOf);
    }
}
