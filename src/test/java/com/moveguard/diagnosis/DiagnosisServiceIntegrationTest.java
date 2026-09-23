package com.moveguard.diagnosis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 로컬 MySQL에 더미 사업(project_id = 1) 필요. 테스트 후 저장 내용은 롤백된다 */
@SpringBootTest
@Transactional
class DiagnosisServiceIntegrationTest {

    @Autowired
    private DiagnosisService diagnosisService;

    @Test
    @DisplayName("더미 사업 진단 → 네트워크 규칙 3건(IP-01·02·03) 발견, HIGH, 전환 차단")
    void diagnosesSeedProject() {
        DiagnosisResult result = diagnosisService.diagnose(1L).orElseThrow();

        assertThat(result.runId()).isNotNull();
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.blocked()).isTrue();
        // 규칙이 추가될 때마다 기대값을 함께 갱신한다
        assertThat(result.findings())
                .extracting(DiagnosisResult.Item::ruleCode)
                .containsExactlyInAnyOrder("IP-01", "IP-02", "IP-03");
        assertThat(result.findings())
                .extracting(DiagnosisResult.Item::message)
                .noneMatch(message -> message.contains("{"));
    }

    @Test
    @DisplayName("없는 사업이면 결과 없음")
    void unknownProject() {
        assertThat(diagnosisService.diagnose(999L)).isEmpty();
    }
}
