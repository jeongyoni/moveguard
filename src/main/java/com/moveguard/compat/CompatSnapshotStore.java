package com.moveguard.compat;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 번들된 스냅샷(classpath:compat-snapshot/{product}.json)을 읽는다.
 * endoflife.date 접속이 실패해도 네트워크 없이 최신에 준하는 기준 데이터를 확보하기 위한 fallback.
 */
@Component
public class CompatSnapshotStore {

    private final ObjectMapper objectMapper;

    public CompatSnapshotStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<JsonNode> load(String product) {
        ClassPathResource resource = new ClassPathResource("compat-snapshot/" + product + ".json");
        if (!resource.exists()) {
            return List.of();
        }
        try (InputStream in = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(in);
            List<JsonNode> cycles = new ArrayList<>();
            if (root != null && root.isArray()) {
                root.forEach(cycles::add);
            }
            return cycles;
        } catch (Exception e) {
            return List.of();
        }
    }
}
