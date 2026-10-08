package com.moveguard.project;

import com.moveguard.imports.ImportValues;
import com.moveguard.imports.ProjectImportMapper;
import com.moveguard.warscan.IpAddresses;
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

/** 상세 화면의 IP 추가/수정/삭제. 실패 시 오류 메시지 목록을 돌려주고 저장하지 않는다. */
@Service
@RequiredArgsConstructor
public class IpEditService {

    private static final Set<String> PHASE = Set.of("BEFORE", "AFTER");

    private final ProjectEditMapper editMapper;
    private final ProjectImportMapper importMapper;

    @Transactional
    public List<String> add(long projectId, Long assetId, String address,
                            String phase, String extWhitelisted, String note) {
        List<String> errors = validate(projectId, assetId, address, phase, null);
        if (!errors.isEmpty()) {
            return errors;
        }
        Map<String, Object> row = new HashMap<>();
        row.put("assetId", assetId);
        row.put("address", address.trim());
        row.put("ipType", IpAddresses.classify(address));   // 주소로 공인/사설 자동 판별
        row.put("phase", phase.trim());
        row.put("extWhitelisted", ImportValues.toBool(extWhitelisted));
        row.put("note", blankToNull(note));
        importMapper.insertIp(row);
        return errors;
    }

    @Transactional
    public List<String> update(long ipId, Long assetId, String address,
                               String phase, String extWhitelisted, String note) {
        IpView current = editMapper.findIp(ipId);
        if (current == null) {
            return List.of("IP를 찾을 수 없습니다");
        }
        List<String> errors = validate(projectIdOf(current), assetId, address, phase, ipId);
        if (!errors.isEmpty()) {
            return errors;
        }
        Map<String, Object> row = new HashMap<>();
        row.put("ipId", ipId);
        row.put("assetId", assetId);
        row.put("address", address.trim());
        row.put("ipType", IpAddresses.classify(address));   // 주소로 공인/사설 자동 판별
        row.put("phase", phase.trim());
        row.put("extWhitelisted", ImportValues.toBool(extWhitelisted));
        row.put("note", blankToNull(note));
        editMapper.updateIp(row);
        return errors;
    }

    @Transactional
    public void delete(long ipId) {
        editMapper.deleteIp(ipId);
    }

    private long projectIdOf(IpView ip) {
        AssetDetail asset = editMapper.findAsset(ip.getAssetId());
        return asset.getProjectId();
    }

    /** excludeIpId: 수정 시 자기 자신은 중복으로 보지 않도록 제외 */
    private List<String> validate(Long projectId, Long assetId, String address,
                                  String phase, Long excludeIpId) {
        List<String> errors = new ArrayList<>();

        Set<Long> projectAssets = projectId == null ? Set.of()
                : editMapper.findAssets(projectId).stream()
                        .map(AssetDetail::getAssetId).collect(Collectors.toSet());
        if (assetId == null || !projectAssets.contains(assetId)) {
            errors.add("자산을 선택하세요");
        }
        if (!ImportValues.isIp(address)) {
            errors.add("IP 형식 오류: " + ImportValues.trim(address));
        }
        if (ImportValues.isBlank(phase) || !PHASE.contains(phase.trim())) {
            errors.add("단계는 BEFORE 또는 AFTER여야 합니다");
        }
        // (자산·주소·단계) 중복
        if (assetId != null && ImportValues.isIp(address) && !ImportValues.isBlank(phase)
                && PHASE.contains(phase.trim())) {
            Long dup = editMapper.findIpId(assetId, address.trim(), phase.trim());
            if (dup != null && !Objects.equals(dup, excludeIpId)) {
                errors.add("이미 등록된 IP입니다 (자산·주소·단계 중복)");
            }
        }
        return errors;
    }

    private static String blankToNull(String s) {
        return ImportValues.isBlank(s) ? null : s.trim();
    }
}
