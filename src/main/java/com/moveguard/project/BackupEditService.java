package com.moveguard.project;

import com.moveguard.imports.ImportValues;
import com.moveguard.imports.ProjectImportMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 상세 화면의 백업 계획 추가/수정/삭제. */
@Service
@RequiredArgsConstructor
public class BackupEditService {

    private static final Set<String> PHASE = Set.of("BEFORE", "AFTER");

    private final ProjectEditMapper editMapper;
    private final ProjectImportMapper importMapper;

    @Transactional
    public List<String> add(long projectId, Long assetId, String lastBackupAt,
                            String restoreTested, String offsite, String phase) {
        List<String> errors = validate(projectId, assetId, lastBackupAt, phase);
        if (!errors.isEmpty()) {
            return errors;
        }
        importMapper.insertBackup(row(null, assetId, lastBackupAt, restoreTested, offsite, phase));
        return errors;
    }

    @Transactional
    public List<String> update(long backupId, Long assetId, String lastBackupAt,
                               String restoreTested, String offsite, String phase) {
        BackupView current = editMapper.findBackup(backupId);
        if (current == null) {
            return List.of("백업을 찾을 수 없습니다");
        }
        AssetDetail asset = editMapper.findAsset(current.getAssetId());
        List<String> errors = validate(asset.getProjectId(), assetId, lastBackupAt, phase);
        if (!errors.isEmpty()) {
            return errors;
        }
        editMapper.updateBackup(row(backupId, assetId, lastBackupAt, restoreTested, offsite, phase));
        return errors;
    }

    @Transactional
    public void delete(long backupId) {
        editMapper.deleteBackup(backupId);
    }

    private List<String> validate(Long projectId, Long assetId, String lastBackupAt, String phase) {
        List<String> errors = new ArrayList<>();
        Set<Long> projectAssets = editMapper.findAssets(projectId).stream()
                .map(AssetDetail::getAssetId).collect(Collectors.toSet());
        if (assetId == null || !projectAssets.contains(assetId)) {
            errors.add("자산을 선택하세요");
        }
        if (!ImportValues.isBlank(lastBackupAt) && ImportValues.toDateOrNull(lastBackupAt) == null) {
            errors.add("마지막 백업일 형식 오류(yyyy-MM-dd)");
        }
        if (ImportValues.isBlank(phase) || !PHASE.contains(phase.trim())) {
            errors.add("단계는 BEFORE 또는 AFTER여야 합니다");
        }
        return errors;
    }

    private Map<String, Object> row(Long backupId, Long assetId, String lastBackupAt,
                                    String restoreTested, String offsite, String phase) {
        Map<String, Object> m = new HashMap<>();
        m.put("backupId", backupId);
        m.put("assetId", assetId);
        m.put("lastBackupAt", ImportValues.toDateOrNull(lastBackupAt));
        m.put("restoreTested", ImportValues.toBool(restoreTested));
        m.put("offsite", ImportValues.toBool(offsite));
        m.put("phase", phase.trim());
        return m;
    }
}
