package com.moveguard.diagnosis;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 진단 실행 1건의 요약 (재진단 비교용). 컬럼 순서는 DiagnosisMapper.findLatestRun과 일치. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RunSummary {

    private Long runId;
    private LocalDateTime executedAt;
    private String riskLevel;
    private boolean blocked;
    private int findingCount;
    private int maxRpn;
    private BigDecimal totalScore;
}
