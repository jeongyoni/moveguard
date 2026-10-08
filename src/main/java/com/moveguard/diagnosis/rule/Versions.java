package com.moveguard.diagnosis.rule;

import java.util.ArrayList;
import java.util.List;

/** 릴리스 라인 문자열에서 메이저 버전을 뽑는 도우미 */
final class Versions {

    private Versions() {
    }

    /** "9.0" → 9, "11" → 11, "5.7" → 5, 파싱 불가면 null */
    static Integer major(String releaseLine) {
        if (releaseLine == null || releaseLine.isBlank()) {
            return null;
        }
        String head = releaseLine.trim().split("\\.")[0];
        try {
            return Integer.parseInt(head);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 점으로 구분된 버전을 숫자 세그먼트 단위로 비교. 없는 자리는 0으로 본다.
     * "8.0.33" vs "8.0.11" → 양수, "5.1.49" vs "8.0.11" → 음수, "19.3" vs "19.3.0.0" → 0.
     */
    static int compare(String a, String b) {
        List<Integer> x = segments(a);
        List<Integer> y = segments(b);
        int n = Math.max(x.size(), y.size());
        for (int i = 0; i < n; i++) {
            int xi = i < x.size() ? x.get(i) : 0;
            int yi = i < y.size() ? y.get(i) : 0;
            if (xi != yi) {
                return Integer.compare(xi, yi);
            }
        }
        return 0;
    }

    private static List<Integer> segments(String v) {
        List<Integer> out = new ArrayList<>();
        if (v == null) {
            return out;
        }
        for (String s : v.trim().split("[^0-9]+")) {
            if (!s.isEmpty()) {
                try {
                    out.add(Integer.parseInt(s));
                } catch (NumberFormatException ignore) {
                    // 세그먼트가 int 범위를 넘으면 건너뛴다
                }
            }
        }
        return out;
    }
}
