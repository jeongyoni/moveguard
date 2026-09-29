package com.moveguard.compat;

import tools.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * endoflife.date REST API 클라이언트.
 * 제품별 릴리스(cycle) 목록을 원본 JSON 노드로 반환한다. 실패 시 예외를 던져 스냅샷 fallback으로 넘긴다.
 */
@Component
public class EndOfLifeClient {

    private final RestClient restClient;

    public EndOfLifeClient(@Value("${compat.endoflife.base-url:https://endoflife.date/api}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<JsonNode> fetchCycles(String product) {
        JsonNode root = restClient.get()
                .uri("/{product}.json", product)
                .retrieve()
                .body(JsonNode.class);

        List<JsonNode> cycles = new ArrayList<>();
        if (root != null && root.isArray()) {
            root.forEach(cycles::add);
        }
        return cycles;
    }
}
