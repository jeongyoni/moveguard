package com.moveguard.sim;

import com.moveguard.sim.OutcomeModel.Outcome;
import com.moveguard.sim.RuleAnalysis.Observation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 시뮬레이션 데이터로 규칙(전환 차단)의 예측 성능을 분석한다. */
@Service
@RequiredArgsConstructor
public class RuleAnalysisService {

    private final SimulationService simulationService;

    public RuleAnalysis analyze(int count, long seed) {
        List<Observation> observations = simulationService.simulate(count, seed).stream()
                .map(r -> new Observation(r.blocked(), r.outcome() == Outcome.FAIL, r.firedCodes()))
                .toList();
        return RuleAnalysis.compute(observations);
    }
}
