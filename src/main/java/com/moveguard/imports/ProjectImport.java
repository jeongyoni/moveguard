package com.moveguard.imports;

import java.util.List;

/**
 * 한 이전사업의 입력 데이터(검증 전 원시 값). 엑셀 업로드와 (2단계) 웹 폼이 공통으로 쓴다.
 * 자산은 이름으로 참조한다(IP·의존관계·소프트웨어·백업). 각 행은 원본 행 번호를 들고 있어
 * 검증 오류를 "시트·행"으로 가리킬 수 있다. 값은 문자열 그대로이며 변환은 검증 통과 후 수행한다.
 */
public record ProjectImport(
        ProjectRow project,
        List<AssetRow> assets,
        List<IpRow> ips,
        List<DependencyRow> dependencies,
        List<DnsRow> dnsRecords,
        List<CertRow> certificates,
        List<SoftwareRow> software,
        List<BackupRow> backups) {

    public record ProjectRow(int row, String name, String customerName, String sourceEnv,
                             String targetEnv, String status, String plannedDate) {
    }

    public record AssetRow(int row, String name, String assetType, String role,
                           String osName, String osVersion) {
    }

    public record IpRow(int row, String assetName, String address, String ipType,
                        String phase, String extWhitelisted, String note) {
    }

    public record DependencyRow(int row, String fromAsset, String toAsset, String targetAddress,
                                String targetPort, String protocol, String configLocation) {
    }

    public record DnsRow(int row, String domain, String recordType, String value,
                         String ttl, String phase) {
    }

    public record CertRow(int row, String domain, String issuer, String notAfter, String phase) {
    }

    public record SoftwareRow(int row, String assetName, String product, String role,
                              String version, String releaseLine, String phase) {
    }

    public record BackupRow(int row, String assetName, String lastBackupAt, String restoreTested,
                            String offsite, String phase) {
    }
}
