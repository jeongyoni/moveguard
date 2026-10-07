package com.moveguard.project;

import com.moveguard.imports.ImportValues;
import com.moveguard.imports.ProjectImportMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 상세 화면의 DNS 레코드 추가/수정/삭제. */
@Service
@RequiredArgsConstructor
public class DnsEditService {

    private static final Set<String> TYPE = Set.of("A", "AAAA", "CNAME", "MX", "TXT");
    private static final Set<String> PHASE = Set.of("BEFORE", "AFTER");

    private final ProjectEditMapper editMapper;
    private final ProjectImportMapper importMapper;

    @Transactional
    public List<String> add(long projectId, String domain, String recordType, String value,
                            String ttl, String phase) {
        List<String> errors = validate(domain, recordType, value, ttl, phase);
        if (!errors.isEmpty()) {
            return errors;
        }
        Map<String, Object> row = new HashMap<>();
        row.put("projectId", projectId);
        row.put("domain", domain.trim());
        row.put("recordType", recordType.trim());
        row.put("value", value.trim());
        row.put("ttl", ImportValues.toIntOrNull(ttl));
        row.put("phase", phase.trim());
        importMapper.insertDns(row);
        return errors;
    }

    @Transactional
    public List<String> update(long dnsId, String domain, String recordType, String value,
                               String ttl, String phase) {
        if (editMapper.findDns(dnsId) == null) {
            return List.of("DNS 레코드를 찾을 수 없습니다");
        }
        List<String> errors = validate(domain, recordType, value, ttl, phase);
        if (!errors.isEmpty()) {
            return errors;
        }
        Map<String, Object> row = new HashMap<>();
        row.put("dnsId", dnsId);
        row.put("domain", domain.trim());
        row.put("recordType", recordType.trim());
        row.put("value", value.trim());
        row.put("ttl", ImportValues.toIntOrNull(ttl));
        row.put("phase", phase.trim());
        editMapper.updateDns(row);
        return errors;
    }

    @Transactional
    public void delete(long dnsId) {
        editMapper.deleteDns(dnsId);
    }

    private List<String> validate(String domain, String recordType, String value, String ttl,
                                  String phase) {
        List<String> errors = new ArrayList<>();
        if (ImportValues.isBlank(domain)) {
            errors.add("도메인은 필수입니다");
        }
        if (ImportValues.isBlank(recordType) || !TYPE.contains(recordType.trim())) {
            errors.add("레코드종류는 A/AAAA/CNAME/MX/TXT 중 하나여야 합니다");
        }
        if (ImportValues.isBlank(value)) {
            errors.add("레코드 값은 필수입니다");
        }
        Integer t = ImportValues.toIntOrNull(ttl);
        if (t == null || t < 0) {
            errors.add("TTL은 0 이상 숫자여야 합니다");
        }
        if (ImportValues.isBlank(phase) || !PHASE.contains(phase.trim())) {
            errors.add("단계는 BEFORE 또는 AFTER여야 합니다");
        }
        return errors;
    }
}
