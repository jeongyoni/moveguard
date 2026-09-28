package com.moveguard.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DnsRecord {

    private Long dnsId;
    private Long projectId;
    private String domain;
    private String recordType;
    private String value;
    private int ttl;
    private Phase phase;
}
