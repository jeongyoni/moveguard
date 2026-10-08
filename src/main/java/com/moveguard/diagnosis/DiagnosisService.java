package com.moveguard.diagnosis;

import com.moveguard.diagnosis.DiagnosisEngine.Assessment;
import com.moveguard.diagnosis.RiskScorer.RiskScore;
import com.moveguard.project.ProjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private final ProjectMapper projectMapper;
    private final DiagnosisContextLoader contextLoader;
    private final DiagnosisMapper diagnosisMapper;
    private final DiagnosisEngine engine;

    @Transactional
    public Optional<DiagnosisResult> diagnose(Long projectId) {
        if (projectMapper.findById(projectId).isEmpty()) {
            return Optional.empty();
        }

        DiagnosisContext context = contextLoader.load(projectId);
        Map<String, RuleDefinition> rules = diagnosisMapper.findEnabledRules().stream()
                .collect(Collectors.toMap(RuleDefinition::getRuleCode, Function.identity()));

        Assessment assessment = engine.assess(context, rules);
        List<EvaluatedFinding> evaluated = assessment.findings();
        RiskScore score = assessment.score();

        // 이번 실행을 저장하기 전의 최신 실행이 "직전 진단" 비교 기준이 된다.
        RunSummary previous = diagnosisMapper.findLatestRun(projectId);

        DiagnosisRun run = new DiagnosisRun(projectId, score.totalScore(), score.level(),
                score.blocked(), evaluated.size());
        diagnosisMapper.insertRun(run);

        if (!evaluated.isEmpty()) {
            diagnosisMapper.insertFindings(evaluated.stream()
                    .map(e -> new FindingRecord(run.getRunId(), e.rule().getRuleId(),
                            e.finding().assetId(), e.finding().dependencyId(), e.rpn(), e.message()))
                    .toList());
        }

        return Optional.of(DiagnosisResult.of(run.getRunId(), projectId, score, evaluated, previous));
    }
}
