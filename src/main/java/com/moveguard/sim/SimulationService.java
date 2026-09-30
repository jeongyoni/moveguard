package com.moveguard.sim;

import com.moveguard.compat.CompatMapper;
import com.moveguard.compat.CompatRelease;
import com.moveguard.diagnosis.DiagnosisEngine;
import com.moveguard.diagnosis.DiagnosisEngine.Assessment;
import com.moveguard.diagnosis.DiagnosisMapper;
import com.moveguard.diagnosis.RuleDefinition;
import com.moveguard.sim.ScenarioGenerator.Scenario;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가상 이전사업을 대량 생성해 진단 엔진에 투입하고, (입력 특징, 진단 결과)를 CSV로 만든다.
 * 진단 엔진과 규칙·기준 데이터를 재사용하되, DB에 저장하지 않는다(인메모리 평가).
 */
@Service
@RequiredArgsConstructor
public class SimulationService {

    private final ScenarioGenerator generator;
    private final DiagnosisEngine engine;
    private final DiagnosisMapper diagnosisMapper;
    private final CompatMapper compatMapper;

    private static final List<String> OUTPUT_COLUMNS =
            List.of("riskLevel", "blocked", "maxRpn", "totalScore", "findingCount", "findingCodes");

    @Transactional(readOnly = true)
    public String generateCsv(int count, long seed) {
        Map<String, RuleDefinition> rules = diagnosisMapper.findEnabledRules().stream()
                .collect(Collectors.toMap(RuleDefinition::getRuleCode, Function.identity()));
        List<CompatRelease> releases = compatMapper.findAllReleases();

        List<String> featureColumns = null;
        List<String> lines = new ArrayList<>();
        java.util.Random rnd = new java.util.Random(seed);

        for (int i = 0; i < count; i++) {
            Scenario scenario = generator.generate(rnd, releases);
            Assessment assessment = engine.assess(scenario.context(), rules);

            if (featureColumns == null) {
                featureColumns = new ArrayList<>(scenario.features().keySet());
                List<String> header = new ArrayList<>(featureColumns);
                header.addAll(OUTPUT_COLUMNS);
                lines.add(String.join(",", header));
            }

            List<String> row = new ArrayList<>();
            for (String col : featureColumns) {
                row.add(scenario.features().get(col));
            }
            String codes = assessment.findings().stream()
                    .map(f -> f.rule().getRuleCode())
                    .collect(Collectors.joining("|"));
            row.add(assessment.score().level().name());
            row.add(String.valueOf(assessment.score().blocked()));
            row.add(String.valueOf(assessment.score().maxRpn()));
            row.add(assessment.score().totalScore().toPlainString());
            row.add(String.valueOf(assessment.findings().size()));
            row.add(codes);
            lines.add(String.join(",", row));
        }

        return String.join("\n", lines) + "\n";
    }
}
