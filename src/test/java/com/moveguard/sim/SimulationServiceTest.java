package com.moveguard.sim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.moveguard.compat.CompatMapper;
import com.moveguard.diagnosis.DiagnosisEngine;
import com.moveguard.diagnosis.DiagnosisMapper;
import com.moveguard.diagnosis.RiskScorer;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SimulationServiceTest {

    private SimulationService service;

    @BeforeEach
    void setUp() {
        DiagnosisMapper diagnosisMapper = Mockito.mock(DiagnosisMapper.class);
        CompatMapper compatMapper = Mockito.mock(CompatMapper.class);
        when(diagnosisMapper.findEnabledRules()).thenReturn(List.of());
        when(compatMapper.findAllReleases()).thenReturn(List.of());

        // 규칙 없는 빈 엔진 → findings 0건, 파이프라인·CSV 구조만 검증
        DiagnosisEngine engine = new DiagnosisEngine(List.of(), new RiskScorer());
        service = new SimulationService(new ScenarioGenerator(), engine, new OutcomeModel(),
                diagnosisMapper, compatMapper);
    }

    @Test
    @DisplayName("count개 행 + 헤더로 CSV를 만든다")
    void producesHeaderAndRows() {
        String csv = service.generateCsv(5, 42L);
        String[] lines = csv.strip().split("\n");

        assertThat(lines).hasSize(6); // 헤더 1 + 5행
        assertThat(lines[0]).startsWith("ipChanges,")
                .contains("riskLevel", "blocked", "maxRpn", "totalScore", "findingCount",
                        "findingCodes", "outcome");
        // 규칙이 없으므로 모든 행은 위험 없음
        assertThat(lines[1]).contains("LOW");
        // 마지막 컬럼은 성패 라벨
        assertThat(lines[1]).matches(".*(SUCCESS|FAIL)$");
    }

    @Test
    @DisplayName("같은 시드면 같은 CSV (재현성)")
    void isReproducible() {
        assertThat(service.generateCsv(10, 7L)).isEqualTo(service.generateCsv(10, 7L));
    }

    @Test
    @DisplayName("모든 행의 컬럼 수가 헤더와 일치한다")
    void rowsMatchHeaderWidth() {
        String[] lines = service.generateCsv(20, 1L).strip().split("\n");
        int width = lines[0].split(",", -1).length;
        for (String line : lines) {
            assertThat(line.split(",", -1)).hasSize(width);
        }
    }
}
