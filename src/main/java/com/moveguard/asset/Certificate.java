package com.moveguard.asset;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Certificate {

    private Long certId;
    private Long projectId;
    private String domain;
    private String issuer;
    private LocalDate notAfter;
    private Phase phase;
}
