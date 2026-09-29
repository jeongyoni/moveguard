package com.moveguard.diagnosis.rule;

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
}
