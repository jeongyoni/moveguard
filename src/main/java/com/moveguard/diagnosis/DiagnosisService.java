package com.moveguard.diagnosis;

import com.moveguard.diagnosis.RiskScorer.RiskScore;
import com.moveguard.project.ProjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final RiskScorer riskScorer;
    private final List<RiskRuleEvaluator> evaluators;

    @Transactional
    public Optional<DiagnosisResult> diagnose(Long projectId) {
        if (projectMapper.findById(projectId).isEmpty()) {
            return Optional.empty();
        }

        DiagnosisContext context = contextLoader.load(projectId);
        Map<String, RuleDefinition> rules = diagnosisMapper.findEnabledRules().stream()
                .collect(Collectors.toMap(RuleDefinition::getRuleCode, Function.identity()));

        List<EvaluatedFinding> evaluated = new ArrayList<>();
        for (RiskRuleEvaluator evaluator : evaluators) {
            RuleDefinition rule = rules.get(evaluator.ruleCode());
            if (rule == null) {
                log.warn("미등록 또는 비활성 규칙이라 건너뜀: {}", evaluator.ruleCode());
                continue;
            }
            for (Finding finding : evaluator.evaluate(context)) {
                evaluated.add(new EvaluatedFinding(finding, rule, rule.renderMessage(finding.params())));
            }
        }
        evaluated.sort(Comparator.comparingInt(EvaluatedFinding::rpn).reversed());

        RiskScore score = riskScorer.score(evaluated);
        DiagnosisRun run = new DiagnosisRun(projectId, score.totalScore(), score.level(),
                score.blocked(), evaluated.size());
        diagnosisMapper.insertRun(run);

        if (!evaluated.isEmpty()) {
            diagnosisMapper.insertFindings(evaluated.stream()
                    .map(e -> new FindingRecord(run.getRunId(), e.rule().getRuleId(),
                            e.finding().assetId(), e.finding().dependencyId(), e.rpn(), e.message()))
                    .toList());
        }

        return Optional.of(DiagnosisResult.of(run.getRunId(), projectId, score, evaluated));
    }
}
