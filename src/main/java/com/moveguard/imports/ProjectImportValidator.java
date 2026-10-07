package com.moveguard.imports;

import com.moveguard.imports.ProjectImport.AssetRow;
import com.moveguard.imports.ProjectImport.BackupRow;
import com.moveguard.imports.ProjectImport.CertRow;
import com.moveguard.imports.ProjectImport.DependencyRow;
import com.moveguard.imports.ProjectImport.DnsRow;
import com.moveguard.imports.ProjectImport.IpRow;
import com.moveguard.imports.ProjectImport.ProjectRow;
import com.moveguard.imports.ProjectImport.SoftwareRow;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 업로드 입력 전건 검증. 하나라도 오류면 저장하지 않는다(오류 전부 수집해 반환). */
@Component
public class ProjectImportValidator {

    static final String S_PROJECT = "사업";
    static final String S_ASSET = "자산";
    static final String S_IP = "IP";
    static final String S_DEP = "의존관계";
    static final String S_DNS = "DNS";
    static final String S_CERT = "인증서";
    static final String S_SW = "소프트웨어";
    static final String S_BACKUP = "백업";

    private static final Set<String> STATUS = Set.of("PLAN", "READY", "BLOCKED", "DONE");
    private static final Set<String> ASSET_TYPE = Set.of("SERVER", "EXTERNAL");
    private static final Set<String> ROLE = Set.of("WEB", "WAS", "DB", "ETC");
    private static final Set<String> IP_TYPE = Set.of("PUBLIC", "PRIVATE");
    private static final Set<String> PHASE = Set.of("BEFORE", "AFTER");
    private static final Set<String> DNS_TYPE = Set.of("A", "AAAA", "CNAME", "MX", "TXT");

    public List<ImportError> validate(ProjectImport in) {
        List<ImportError> errors = new ArrayList<>();

        validateProject(in.project(), errors);
        Set<String> assetNames = validateAssets(in.assets(), errors);
        validateIps(in.ips(), assetNames, errors);
        validateDependencies(in.dependencies(), assetNames, errors);
        validateDns(in.dnsRecords(), errors);
        validateCerts(in.certificates(), errors);
        validateSoftware(in.software(), assetNames, errors);
        validateBackups(in.backups(), assetNames, errors);
        return errors;
    }

    private void validateProject(ProjectRow p, List<ImportError> errors) {
        if (p == null) {
            errors.add(new ImportError(S_PROJECT, 0, "사업 정보가 없습니다"));
            return;
        }
        if (ImportValues.isBlank(p.name())) {
            errors.add(new ImportError(S_PROJECT, p.row(), "사업명은 필수입니다"));
        }
        if (ImportValues.isBlank(p.customerName())) {
            errors.add(new ImportError(S_PROJECT, p.row(), "고객사는 필수입니다"));
        }
        if (!ImportValues.isBlank(p.status()) && !STATUS.contains(p.status().trim())) {
            errors.add(new ImportError(S_PROJECT, p.row(), "상태는 PLAN/READY/BLOCKED/DONE 중 하나여야 합니다"));
        }
        if (!ImportValues.isBlank(p.plannedDate()) && ImportValues.toDateOrNull(p.plannedDate()) == null) {
            errors.add(new ImportError(S_PROJECT, p.row(), "전환 예정일 형식 오류(yyyy-MM-dd)"));
        }
    }

    private Set<String> validateAssets(List<AssetRow> assets, List<ImportError> errors) {
        Set<String> names = new HashSet<>();
        for (AssetRow a : assets) {
            if (ImportValues.isBlank(a.name())) {
                errors.add(new ImportError(S_ASSET, a.row(), "자산명은 필수입니다"));
            } else if (!names.add(a.name().trim())) {
                errors.add(new ImportError(S_ASSET, a.row(), "자산명이 중복됩니다: " + a.name().trim()));
            }
            if (!enumOk(a.assetType(), ASSET_TYPE)) {
                errors.add(new ImportError(S_ASSET, a.row(), "자산유형은 SERVER/EXTERNAL 중 하나여야 합니다"));
            }
            if (!ImportValues.isBlank(a.role()) && !ROLE.contains(a.role().trim())) {
                errors.add(new ImportError(S_ASSET, a.row(), "역할은 WEB/WAS/DB/ETC 중 하나여야 합니다"));
            }
        }
        return names;
    }

    private void validateIps(List<IpRow> ips, Set<String> assets, List<ImportError> errors) {
        for (IpRow ip : ips) {
            refAsset(S_IP, ip.row(), ip.assetName(), assets, errors);
            if (!ImportValues.isIp(ip.address())) {
                errors.add(new ImportError(S_IP, ip.row(), "IP 형식 오류: " + ImportValues.trim(ip.address())));
            }
            if (!enumOk(ip.ipType(), IP_TYPE)) {
                errors.add(new ImportError(S_IP, ip.row(), "IP종류는 PUBLIC/PRIVATE 중 하나여야 합니다"));
            }
            if (!enumOk(ip.phase(), PHASE)) {
                errors.add(new ImportError(S_IP, ip.row(), "단계는 BEFORE/AFTER 중 하나여야 합니다"));
            }
            if (!ImportValues.isBlank(ip.extWhitelisted()) && !ImportValues.isBool(ip.extWhitelisted())) {
                errors.add(new ImportError(S_IP, ip.row(), "허용목록 값은 Y/N(1/0)이어야 합니다"));
            }
        }
    }

