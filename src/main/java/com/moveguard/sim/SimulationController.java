package com.moveguard.sim;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sim")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;
    private final RuleAnalysisService ruleAnalysisService;

    /** 가상 이전사업 count개를 생성·진단해 (특징, 결과) 데이터셋을 CSV로 반환한다. */
    @GetMapping(value = "/dataset", produces = "text/csv")
    public String dataset(@RequestParam(defaultValue = "100") int count,
                          @RequestParam(defaultValue = "42") long seed) {
        return simulationService.generateCsv(count, seed);
    }

    /** 규칙(전환 차단)이 실제 성패를 얼마나 잘 예측하는지 분석 리포트를 반환한다. */
    @GetMapping("/analysis")
    public RuleAnalysis analysis(@RequestParam(defaultValue = "1000") int count,
                                 @RequestParam(defaultValue = "42") long seed) {
        return ruleAnalysisService.analyze(count, seed);
    }
}
