package com.moveguard.sim;

import com.moveguard.compat.CompatMapper;
import com.moveguard.compat.CompatRelease;
import com.moveguard.diagnosis.DiagnosisEngine;
import com.moveguard.diagnosis.DiagnosisEngine.Assessment;
import com.moveguard.diagnosis.DiagnosisMapper;
import com.moveguard.diagnosis.RuleDefinition;
import com.moveguard.sim.OutcomeModel.Outcome;
import com.moveguard.sim.ScenarioGenerator.Scenario;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가상 이전사업을 대량 생성해 진단 엔진에 투입하고, (입력 특징, 진단 결과, 성패)를 만든다.
 * 진단 엔진·규칙·기준 데이터를 재사용하되, DB에 저장하지 않는다(인메모리 평가).
 */
@Service
@RequiredArgsConstructor
public class SimulationService {

    private final ScenarioGenerator generator;
    private final DiagnosisEngine engine;
    private final OutcomeModel outcomeModel;
    private final DiagnosisMapper diagnosisMapper;
    private final CompatMapper compatMapper;

    private static final List<String> OUTPUT_COLUMNS =
            List.of("riskLevel", "blocked", "maxRpn", "totalScore", "findingCount", "findingCodes", "outcome");

    /** 시뮬레이션 1건: 생성 특징 + 진단 결과 + 실제 성패 */
    public record SimResult(Map<String, String> features, Assessment assessment, Outcome outcome) {

        public boolean blocked() {
            return assessment.score().blocked();
        }

        public List<String> firedCodes() {
            return assessment.findings().stream().map(f -> f.rule().getRuleCode()).toList();
        }
    }

    /** count개 시나리오를 생성·진단·라벨링해 결과 목록으로 반환 (같은 seed → 같은 결과) */
    @Transactional(readOnly = true)
    public List<SimResult> simulate(int count, long seed) {
        Map<String, RuleDefinition> rules = diagnosisMapper.findEnabledRules().stream()
                .collect(Collectors.toMap(RuleDefinition::getRuleCode, Function.identity()));
        List<CompatRelease> releases = compatMapper.findAllReleases();

        List<SimResult> results = new ArrayList<>();
        Random rnd = new Random(seed);
        for (int i = 0; i < count; i++) {
            Scenario scenario = generator.generate(rnd, releases);
            Assessment assessment = engine.assess(scenario.context(), rules);
            Outcome outcome = outcomeModel.sample(scenario.features(), rnd);
            results.add(new SimResult(scenario.features(), assessment, outcome));
        }
        return results;
    }

    public String generateCsv(int count, long seed) {
        List<SimResult> results = simulate(count, seed);

        List<String> featureColumns = results.isEmpty()
                ? List.of() : new ArrayList<>(results.get(0).features().keySet());
        List<String> lines = new ArrayList<>();

        List<String> header = new ArrayList<>(featureColumns);
        header.addAll(OUTPUT_COLUMNS);
        lines.add(String.join(",", header));

        for (SimResult r : results) {
            List<String> row = new ArrayList<>();
            for (String col : featureColumns) {
                row.add(r.features().get(col));
            }
            row.add(r.assessment().score().level().name());
            row.add(String.valueOf(r.blocked()));
            row.add(String.valueOf(r.assessment().score().maxRpn()));
            row.add(r.assessment().score().totalScore().toPlainString());
            row.add(String.valueOf(r.assessment().findings().size()));
            row.add(String.join("|", r.firedCodes()));
            row.add(r.outcome().name());
            lines.add(String.join(",", row));
        }

        return String.join("\n", lines) + "\n";
    }
}
