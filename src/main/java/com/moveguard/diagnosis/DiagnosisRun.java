package com.moveguard.diagnosis;

import java.math.BigDecimal;
import lombok.Getter;

/** diagnosis_run 저장용. runId는 INSERT 후 MyBatis가 채운다 */
@Getter
public class DiagnosisRun {

    private Long runId;
    private final Long projectId;
    private final BigDecimal totalScore;
    private final RiskLevel riskLevel;
    private final boolean blocked;
    private final int findingCount;

    public DiagnosisRun(Long projectId, BigDecimal totalScore, RiskLevel riskLevel,
                        boolean blocked, int findingCount) {
        this.projectId = projectId;
        this.totalScore = totalScore;
        this.riskLevel = riskLevel;
        this.blocked = blocked;
        this.findingCount = findingCount;
    }
}
