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

/** 상세 화면의 인증서 추가/수정/삭제. */
@Service
@RequiredArgsConstructor
public class CertEditService {

    private static final Set<String> PHASE = Set.of("BEFORE", "AFTER");

    private final ProjectEditMapper editMapper;
    private final ProjectImportMapper importMapper;

    @Transactional
    public List<String> add(long projectId, String domain, String issuer, String notAfter,
                            String phase) {
        List<String> errors = validate(domain, notAfter, phase);
        if (!errors.isEmpty()) {
            return errors;
        }
        Map<String, Object> row = new HashMap<>();
        row.put("projectId", projectId);
        row.put("domain", domain.trim());
        row.put("issuer", blankToNull(issuer));
        row.put("notAfter", ImportValues.toDateOrNull(notAfter));
        row.put("phase", phase.trim());
        importMapper.insertCertificate(row);
        return errors;
    }

    @Transactional
    public List<String> update(long certId, String domain, String issuer, String notAfter,
                               String phase) {
        if (editMapper.findCert(certId) == null) {
            return List.of("인증서를 찾을 수 없습니다");
        }
        List<String> errors = validate(domain, notAfter, phase);
        if (!errors.isEmpty()) {
            return errors;
        }
        Map<String, Object> row = new HashMap<>();
        row.put("certId", certId);
        row.put("domain", domain.trim());
        row.put("issuer", blankToNull(issuer));
        row.put("notAfter", ImportValues.toDateOrNull(notAfter));
        row.put("phase", phase.trim());
        editMapper.updateCert(row);
        return errors;
    }

    @Transactional
    public void delete(long certId) {
        editMapper.deleteCert(certId);
    }

    private List<String> validate(String domain, String notAfter, String phase) {
        List<String> errors = new ArrayList<>();
        if (ImportValues.isBlank(domain)) {
            errors.add("도메인은 필수입니다");
        }
        if (ImportValues.toDateOrNull(notAfter) == null) {
            errors.add("만료일 형식 오류(yyyy-MM-dd)");
        }
        if (ImportValues.isBlank(phase) || !PHASE.contains(phase.trim())) {
            errors.add("단계는 BEFORE 또는 AFTER여야 합니다");
        }
        return errors;
    }

    private static String blankToNull(String s) {
        return ImportValues.isBlank(s) ? null : s.trim();
    }
}
