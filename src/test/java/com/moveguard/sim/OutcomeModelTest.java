package com.moveguard.sim;

import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.sim.OutcomeModel.Outcome;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutcomeModelTest {

    private final OutcomeModel model = new OutcomeModel();

    private Map<String, String> features(String ipChanges, String whitelisted, String hardcoded,
            String dnsAfter, String certExpiring, String certAfter, String tomcatFrom,
            String tomcatTo, String javaAfter, String dbFrom, String dbTo, String protocol) {
        Map<String, String> f = new java.util.HashMap<>();
        f.put("ipChanges", ipChanges);
        f.put("whitelisted", whitelisted);
        f.put("hardcoded", hardcoded);
        f.put("dnsAfter", dnsAfter);
        f.put("certExpiring", certExpiring);
        f.put("certAfter", certAfter);
        f.put("tomcatFrom", tomcatFrom);
        f.put("tomcatTo", tomcatTo);
        f.put("javaAfter", javaAfter);
        f.put("dbFrom", dbFrom);
        f.put("dbTo", dbTo);
        f.put("protocol", protocol);
        return f;
    }

    private Map<String, String> lowRisk() {
        return features("false", "false", "false", "true", "false", "true",
                "11.0", "11.0", "17", "8.4", "8.4", "JDBC");
    }

    private Map<String, String> highRisk() {
        return features("true", "true", "true", "false", "true", "false",
                "9.0", "11.0", "8", "5.7", "9.6", "HTTP");
    }

    private long failCount(Map<String, String> features, long seed, int n) {
        Random rnd = new Random(seed);
        long fails = 0;
        for (int i = 0; i < n; i++) {
            if (model.sample(features, rnd) == Outcome.FAIL) {
                fails++;
            }
        }
        return fails;
    }

    @Test
    @DisplayName("고위험 시나리오는 실패율이 높다")
    void highRiskFailsOften() {
        long fails = failCount(highRisk(), 1L, 1000);
        assertThat(fails).isGreaterThan(800);
    }

    @Test
    @DisplayName("저위험 시나리오는 성공률이 높다")
    void lowRiskSucceedsOften() {
        long fails = failCount(lowRisk(), 1L, 1000);
        assertThat(fails).isLessThan(200);
    }

    @Test
    @DisplayName("고위험이 저위험보다 실패 로짓이 크다")
    void highRiskHasHigherLogit() {
        assertThat(model.failureLogit(highRisk()))
                .isGreaterThan(model.failureLogit(lowRisk()));
    }

    @Test
    @DisplayName("평문 프로토콜은 성패에 영향을 주지 않는다 (규칙과 라벨의 의도적 불일치)")
    void plaintextProtocolDoesNotAffectOutcome() {
        Map<String, String> https = new java.util.HashMap<>(lowRisk());
        https.put("protocol", "HTTPS");
        Map<String, String> http = new java.util.HashMap<>(lowRisk());
        http.put("protocol", "HTTP");

        assertThat(model.failureLogit(http)).isEqualTo(model.failureLogit(https));
    }

    @Test
    @DisplayName("같은 시드면 같은 라벨 (재현성)")
    void isReproducible() {
        assertThat(failCount(highRisk(), 7L, 500))
                .isEqualTo(failCount(highRisk(), 7L, 500));
    }
}
