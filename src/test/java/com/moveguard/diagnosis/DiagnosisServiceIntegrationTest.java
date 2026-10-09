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
    @DisplayName("더미 환경 진단 → 운영 헬스체크 세트(버전·보안·백업)가 RPN 내림차순으로 발견, HIGH, 즉시 조치")
    void diagnosesSeedProject() {
        DiagnosisResult result = diagnosisService.diagnose(1L).orElseThrow();

        assertThat(result.runId()).isNotNull();
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.blocked()).isTrue();
        assertThat(result.maxRpn()).isEqualTo(420);   // EOL 버전(리눅스 OS·DB) 운영 = 치명적
        assertThat(result.totalScore()).isEqualByComparingTo("15.15");
        // 운영 헬스체크 세트(SECURITY·BACKUP·COMPAT 일부) 활성, 전환 전용(NETWORK_IP·DNS·CMP-03/05) 비활성.
        // CentOS 7(서버 OS) EOL이 web01·db01 두 서버에서 각각 CMP-01로 잡혀 mysql 9.6 EOL과 함께 최상위에 온다.
        assertThat(result.findings())
                .extracting(DiagnosisResult.Item::ruleCode)
                .containsExactly("CMP-01", "CMP-01", "CMP-01", "CMP-02", "PORT-01", "BAK-02", "CMP-07", "CERT-01", "BAK-03");
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
