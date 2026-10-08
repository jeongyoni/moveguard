package com.moveguard.warscan;

import com.moveguard.imports.ProjectImportMapper;
import com.moveguard.project.AssetDetail;
import com.moveguard.project.ProjectEditMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** WAR 분석에서 확인된 하드코딩 IP를 자산의 의존성으로 저장한다 (IP-03 진단에 반영됨). */
@Service
@RequiredArgsConstructor
public class WarScanService {

    private final ProjectEditMapper editMapper;
    private final ProjectImportMapper importMapper;

    public List<AssetDetail> assets(long projectId) {
        return editMapper.findAssets(projectId);
    }

    /**
     * 확인 폼에서 선택된 토큰들을 의존성으로 추가한다.
     * @return 추가된 건수
     */
    @Transactional
    public int addDependencies(long projectId, Long fromAssetId, List<String> tokens) {
        if (fromAssetId == null || tokens == null || tokens.isEmpty()) {
            return 0;
        }
        Set<Long> projectAssets = editMapper.findAssets(projectId).stream()
                .map(AssetDetail::getAssetId).collect(Collectors.toSet());
        if (!projectAssets.contains(fromAssetId)) {
            throw new IllegalArgumentException("선택한 자산이 사업에 속하지 않습니다.");
        }

        int added = 0;
        for (String token : tokens) {
            String[] f = token.split("\t", 4);
            if (f.length < 4) {
                continue;
            }
            String ip = f[0].trim();
            if (!IpAddresses.isIpv4Literal(ip)) {
                continue;
            }
            Integer port = parsePort(f[1]);
            boolean jdbc = Boolean.parseBoolean(f[2]);
            String entry = f[3].trim();

            Map<String, Object> row = new HashMap<>();
            row.put("projectId", projectId);
            row.put("fromAssetId", fromAssetId);
            row.put("toAssetId", null);
            row.put("targetAddress", ip);
            row.put("targetPort", port != null ? port : 0);
            row.put("protocol", jdbc ? "JDBC" : "TCP");
            row.put("configLocation", entry.length() > 255 ? entry.substring(0, 255) : entry);
            importMapper.insertDependency(row);
            added++;
        }
        return added;
    }

    private Integer parsePort(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            int p = Integer.parseInt(s.trim());
            return (p >= 1 && p <= 65535) ? p : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
