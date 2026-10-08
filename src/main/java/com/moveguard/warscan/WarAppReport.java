package com.moveguard.warscan;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** WAR/JAR 애플리케이션 분석 결과. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WarAppReport {

    private int classCount;        // 분석한 .class 수
    private int maxClassMajor;     // 가장 높은 class 파일 포맷 major (0이면 없음)
    private String requiredJava;   // maxClassMajor로 환산한 최소 실행 Java (예: "8", "11", "17")
    private boolean usesJavax;     // javax.servlet 사용 흔적
    private boolean usesJakarta;   // jakarta.servlet 사용 흔적
    private String servletSpec;    // web.xml의 서블릿 스펙 버전 (없으면 null)
    private List<WarLib> libraries; // WEB-INF/lib·BOOT-INF/lib의 라이브러리

    public boolean hasFindings() {
        return classCount > 0 || servletSpec != null || (libraries != null && !libraries.isEmpty());
    }

    /** class major 버전 → 사람이 읽는 Java 피처 버전 */
    public static String javaFromMajor(int major) {
        if (major <= 0) {
            return null;
        }
        if (major >= 49) {              // Java 5(49) 이상: major-44
            return String.valueOf(major - 44);
        }
        return "1." + (major - 44);     // 1.1(45)~1.4(48)
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarLib {
        private String name;
        private String version;  // 파일명에서 뽑은 버전 (없으면 null)
    }
}
