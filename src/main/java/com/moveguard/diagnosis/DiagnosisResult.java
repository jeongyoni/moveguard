package com.moveguard.diagnosis;

import com.moveguard.diagnosis.RiskScorer.RiskScore;
import java.math.BigDecimal;
import java.util.List;

/** 진단 API 응답 */
public record DiagnosisResult(
        Long runId,
        Long projectId,
        BigDecimal totalScore,
        RiskLevel riskLevel,
        boolean blocked,
        int maxRpn,
        List<Item> findings) {

    public record Item(String ruleCode, String title, int rpn, boolean blocking,
                       String message, String mitigation) {
    }

    static DiagnosisResult of(Long runId, Long projectId, RiskScore score,
                              List<EvaluatedFinding> findings) {
        List<Item> items = findings.stream()
                .map(f -> new Item(
                        f.rule().getRuleCode(),
                        f.rule().getTitle(),
                        f.rpn(),
                        f.rule().isBlocking(),
                        f.message(),
                        f.rule().getMitigation()))
                .toList();
        return new DiagnosisResult(runId, projectId, score.totalScore(), score.level(),
                score.blocked(), score.maxRpn(), items);
    }
}
