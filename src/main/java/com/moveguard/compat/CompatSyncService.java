package com.moveguard.compat;

import tools.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 호환성 기준 데이터를 endoflife.date와 동기화한다.
 * 제품별로 라이브 조회 → 실패 시 스냅샷 → 둘 다 없으면 FAILED. 수동 관리 릴리스는 덮어쓰지 않는다.
 * 규칙은 이 결과가 쌓인 compat_release 테이블만 참조하므로, 동기화 없이도(스냅샷만으로도) 진단이 동작한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompatSyncService {

    private final CompatMapper compatMapper;
    private final EndOfLifeClient client;
    private final CompatSnapshotStore snapshotStore;

    public enum Source { LIVE, SNAPSHOT, SKIPPED, FAILED }

    public record SyncResult(String product, Source source, int upserted) {
    }

    public List<SyncResult> sync() {
        List<SyncResult> results = new ArrayList<>();
        for (CompatProduct product : compatMapper.findProducts()) {
            results.add(syncProduct(product));
        }
        return results;
    }

    private SyncResult syncProduct(CompatProduct product) {
        String key = product.getProduct();
        if ("MANUAL".equals(product.getFetchStatus())) {
            return new SyncResult(key, Source.SKIPPED, 0);
        }

        List<JsonNode> cycles;
        Source source;
        try {
            cycles = client.fetchCycles(key);
            source = Source.LIVE;
            if (cycles.isEmpty()) {
                cycles = snapshotStore.load(key);
                source = Source.SNAPSHOT;
            }
        } catch (Exception e) {
            log.warn("endoflife.date 조회 실패, 스냅샷으로 대체: product={}, cause={}", key, e.toString());
            cycles = snapshotStore.load(key);
            source = Source.SNAPSHOT;
        }

        if (cycles.isEmpty()) {
            compatMapper.updateProductStatus(key, "FAILED", LocalDateTime.now());
            return new SyncResult(key, Source.FAILED, 0);
        }

        LocalDate today = LocalDate.now();
        Set<String> manualVersions = new HashSet<>(compatMapper.findManualVersions(key));
        int upserted = 0;
        for (JsonNode cycle : cycles) {
            CompatRelease release = CompatReleaseFactory.fromCycle(key, cycle, today);
            if (release.getVersion() == null || release.getVersion().isBlank()
                    || manualVersions.contains(release.getVersion())) {
                continue;
            }
            compatMapper.upsertRelease(release);
            upserted++;
        }

        compatMapper.updateProductStatus(key, "OK", LocalDateTime.now());
        return new SyncResult(key, source, upserted);
    }
}
