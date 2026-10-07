package com.moveguard.project;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 상세 화면의 편집용 인증서 행. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CertView {

    private Long certId;
    private Long projectId;
    private String domain;
    private String issuer;
    private LocalDate notAfter;
    private String phase;
}