    private void validateDependencies(List<DependencyRow> deps, Set<String> assets,
                                      List<ImportError> errors) {
        for (DependencyRow d : deps) {
            refAsset(S_DEP, d.row(), d.fromAsset(), assets, errors);
            if (!ImportValues.isBlank(d.toAsset()) && !assets.contains(d.toAsset().trim())) {
                errors.add(new ImportError(S_DEP, d.row(), "대상 자산이 자산 시트에 없습니다: " + d.toAsset().trim()));
            }
            if (ImportValues.isBlank(d.targetAddress())) {
                errors.add(new ImportError(S_DEP, d.row(), "접속 주소는 필수입니다"));
            }
            Integer port = ImportValues.toIntOrNull(d.targetPort());
            if (port == null || port < 1 || port > 65535) {
                errors.add(new ImportError(S_DEP, d.row(), "포트는 1~65535 사이 숫자여야 합니다"));
            }
            if (ImportValues.isBlank(d.protocol())) {
                errors.add(new ImportError(S_DEP, d.row(), "프로토콜은 필수입니다"));
            }
        }
    }

    private void validateDns(List<DnsRow> dns, List<ImportError> errors) {
        for (DnsRow r : dns) {
            if (ImportValues.isBlank(r.domain())) {
                errors.add(new ImportError(S_DNS, r.row(), "도메인은 필수입니다"));
            }
            if (!enumOk(r.recordType(), DNS_TYPE)) {
                errors.add(new ImportError(S_DNS, r.row(), "레코드종류는 A/AAAA/CNAME/MX/TXT 중 하나여야 합니다"));
            }
            if (ImportValues.isBlank(r.value())) {
                errors.add(new ImportError(S_DNS, r.row(), "레코드 값은 필수입니다"));
            }
            Integer ttl = ImportValues.toIntOrNull(r.ttl());
            if (ttl == null || ttl < 0) {
                errors.add(new ImportError(S_DNS, r.row(), "TTL은 0 이상 숫자여야 합니다"));
            }
            if (!enumOk(r.phase(), PHASE)) {
                errors.add(new ImportError(S_DNS, r.row(), "단계는 BEFORE/AFTER 중 하나여야 합니다"));
            }
        }
    }

    private void validateCerts(List<CertRow> certs, List<ImportError> errors) {
        for (CertRow c : certs) {
            if (ImportValues.isBlank(c.domain())) {
                errors.add(new ImportError(S_CERT, c.row(), "도메인은 필수입니다"));
            }
            if (ImportValues.toDateOrNull(c.notAfter()) == null) {
                errors.add(new ImportError(S_CERT, c.row(), "만료일 형식 오류(yyyy-MM-dd)"));
            }
            if (!enumOk(c.phase(), PHASE)) {
                errors.add(new ImportError(S_CERT, c.row(), "단계는 BEFORE/AFTER 중 하나여야 합니다"));
            }
        }
    }

    private void validateSoftware(List<SoftwareRow> sw, Set<String> assets, List<ImportError> errors) {
        for (SoftwareRow s : sw) {
            refAsset(S_SW, s.row(), s.assetName(), assets, errors);
            if (ImportValues.isBlank(s.product())) {
                errors.add(new ImportError(S_SW, s.row(), "제품은 필수입니다"));
            }
            if (ImportValues.isBlank(s.version())) {
                errors.add(new ImportError(S_SW, s.row(), "버전은 필수입니다"));
            }
            if (ImportValues.isBlank(s.releaseLine())) {
                errors.add(new ImportError(S_SW, s.row(), "릴리스라인은 필수입니다"));
            }
            if (!enumOk(s.phase(), PHASE)) {
                errors.add(new ImportError(S_SW, s.row(), "단계는 BEFORE/AFTER 중 하나여야 합니다"));
            }
        }
    }

    private void validateBackups(List<BackupRow> backups, Set<String> assets, List<ImportError> errors) {
        for (BackupRow b : backups) {
            refAsset(S_BACKUP, b.row(), b.assetName(), assets, errors);
            if (!ImportValues.isBlank(b.lastBackupAt()) && ImportValues.toDateOrNull(b.lastBackupAt()) == null) {
                errors.add(new ImportError(S_BACKUP, b.row(), "마지막 백업일 형식 오류(yyyy-MM-dd)"));
            }
            if (!ImportValues.isBlank(b.restoreTested()) && !ImportValues.isBool(b.restoreTested())) {
                errors.add(new ImportError(S_BACKUP, b.row(), "복구테스트 값은 Y/N(1/0)이어야 합니다"));
            }
            if (!ImportValues.isBlank(b.offsite()) && !ImportValues.isBool(b.offsite())) {
                errors.add(new ImportError(S_BACKUP, b.row(), "오프사이트 값은 Y/N(1/0)이어야 합니다"));
            }
            if (!enumOk(b.phase(), PHASE)) {
                errors.add(new ImportError(S_BACKUP, b.row(), "단계는 BEFORE/AFTER 중 하나여야 합니다"));
            }
        }
    }

    private void refAsset(String sheet, int row, String name, Set<String> assets,
                          List<ImportError> errors) {
        if (ImportValues.isBlank(name)) {
            errors.add(new ImportError(sheet, row, "자산명은 필수입니다"));
        } else if (!assets.contains(name.trim())) {
            errors.add(new ImportError(sheet, row, "자산 시트에 없는 자산명입니다: " + name.trim()));
        }
    }

    private boolean enumOk(String value, Set<String> allowed) {
        return !ImportValues.isBlank(value) && allowed.contains(value.trim());
    }
}
