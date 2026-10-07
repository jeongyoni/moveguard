package com.moveguard.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 상세 화면의 편집용 DNS 행. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DnsView {

    private Long dnsId;
    private Long projectId;
    private String domain;
    private String recordType;
    private String value;
    private int ttl;
    private String phase;
}
