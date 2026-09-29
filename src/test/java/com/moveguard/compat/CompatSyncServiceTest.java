package com.moveguard.compat;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 네트워크 없이(라이브 조회 실패 → 스냅샷 fallback) 동기화가 동작함을 검증한다. */
class CompatSyncServiceTest {

    /** fetchCycles가 항상 실패하는 클라이언트 — 스냅샷 fallback 경로를 강제한다 */
    private final EndOfLifeClient failingClient = new EndOfLifeClient("http://localhost:0") {
        @Override
        public List<tools.jackson.databind.JsonNode> fetchCycles(String product) {
            throw new RuntimeException("network down");
        }
    };

    private final CompatSnapshotStore snapshotStore = new CompatSnapshotStore(new ObjectMapper());

    @Test
    @DisplayName("라이브 실패 시 스냅샷으로 채우고, MANUAL 제품은 건너뛰며, 수동 버전은 덮어쓰지 않는다")
    void fallsBackToSnapshotOffline() {
        FakeMapper mapper = new FakeMapper();
        mapper.products.add(new CompatProduct("java", "Java", "lang", null, null, null, "MANUAL"));
        mapper.products.add(new CompatProduct("tomcat", "Apache Tomcat", "server-app", null, null, null, "PENDING"));
        mapper.manualVersions.put("tomcat", List.of("9.0")); // 9.0은 수동 관리 → 건너뛰어야 함

        CompatSyncService service = new CompatSyncService(mapper, failingClient, snapshotStore);
        List<CompatSyncService.SyncResult> results = service.sync();

        CompatSyncService.SyncResult java = find(results, "java");
        CompatSyncService.SyncResult tomcat = find(results, "tomcat");

        assertThat(java.source()).isEqualTo(CompatSyncService.Source.SKIPPED);
        assertThat(java.upserted()).isZero();

        assertThat(tomcat.source()).isEqualTo(CompatSyncService.Source.SNAPSHOT);
        assertThat(tomcat.upserted()).isPositive();
        // 수동 관리 버전(9.0)은 upsert 대상에서 제외
        assertThat(mapper.upserted).noneMatch(r -> "9.0".equals(r.getVersion()));
        assertThat(mapper.upserted).anyMatch(r -> "11.0".equals(r.getVersion()));
        // 상태는 OK로 기록
        assertThat(mapper.statuses).containsEntry("tomcat", "OK");
    }

    private CompatSyncService.SyncResult find(List<CompatSyncService.SyncResult> rs, String product) {
        return rs.stream().filter(r -> r.product().equals(product)).findFirst().orElseThrow();
    }

    /** 인메모리 CompatMapper 스텁 */
    private static class FakeMapper implements CompatMapper {
        final List<CompatProduct> products = new ArrayList<>();
        final java.util.Map<String, List<String>> manualVersions = new java.util.HashMap<>();
        final List<CompatRelease> upserted = new ArrayList<>();
        final java.util.Map<String, String> statuses = new java.util.HashMap<>();

        @Override
        public List<CompatProduct> findProducts() {
            return products;
        }

        @Override
        public List<String> findManualVersions(String product) {
            return manualVersions.getOrDefault(product, List.of());
        }

        @Override
        public void upsertRelease(CompatRelease release) {
            upserted.add(release);
        }

        @Override
        public void updateProductStatus(String product, String status, LocalDateTime fetchedAt) {
            statuses.put(product, status);
        }
    }
}
