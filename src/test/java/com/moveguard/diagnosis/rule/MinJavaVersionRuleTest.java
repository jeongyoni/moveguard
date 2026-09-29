package com.moveguard.diagnosis.rule;

import static com.moveguard.asset.Phase.AFTER;
import static com.moveguard.diagnosis.rule.Fixtures.PLANNED_DATE;
import static com.moveguard.diagnosis.rule.Fixtures.WEB;
import static com.moveguard.diagnosis.rule.Fixtures.compatContext;
import static com.moveguard.diagnosis.rule.Fixtures.release;
import static com.moveguard.diagnosis.rule.Fixtures.sw;
import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.diagnosis.Finding;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MinJavaVersionRuleTest {

    private final MinJavaVersionRule rule = new MinJavaVersionRule();

    @Test
    @DisplayName("이전 후 WAS의 최소 Java(17)를 런타임(11)이 못 맞추면 CMP-02 발견")
    void detectsJavaBelowMinimum() {
        List<Finding> findings = rule.evaluate(compatContext(
                List.of(sw(WEB, "tomcat", "11.0.22", "11.0", AFTER),
                        sw(WEB, "java", "11.0.24", "11", AFTER)),
                List.of(release("tomcat", "11.0", false, null, "17")),
                PLANNED_DATE));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).params())
                .containsEntry("asset", "web01")
                .containsEntry("product", "tomcat")
                .containsEntry("required", "17")
                .containsEntry("current", "11.0.24");
    }

    @Test
    @DisplayName("런타임 Java가 최소 요구를 충족하면 해당 없음")
    void ignoresWhenJavaSufficient() {
        assertThat(rule.evaluate(compatContext(
                List.of(sw(WEB, "tomcat", "11.0.22", "11.0", AFTER),
                        sw(WEB, "java", "17.0.10", "17", AFTER)),
                List.of(release("tomcat", "11.0", false, null, "17")),
                PLANNED_DATE))).isEmpty();
    }

    @Test
    @DisplayName("Java 런타임 정보가 없으면 해당 없음")
    void ignoresWhenNoJavaInfo() {
        assertThat(rule.evaluate(compatContext(
                List.of(sw(WEB, "tomcat", "11.0.22", "11.0", AFTER)),
                List.of(release("tomcat", "11.0", false, null, "17")),
                PLANNED_DATE))).isEmpty();
    }

    @Test
    @DisplayName("최소 Java 정보가 없는 제품은 해당 없음")
    void ignoresWhenNoMinJava() {
        assertThat(rule.evaluate(compatContext(
                List.of(sw(WEB, "nginx", "1.27.0", "1.27", AFTER),
                        sw(WEB, "java", "11.0.24", "11", AFTER)),
                List.of(release("nginx", "1.27", false, null, null)),
                PLANNED_DATE))).isEmpty();
    }
}
