package com.moveguard.diagnosis.rule;

import static com.moveguard.asset.Phase.AFTER;
import static com.moveguard.asset.Phase.BEFORE;
import static com.moveguard.diagnosis.rule.Fixtures.PLANNED_DATE;
import static com.moveguard.diagnosis.rule.Fixtures.WEB;
import static com.moveguard.diagnosis.rule.Fixtures.compatContext;
import static com.moveguard.diagnosis.rule.Fixtures.sw;
import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.diagnosis.Finding;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JavaxToJakartaRuleTest {

    private final JavaxToJakartaRule rule = new JavaxToJakartaRule();

    @Test
    @DisplayName("Tomcat 9 → 11 이전이면 CMP-03 발견")
    void detectsTomcat9To11() {
        List<Finding> findings = rule.evaluate(compatContext(
                List.of(sw(WEB, "tomcat", "9.0.80", "9.0", BEFORE),
                        sw(WEB, "tomcat", "11.0.22", "11.0", AFTER)),
                List.of(), PLANNED_DATE));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).params())
                .containsEntry("asset", "web01")
                .containsEntry("from", "9.0.80")
                .containsEntry("to", "11.0.22");
    }

    @Test
    @DisplayName("Tomcat 10 → 11 (이미 jakarta)이면 해당 없음")
    void ignoresWithin10Plus() {
        assertThat(rule.evaluate(compatContext(
                List.of(sw(WEB, "tomcat", "10.1.20", "10.1", BEFORE),
                        sw(WEB, "tomcat", "11.0.22", "11.0", AFTER)),
                List.of(), PLANNED_DATE))).isEmpty();
    }

    @Test
    @DisplayName("Tomcat 9 → 9 (유지)면 해당 없음")
    void ignoresSameMajor() {
        assertThat(rule.evaluate(compatContext(
                List.of(sw(WEB, "tomcat", "9.0.70", "9.0", BEFORE),
                        sw(WEB, "tomcat", "9.0.80", "9.0", AFTER)),
                List.of(), PLANNED_DATE))).isEmpty();
    }

    @Test
    @DisplayName("이전 전 Tomcat 정보가 없으면 해당 없음")
    void ignoresWhenNoBefore() {
        assertThat(rule.evaluate(compatContext(
                List.of(sw(WEB, "tomcat", "11.0.22", "11.0", AFTER)),
                List.of(), PLANNED_DATE))).isEmpty();
    }
}
