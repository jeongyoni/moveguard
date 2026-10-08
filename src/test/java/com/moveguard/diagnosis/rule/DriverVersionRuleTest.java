package com.moveguard.diagnosis.rule;

import static com.moveguard.asset.Phase.AFTER;
import static com.moveguard.asset.Phase.BEFORE;
import static com.moveguard.diagnosis.rule.Fixtures.DB;
import static com.moveguard.diagnosis.rule.Fixtures.WEB;
import static com.moveguard.diagnosis.rule.Fixtures.driverContext;
import static com.moveguard.diagnosis.rule.Fixtures.driverReq;
import static com.moveguard.diagnosis.rule.Fixtures.sw;
import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.diagnosis.Finding;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DriverVersionRuleTest {

    private final DriverVersionRule rule = new DriverVersionRule();

    @Test
    @DisplayName("목표 DB 요구치보다 낮은 드라이버면 CMP-07 발견")
    void detectsOutdatedDriver() {
        List<Finding> findings = rule.evaluate(driverContext(
                List.of(
                        sw(DB, "mysql", "8.4.0", "8.4", AFTER),
                        sw(WEB, "mysql-connector-j", "5.1.49", "5.1", AFTER)),
                List.of(driverReq("mysql", "8.4", "mysql-connector-j", "8.4.0"))));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).assetId()).isEqualTo(WEB.getAssetId());
        assertThat(findings.get(0).params())
                .containsEntry("driver", "mysql-connector-j")
                .containsEntry("version", "5.1.49")
                .containsEntry("required", "8.4.0")
                .containsEntry("db", "mysql 8.4");
    }

    @Test
    @DisplayName("요구치를 충족하면 해당 없음")
    void ignoresSufficientDriver() {
        assertThat(rule.evaluate(driverContext(
                List.of(
                        sw(DB, "mysql", "8.4.0", "8.4", AFTER),
                        sw(WEB, "mysql-connector-j", "8.4.0", "8.4", AFTER)),
                List.of(driverReq("mysql", "8.4", "mysql-connector-j", "8.4.0"))))).isEmpty();
    }

    @Test
    @DisplayName("목표 DB 라인이 매트릭스와 다르면 적용되지 않는다")
    void ignoresWhenDbLineDiffers() {
        assertThat(rule.evaluate(driverContext(
                List.of(
                        sw(DB, "mysql", "8.0.36", "8.0", AFTER),
                        sw(WEB, "mysql-connector-j", "5.1.49", "5.1", AFTER)),
                List.of(driverReq("mysql", "8.4", "mysql-connector-j", "8.4.0"))))).isEmpty();
    }

    @Test
    @DisplayName("이전 전(BEFORE) 드라이버는 검사하지 않는다")
    void ignoresBeforePhase() {
        assertThat(rule.evaluate(driverContext(
                List.of(
                        sw(DB, "mysql", "8.4.0", "8.4", AFTER),
                        sw(WEB, "mysql-connector-j", "5.1.49", "5.1", BEFORE)),
                List.of(driverReq("mysql", "8.4", "mysql-connector-j", "8.4.0"))))).isEmpty();
    }

    @Test
    @DisplayName("매트릭스가 비어 있으면 해당 없음")
    void ignoresWhenNoRequirements() {
        assertThat(rule.evaluate(driverContext(
                List.of(
                        sw(DB, "mysql", "8.4.0", "8.4", AFTER),
                        sw(WEB, "mysql-connector-j", "5.1.49", "5.1", AFTER)),
                List.of()))).isEmpty();
    }

    @Test
    @DisplayName("db_release_line이 NULL이면 제품 전체에 적용")
    void appliesToWholeProductWhenLineNull() {
        assertThat(rule.evaluate(driverContext(
                List.of(
                        sw(DB, "mysql", "9.6.1", "9.6", AFTER),
                        sw(WEB, "mariadb-java-client", "2.4.0", "2.4", AFTER)),
                List.of(driverReq("mysql", null, "mariadb-java-client", "2.7.0")))))
                .hasSize(1);
    }
}
