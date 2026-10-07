package com.moveguard.project;

import com.moveguard.imports.ImportValues;
import com.moveguard.imports.ProjectImportMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 상세 화면의 설치 소프트웨어 추가/수정/삭제. */
@Service
@RequiredArgsConstructor
public class SoftwareEditService {

    private static final Set<String> PHASE = Set.of("BEFORE", "AFTER");

    private final ProjectEditMapper editMapper;
    private final ProjectImportMapper importMapper;

    @Transactional
    public List<String> add(long projectId, Long assetId, String product, String role,
                            String version, String releaseLine, String phase) {
        List<String> errors = validate(projectId, assetId, product, version, releaseLine, phase, null);
        if (!errors.isEmpty()) {
            return errors;
        }
        importMapper.insertSoftware(row(null, assetId, product, role, version, releaseLine, phase));
        return errors;
    }

    @Transactional
    public List<String> update(long softwareId, Long assetId, String product, String role,
                               String version, String releaseLine, String phase) {
        SoftwareView current = editMapper.findSoftwareOne(softwareId);
        if (current == null) {
            return List.of("소프트웨어를 찾을 수 없습니다");
        }
        AssetDetail asset = editMapper.findAsset(current.getAssetId());
        List<String> errors = validate(asset.getProjectId(), assetId, product, version,
                releaseLine, phase, softwareId);
        if (!errors.isEmpty()) {
            return errors;
        }
        editMapper.updateSoftware(row(softwareId, assetId, product, role, version, releaseLine, phase));
        return errors;
    }

    @Transactional
    public void delete(long softwareId) {
        editMapper.deleteSoftware(softwareId);
    }

    private List<String> validate(Long projectId, Long assetId, String product, String version,
                                  String releaseLine, String phase, Long excludeId) {
        List<String> errors = new ArrayList<>();
        Set<Long> projectAssets = editMapper.findAssets(projectId).stream()
                .map(AssetDetail::getAssetId).collect(Collectors.toSet());
        if (assetId == null || !projectAssets.contains(assetId)) {
            errors.add("자산을 선택하세요");
        }
        if (ImportValues.isBlank(product)) {
            errors.add("제품은 필수입니다");
        }
        if (ImportValues.isBlank(version)) {
            errors.add("버전은 필수입니다");
        }
        if (ImportValues.isBlank(releaseLine)) {
            errors.add("릴리스라인은 필수입니다");
        }
        if (ImportValues.isBlank(phase) || !PHASE.contains(phase.trim())) {
            errors.add("단계는 BEFORE 또는 AFTER여야 합니다");
        }
        if (assetId != null && !ImportValues.isBlank(product) && !ImportValues.isBlank(phase)
                && PHASE.contains(phase.trim())) {
            Long dup = editMapper.findSoftwareId(assetId, product.trim(), phase.trim());
            if (dup != null && !Objects.equals(dup, excludeId)) {
                errors.add("이미 등록된 소프트웨어입니다 (자산·제품·단계 중복)");
            }
        }
        return errors;
    }

    private Map<String, Object> row(Long softwareId, Long assetId, String product, String role,
                                    String version, String releaseLine, String phase) {
        Map<String, Object> m = new HashMap<>();
        m.put("softwareId", softwareId);
        m.put("assetId", assetId);
        m.put("product", product.trim());
        m.put("role", ImportValues.isBlank(role) ? null : role.trim());
        m.put("version", version.trim());
        m.put("releaseLine", releaseLine.trim());
        m.put("phase", phase.trim());
        return m;
    }
}
