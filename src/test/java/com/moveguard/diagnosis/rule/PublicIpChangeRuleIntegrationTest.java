package com.moveguard.diagnosis.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.moveguard.diagnosis.DiagnosisContext;
import com.moveguard.diagnosis.DiagnosisContextLoader;
import com.moveguard.diagnosis.Finding;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 로컬 MySQL에 data.sql 더미 사업(project_id = 1)이 적재되어 있어야 한다. */
@SpringBootTest
class PublicIpChangeRuleIntegrationTest {

    @Autowired
    private DiagnosisContextLoader loader;

    @Autowired
    private PublicIpChangeRule rule;

    @Test
    @DisplayName("더미 이전사업에서 web01 → db01 공인IP 접속이 IP-01로 발견된다")
    void detectsInSeedProject() {
        DiagnosisContext context = loader.load(1L);

        List<Finding> findings = rule.evaluate(context);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).params())
                .containsEntry("asset", "web01")
                .containsEntry("target", "db01")
                .containsEntry("address", "203.0.113.21")
                .containsEntry("config", "application.yml spring.datasource.url");
    }
}
