package com.moveguard.sim;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 규칙(전환 차단)이 실제 성패를 얼마나 잘 예측하는지에 대한 분석.
 * 양성(positive) = 실제 FAIL, 예측 = blocked(차단)이면 FAIL로 본다.
 */
public record RuleAnalysis(
        int total, int truePositive, int falsePositive, int falseNegative, int trueNegative,
        double precision, double recall, double f1, double accuracy,
        List<RuleStat> ruleStats) {

    /** 규칙별: 발생 횟수와 그때의 실제 실패율 */
    public record RuleStat(String code, int fired, int firedAndFail, double failRate) {
    }

    /** 한 시나리오 관측: (실패로 예측했는가, 실제 실패였는가, 발생 규칙 코드들) */
    public record Observation(boolean predictedFail, boolean actualFail, List<String> firedCodes) {
    }

    public static RuleAnalysis compute(List<Observation> observations) {
        int tp = 0;
        int fp = 0;
        int fn = 0;
        int tn = 0;
        Map<String, int[]> perRule = new HashMap<>(); // code -> [fired, firedAndFail]

        for (Observation o : observations) {
            if (o.predictedFail() && o.actualFail()) {
                tp++;
            } else if (o.predictedFail() && !o.actualFail()) {
                fp++;
            } else if (!o.predictedFail() && o.actualFail()) {
                fn++;
            } else {
                tn++;
            }
            for (String code : o.firedCodes()) {
                int[] c = perRule.computeIfAbsent(code, k -> new int[2]);
                c[0]++;
                if (o.actualFail()) {
                    c[1]++;
                }
            }
        }

        int total = observations.size();
        double precision = ratio(tp, tp + fp);
        double recall = ratio(tp, tp + fn);
        double f1 = (precision + recall) == 0 ? 0.0 : 2 * precision * recall / (precision + recall);
        double accuracy = ratio(tp + tn, total);

        List<RuleStat> ruleStats = perRule.entrySet().stream()
                .map(e -> new RuleStat(e.getKey(), e.getValue()[0], e.getValue()[1],
                        round(ratio(e.getValue()[1], e.getValue()[0]))))
                .sorted(Comparator.comparingDouble(RuleStat::failRate).reversed()
                        .thenComparing(RuleStat::code))
                .toList();

        return new RuleAnalysis(total, tp, fp, fn, tn,
                round(precision), round(recall), round(f1), round(accuracy), ruleStats);
    }

    private static double ratio(int num, int denom) {
        return denom == 0 ? 0.0 : (double) num / denom;
    }

    private static double round(double v) {
        return Math.round(v * 10_000.0) / 10_000.0;
    }
}
