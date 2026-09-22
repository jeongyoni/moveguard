package com.moveguard.diagnosis;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** diagnosis_finding 저장용 */
@Getter
@AllArgsConstructor
public class FindingRecord {

    private Long runId;
    private Long ruleId;
    private Long assetId;
    private Long dependencyId;
    private int rpn;
    private String detail;
}
