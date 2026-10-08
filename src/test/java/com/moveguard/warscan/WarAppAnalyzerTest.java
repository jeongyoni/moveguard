package com.moveguard.warscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WarAppAnalyzerTest {

    private final WarAppAnalyzer analyzer = new WarAppAnalyzer();

    /** CAFEBABE + minor(0) + major + 시그니처 문자열을 담은 가짜 .class */
    private byte[] classFile(int major, String signature) {
        byte[] sig = signature.getBytes(StandardCharsets.US_ASCII);
        byte[] out = new byte[8 + sig.length];
        out[0] = (byte) 0xCA; out[1] = (byte) 0xFE; out[2] = (byte) 0xBA; out[3] = (byte) 0xBE;
        out[4] = 0; out[5] = 0;
        out[6] = (byte) ((major >> 8) & 0xFF);
        out[7] = (byte) (major & 0xFF);
        System.arraycopy(sig, 0, out, 8, sig.length);
        return out;
    }

    private byte[] war(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                zos.write(e.getValue());
                zos.closeEntry();
            }
        }
        return bos.toByteArray();
    }

    @Test
    @DisplayName("Java 버전·javax/jakarta·서블릿 스펙·라이브러리를 추출한다")
    void analyzesApp() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("WEB-INF/classes/com/app/OldServlet.class",
                classFile(52, "javax/servlet/http/HttpServlet"));      // Java 8 + javax
        entries.put("WEB-INF/classes/com/app/NewServlet.class",
                classFile(61, "jakarta/servlet/http/HttpServlet"));    // Java 17 + jakarta
        entries.put("WEB-INF/web.xml",
                ("<web-app xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" version=\"4.0\">"
                        + "</web-app>").getBytes(StandardCharsets.UTF_8));
        entries.put("WEB-INF/lib/spring-web-6.1.0.jar", new byte[]{1, 2, 3});
        entries.put("WEB-INF/lib/jakarta.servlet-api-6.0.0.jar", new byte[]{1});
        entries.put("WEB-INF/lib/mysql-connector-j-8.0.33.jar", new byte[]{1});
        entries.put("WEB-INF/classes/application.properties",
                "name=x\n".getBytes(StandardCharsets.UTF_8));           // 무시 대상

        WarAppReport r = analyzer.analyze(new ByteArrayInputStream(war(entries)));

        assertThat(r.getClassCount()).isEqualTo(2);
        assertThat(r.getMaxClassMajor()).isEqualTo(61);
        assertThat(r.getRequiredJava()).isEqualTo("17");
        assertThat(r.isUsesJavax()).isTrue();
        assertThat(r.isUsesJakarta()).isTrue();
        assertThat(r.getServletSpec()).isEqualTo("4.0");

        Map<String, String> libs = r.getLibraries().stream()
                .collect(Collectors.toMap(WarAppReport.WarLib::getName, WarAppReport.WarLib::getVersion));
        assertThat(libs).containsEntry("spring-web", "6.1.0")
                .containsEntry("jakarta.servlet-api", "6.0.0")
                .containsEntry("mysql-connector-j", "8.0.33");

        // JDBC 드라이버만 driverProduct가 채워진다
        Map<String, String> drivers = r.getLibraries().stream()
                .filter(l -> l.getDriverProduct() != null)
                .collect(Collectors.toMap(WarAppReport.WarLib::getName, WarAppReport.WarLib::getDriverProduct));
        assertThat(drivers).containsExactlyEntriesOf(Map.of("mysql-connector-j", "mysql-connector-j"));
    }

    @Test
    @DisplayName("class·lib·web.xml이 없으면 findings 없음")
    void emptyWhenNoApp() throws IOException {
        byte[] w = war(Map.of("README.txt", "hi".getBytes(StandardCharsets.UTF_8)));
        WarAppReport r = analyzer.analyze(new ByteArrayInputStream(w));
        assertThat(r.hasFindings()).isFalse();
        assertThat(r.getRequiredJava()).isNull();
    }

    @Test
    @DisplayName("class major → Java 버전 환산")
    void majorMapping() {
        assertThat(WarAppReport.javaFromMajor(52)).isEqualTo("8");
        assertThat(WarAppReport.javaFromMajor(55)).isEqualTo("11");
        assertThat(WarAppReport.javaFromMajor(61)).isEqualTo("17");
        assertThat(WarAppReport.javaFromMajor(65)).isEqualTo("21");
        assertThat(WarAppReport.javaFromMajor(0)).isNull();
    }
}
