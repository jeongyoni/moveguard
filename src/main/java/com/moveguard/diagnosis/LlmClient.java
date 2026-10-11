package com.moveguard.diagnosis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 로컬 LLM(Ollama) HTTP API 호출기. 외부 AI API를 쓰지 않고 사내/로컬에서만 처리한다.
 * 비활성·미가용(연결 거부)·타임아웃·오류 시 {@link Optional#empty()}를 돌려 규칙 기반 폴백을 유도한다.
 */
@Component
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final boolean enabled;
    private final String baseUrl;
    private final String model;
    private final Duration timeout;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public LlmClient(@Value("${moveguard.llm.enabled:true}") boolean enabled,
                     @Value("${moveguard.llm.base-url:http://localhost:11434}") String baseUrl,
                     @Value("${moveguard.llm.model:qwen2.5:3b}") String model,
                     @Value("${moveguard.llm.timeout-seconds:25}") int timeoutSeconds) {
        this.enabled = enabled;
        this.baseUrl = baseUrl;
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String model() {
        return model;
    }

    /** 프롬프트로 생성한 텍스트. 비활성·미가용·오류 시 empty(→ 규칙 기반 폴백). */
    public Optional<String> generate(String prompt) {
        if (!enabled) {
            return Optional.empty();
        }
        try {
            String payload = mapper.writeValueAsString(Map.of(
                    "model", model,
                    "prompt", prompt,
                    "stream", false,
                    "options", Map.of("temperature", 0.3)));
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/generate"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                return Optional.empty();
            }
            Map<?, ?> json = mapper.readValue(resp.body(), Map.class);
            Object text = json.get("response");
            if (text == null || text.toString().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(text.toString().trim());
        } catch (Exception e) {
            log.debug("LLM 생성 실패 → 규칙 기반 폴백: {}", e.toString());
            return Optional.empty();
        }
    }
}
