package com.moveguard.project;

import com.moveguard.imports.ProjectImportMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 상세 화면의 자산 추가/수정/삭제. 검증 실패 시 메시지 목록을 돌려주고 저장하지 않는다. */
@Service
@RequiredArgsConstructor
public class AssetEditService {

    private static final Set<String> TYPE = Set.of("SERVER", "EXTERNAL");
    private static final Set<String> ROLE = Set.of("WEB", "WAS", "DB", "ETC");

    private final ProjectEditMapper editMapper;
    private final ProjectImportMapper importMapper;

    /** 성공 시 빈 목록, 실패 시 오류 메시지 목록(저장 안 함). */
    @Transactional
    public List<String> add(long projectId, String name, String assetType, String role,
                            String osName, String osVersion) {
        Set<String> taken = new HashSet<>(editMapper.findAssetNames(projectId));
        List<String> errors = validate(name, assetType, role, taken);
        if (!errors.isEmpty()) {
            return errors;
        }
        Map<String, Object> row = new HashMap<>();
        row.put("projectId", projectId);
        row.put("name", name.trim());
        row.put("assetType", assetType.trim());
        row.put("role", blankToNull(role));
        row.put("osName", blankToNull(osName));
        row.put("osVersion", blankToNull(osVersion));
        importMapper.insertAsset(row);
        return errors;
    }

    @Transactional
    public List<String> update(long assetId, String name, String assetType, String role,
                               String osName, String osVersion) {
        AssetDetail current = editMapper.findAsset(assetId);
        if (current == null) {
            return List.of("자산을 찾을 수 없습니다");
        }
        Set<String> taken = new HashSet<>(editMapper.findAssetNames(current.getProjectId()));
        taken.remove(current.getName()); // 자기 이름은 중복 아님
        List<String> errors = validate(name, assetType, role, taken);
        if (!errors.isEmpty()) {
            return errors;
        }
        Map<String, Object> row = new HashMap<>();
        row.put("assetId", assetId);
        row.put("name", name.trim());
        row.put("assetType", assetType.trim());
        row.put("role", blankToNull(role));
        row.put("osName", blankToNull(osName));
        row.put("osVersion", blankToNull(osVersion));
        editMapper.updateAsset(row);
        return errors;
    }

    @Transactional
    public void delete(long assetId) {
        editMapper.deleteAsset(assetId);
    }

    private List<String> validate(String name, String type, String role, Set<String> taken) {
        List<String> errors = new ArrayList<>();
        if (isBlank(name)) {
            errors.add("자산명은 필수입니다");
        } else if (taken.contains(name.trim())) {
            errors.add("이미 있는 자산명입니다: " + name.trim());
        }
        if (isBlank(type) || !TYPE.contains(type.trim())) {
            errors.add("자산유형은 SERVER 또는 EXTERNAL이어야 합니다");
        }
        if (!isBlank(role) && !ROLE.contains(role.trim())) {
            errors.add("역할은 WEB/WAS/DB/ETC 중 하나여야 합니다");
        }
        return errors;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String blankToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }
}
