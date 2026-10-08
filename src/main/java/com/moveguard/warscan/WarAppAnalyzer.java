package com.moveguard.warscan;

import com.moveguard.warscan.WarAppReport.WarLib;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.stereotype.Service;

/**
 * WAR/JAR(ZIP)의 애플리케이션 특성을 분석한다.
 * - 컴파일된 Java 버전: .class 파일 헤더의 major 버전
 * - javax / jakarta 서블릿 사용: 클래스 상수풀의 경로 문자열
 * - 서블릿 스펙 버전: WEB-INF/web.xml
 * - 라이브러리 목록: WEB-INF/lib·BOOT-INF/lib의 .jar 파일명
 * 기존 IP 스캐너와 동일하게 메모리 스트림으로만 읽고 총량·엔트리 수를 제한한다.
 */
@Service
public class WarAppAnalyzer {

    private static final long MAX_TOTAL_BYTES = 200L * 1024 * 1024;
    private static final int MAX_ENTRIES = 10_000;
    private static final int MAX_CLASS_SCAN = 512 * 1024; // 서블릿 시그니처 탐색용 class 읽기 상한
    private static final int MAX_XML_BYTES = 1024 * 1024;

    private static final byte[] JAVAX = "javax/servlet".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] JAKARTA = "jakarta/servlet".getBytes(StandardCharsets.US_ASCII);

    private static final Pattern LIB = Pattern.compile("(.+?)-(\\d[\\w.]*)\\.jar$");
    private static final Pattern WEBAPP_VERSION = Pattern.compile("<web-app[^>]*\\sversion\\s*=\\s*\"([\\d.]+)\"");
    private static final Pattern JAKARTA_NS = Pattern.compile("jakarta\\.ee/xml/ns");

    public WarAppReport analyze(InputStream war) throws IOException {
        int classCount = 0;
        int maxMajor = 0;
        boolean usesJavax = false;
        boolean usesJakarta = false;
        String servletSpec = null;
        TreeMap<String, String> libs = new TreeMap<>();
        long total = 0;
        int entries = 0;

        try (ZipInputStream zis = new ZipInputStream(war)) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                if (++entries > MAX_ENTRIES) {
                    throw new IOException("압축 파일의 항목 수가 너무 많습니다.");
                }
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                boolean isClass = name.endsWith(".class");
                boolean isLib = name.endsWith(".jar") && (name.contains("/lib/") || name.startsWith("lib/"));
                boolean isWebXml = name.endsWith("WEB-INF/web.xml") || name.equals("WEB-INF/web.xml");

                // 보관할 바이트 상한(나머지는 총량 집계용으로 드레인). 관심 없는 엔트리는 0.
                int cap = 0;
                if (isClass) {
                    cap = (!usesJavax || !usesJakarta) ? MAX_CLASS_SCAN : 8;
                } else if (isWebXml) {
                    cap = MAX_XML_BYTES;
                }

                Read r = readEntry(zis, buf, cap, total);
                total += r.drained;
                byte[] data = r.kept;
                if (total > MAX_TOTAL_BYTES) {
                    throw new IOException("압축 해제 용량이 상한을 초과했습니다.");
                }

                if (isClass) {
                    classCount++;
                    int major = classMajor(data);
                    if (major > maxMajor) {
                        maxMajor = major;
                    }
                    if (!usesJavax && indexOf(data, JAVAX) >= 0) usesJavax = true;
                    if (!usesJakarta && indexOf(data, JAKARTA) >= 0) usesJakarta = true;
                } else if (isLib) {
                    String base = name.substring(name.lastIndexOf('/') + 1);
                    if (base.contains("jakarta.servlet")) usesJakarta = true;
                    if (base.contains("javax.servlet")) usesJavax = true;
                    Matcher m = LIB.matcher(base);
                    if (m.matches()) {
                        libs.putIfAbsent(m.group(1), m.group(2));
                    } else {
                        libs.putIfAbsent(base.replaceAll("\\.jar$", ""), null);
                    }
                } else if (isWebXml) {
                    String xml = new String(data, StandardCharsets.UTF_8);
                    Matcher m = WEBAPP_VERSION.matcher(xml);
                    if (m.find()) {
                        servletSpec = m.group(1);
                    }
                    if (JAKARTA_NS.matcher(xml).find()) {
                        usesJakarta = true;
                    }
                }
            }
        }

        List<WarLib> libraries = new ArrayList<>();
        libs.forEach((n, v) -> libraries.add(new WarLib(n, v, JdbcDrivers.canonical(n))));
        return new WarAppReport(classCount, maxMajor, WarAppReport.javaFromMajor(maxMajor),
                usesJavax, usesJakarta, servletSpec, libraries);
    }

    /** 보관한 바이트(kept)와 실제 읽은 전체 길이(drained). */
    private record Read(byte[] kept, long drained) {
    }

    /** 엔트리를 최대 cap 바이트까지 보관하고 나머지는 드레인한다. */
    private Read readEntry(ZipInputStream zis, byte[] buf, int cap, long totalSoFar) throws IOException {
        ByteArrayOutputStream kept = new ByteArrayOutputStream();
        long drained = 0;
        int n;
        while ((n = zis.read(buf)) != -1) {
            drained += n;
            if (totalSoFar + drained > MAX_TOTAL_BYTES) {
                throw new IOException("압축 해제 용량이 상한을 초과했습니다.");
            }
            int room = cap - kept.size();
            if (room > 0) {
                kept.write(buf, 0, Math.min(n, room));
            }
        }
        return new Read(kept.toByteArray(), drained);
    }

    private int classMajor(byte[] data) {
        if (data.length < 8) {
            return 0;
        }
        // 0xCAFEBABE, minor(2), major(2)
        if ((data[0] & 0xFF) != 0xCA || (data[1] & 0xFF) != 0xFE
                || (data[2] & 0xFF) != 0xBA || (data[3] & 0xFF) != 0xBE) {
            return 0;
        }
        return ((data[6] & 0xFF) << 8) | (data[7] & 0xFF);
    }

    private int indexOf(byte[] haystack, byte[] needle) {
        if (needle.length == 0 || haystack.length < needle.length) {
            return -1;
        }
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
