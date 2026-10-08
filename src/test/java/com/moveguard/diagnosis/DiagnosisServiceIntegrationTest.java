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
    @DisplayName("더미 사업 진단 → 네트워크·DNS·보안·호환성 규칙이 RPN 내림차순으로 발견, HIGH, 전환 차단")
    void diagnosesSeedProject() {
        DiagnosisResult result = diagnosisService.diagnose(1L).orElseThrow();

        assertThat(result.runId()).isNotNull();
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.blocked()).isTrue();
        assertThat(result.maxRpn()).isEqualTo(504);
        assertThat(result.totalScore()).isEqualByComparingTo("35.10");
        // 규칙이 추가될 때마다 기대값을 함께 갱신한다
        assertThat(result.findings())
                .extracting(DiagnosisResult.Item::ruleCode)
                .containsExactly("IP-04", "IP-01", "CMP-03", "CMP-02", "PORT-01", "IP-03", "DNS-01",
                        "BAK-02", "CMP-07", "CERT-01", "IP-02", "CMP-05", "CMP-05", "CMP-05",
                        "CMP-01", "BAK-03");
        assertThat(result.findings())
                .extracting(DiagnosisResult.Item::message)
                .noneMatch(message -> message.contains("{"));
    }

    @Test
    @DisplayName("재진단 시 직전 실행 요약(previous)이 비교 기준으로 채워진다")
    void reDiagnosisCarriesPrevious() {
        DiagnosisResult first = diagnosisService.diagnose(1L).orElseThrow();
        DiagnosisResult second = diagnosisService.diagnose(1L).orElseThrow();

        assertThat(second.previous()).isNotNull();
        assertThat(second.previous().getRunId()).isEqualTo(first.runId());
        assertThat(second.previous().getFindingCount()).isEqualTo(first.findings().size());
        assertThat(second.previous().getMaxRpn()).isEqualTo(first.maxRpn());
        assertThat(second.previous().getRiskLevel()).isEqualTo(first.riskLevel().name());
    }

    @Test
    @DisplayName("없는 사업이면 결과 없음")
    void unknownProject() {
        assertThat(diagnosisService.diagnose(999L)).isEmpty();
    }
}
