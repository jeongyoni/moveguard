package com.moveguard.imports;

import com.moveguard.imports.ProjectImport.AssetRow;
import com.moveguard.imports.ProjectImport.BackupRow;
import com.moveguard.imports.ProjectImport.CertRow;
import com.moveguard.imports.ProjectImport.DependencyRow;
import com.moveguard.imports.ProjectImport.DnsRow;
import com.moveguard.imports.ProjectImport.IpRow;
import com.moveguard.imports.ProjectImport.ProjectRow;
import com.moveguard.imports.ProjectImport.SoftwareRow;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이전사업 입력을 검증하고 저장하는 단일 진입점. 엑셀 업로드(1단계)와 (2단계) 웹 폼이 공유한다.
 * 저장은 한 트랜잭션 — 중간에 실패하면 전부 롤백된다(전건 저장 원칙).
 */
@Service
@RequiredArgsConstructor
public class ProjectImportService {

    private final ProjectImportValidator validator;
    private final ProjectImportMapper mapper;

    public List<ImportError> validate(ProjectImport in) {
        return validator.validate(in);
    }

    /** 검증 후 저장. 오류가 있으면 저장하지 않고 예외로 알린다. 성공 시 새 projectId. */
    @Transactional
    public long importProject(ProjectImport in) {
        List<ImportError> errors = validator.validate(in);
        if (!errors.isEmpty()) {
            throw new ImportValidationException(errors);
        }
        return save(in);
    }

    private long save(ProjectImport in) {
        ProjectRow p = in.project();
        Map<String, Object> project = map(
                "name", p.name().trim(),
                "customerName", p.customerName().trim(),
                "sourceEnv", blankToNull(p.sourceEnv()),
                "targetEnv", blankToNull(p.targetEnv()),
                "status", ImportValues.isBlank(p.status()) ? "PLAN" : p.status().trim(),
                "plannedDate", ImportValues.toDateOrNull(p.plannedDate()));
        mapper.insertProject(project);
        long projectId = ((Number) project.get("projectId")).longValue();

        Map<String, Long> assetIds = new HashMap<>();
        for (AssetRow a : in.assets()) {
            Map<String, Object> row = map(
                    "projectId", projectId,
                    "name", a.name().trim(),
                    "assetType", a.assetType().trim(),
                    "role", blankToNull(a.role()),
                    "osName", blankToNull(a.osName()),
                    "osVersion", blankToNull(a.osVersion()));
            mapper.insertAsset(row);
            assetIds.put(a.name().trim(), ((Number) row.get("assetId")).longValue());
        }

        for (IpRow ip : in.ips()) {
            mapper.insertIp(map(
                    "assetId", assetIds.get(ip.assetName().trim()),
                    "address", ip.address().trim(),
                    "ipType", ip.ipType().trim(),
                    "phase", ip.phase().trim(),
                    "extWhitelisted", ImportValues.toBool(ip.extWhitelisted()),
                    "note", blankToNull(ip.note())));
        }

        for (DependencyRow d : in.dependencies()) {
            mapper.insertDependency(map(
                    "projectId", projectId,
                    "fromAssetId", assetIds.get(d.fromAsset().trim()),
                    "toAssetId", ImportValues.isBlank(d.toAsset()) ? null : assetIds.get(d.toAsset().trim()),
                    "targetAddress", d.targetAddress().trim(),
                    "targetPort", ImportValues.toIntOrNull(d.targetPort()),
                    "protocol", d.protocol().trim(),
                    "configLocation", blankToNull(d.configLocation())));
        }

        for (DnsRow r : in.dnsRecords()) {
            mapper.insertDns(map(
                    "projectId", projectId,
                    "domain", r.domain().trim(),
                    "recordType", r.recordType().trim(),
                    "value", r.value().trim(),
                    "ttl", ImportValues.toIntOrNull(r.ttl()),
                    "phase", r.phase().trim()));
        }

        for (CertRow c : in.certificates()) {
            mapper.insertCertificate(map(
                    "projectId", projectId,
                    "domain", c.domain().trim(),
                    "issuer", blankToNull(c.issuer()),
                    "notAfter", ImportValues.toDateOrNull(c.notAfter()),
                    "phase", c.phase().trim()));
        }

        for (SoftwareRow s : in.software()) {
            mapper.insertSoftware(map(
                    "assetId", assetIds.get(s.assetName().trim()),
                    "product", s.product().trim(),
                    "role", blankToNull(s.role()),
                    "version", s.version().trim(),
                    "releaseLine", s.releaseLine().trim(),
                    "phase", s.phase().trim()));
        }

        for (BackupRow b : in.backups()) {
            mapper.insertBackup(map(
                    "assetId", assetIds.get(b.assetName().trim()),
                    "lastBackupAt", ImportValues.toDateOrNull(b.lastBackupAt()),
                    "restoreTested", ImportValues.toBool(b.restoreTested()),
                    "offsite", ImportValues.toBool(b.offsite()),
                    "phase", b.phase().trim()));
        }

        return projectId;
    }

    private static String blankToNull(String s) {
        return ImportValues.isBlank(s) ? null : s.trim();
    }

    /** null 값을 허용하는 HashMap 빌더 (Map.of는 null 불가) */
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}
