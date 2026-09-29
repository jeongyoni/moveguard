package com.moveguard.compat;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CompatSnapshotStoreTest {

    private final CompatSnapshotStore store = new CompatSnapshotStore(new ObjectMapper());

    @Test
    @DisplayName("번들된 스냅샷을 네트워크 없이 읽는다")
    void loadsBundledSnapshot() {
        List<JsonNode> cycles = store.load("tomcat");

        assertThat(cycles).isNotEmpty();
        assertThat(cycles).anyMatch(c -> "11.0".equals(c.path("cycle").asText()));
    }

    @Test
    @DisplayName("스냅샷이 없는 제품이면 빈 목록")
    void missingSnapshotReturnsEmpty() {
        assertThat(store.load("no-such-product")).isEmpty();
    }
}
