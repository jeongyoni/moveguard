package com.moveguard.warscan;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.stereotype.Service;

/**
 * WAR/JAR(ZIP) 안의 텍스트 설정 파일을 메모리 스트림으로 읽어 하드코딩된 IPv4를 찾는다.
 * 디스크에 풀지 않으므로 Zip Slip이 없고, 총 해제 용량·엔트리 수 상한으로 Zip Bomb을 방어한다.
 * 중첩 JAR(WEB-INF/lib/*.jar)은 재귀하지 않는다.
 */
@Service
public class WarConfigScanner {

    private static final long MAX_TOTAL_BYTES = 200L * 1024 * 1024; // 총 해제 용량 상한
    private static final int MAX_ENTRIES = 10_000;                  // 엔트리 수 상한
    private static final int MAX_SCAN_BYTES = 4 * 1024 * 1024;      // 설정 파일 1개 스캔 상한
    private static final int MAX_SNIPPET = 200;

    private static final Set<String> CONFIG_EXT =
            Set.of("properties", "yml", "yaml", "xml", "conf", "cfg", "ini", "env");

    /** ip 바로 뒤의 ":포트" */
    private static final Pattern PORT_AFTER = Pattern.compile("^:(\\d{1,5})");

    public List<WarIpHit> scan(InputStream war) throws IOException {
        Map<String, WarIpHit> byKey = new LinkedHashMap<>(); // (entry|ip) 중복 제거, 발견 순서 유지
        long total = 0;
        int entries = 0;

        try (ZipInputStream zis = new ZipInputStream(war)) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                if (++entries > MAX_ENTRIES) {
                    throw new IOException("압축 파일의 항목 수가 너무 많습니다.");
                }
                boolean config = !entry.isDirectory() && isConfigFile(entry.getName());

                // 설정 파일은 스캔용으로 최대 MAX_SCAN_BYTES까지 보관, 그 외에는 버린다.
                // 어떤 경우든 전체를 읽어 총 해제 용량을 집계·제한한다(Zip Bomb 방어).
                ByteArrayOutputStream kept = config ? new ByteArrayOutputStream() : null;
                int n;
                while ((n = zis.read(buf)) != -1) {
                    total += n;
                    if (total > MAX_TOTAL_BYTES) {
                        throw new IOException("압축 해제 용량이 상한을 초과했습니다.");
                    }
                    if (kept != null && kept.size() < MAX_SCAN_BYTES) {
                        kept.write(buf, 0, Math.min(n, MAX_SCAN_BYTES - kept.size()));
                    }
                }
                if (kept != null) {
                    scanText(entry.getName(), kept.toString(StandardCharsets.UTF_8), byKey);
                }
            }
        }

        List<WarIpHit> hits = new ArrayList<>(byKey.values());
        // 공인 IP 먼저, 그다음 파일·줄 순
        hits.sort((x, y) -> {
            if (x.isPublicIp() != y.isPublicIp()) {
                return x.isPublicIp() ? -1 : 1;
            }
            int c = x.getEntry().compareTo(y.getEntry());
            return c != 0 ? c : Integer.compare(x.getLine(), y.getLine());
        });
        return hits;
    }

    private void scanText(String entryName, String content, Map<String, WarIpHit> byKey) {
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            boolean jdbc = line.contains("jdbc:");
            Matcher m = IpAddresses.IPV4_TOKEN.matcher(line);
            while (m.find()) {
                String ip = m.group();
                String key = entryName + "|" + ip;
                if (byKey.containsKey(key)) {
                    continue;
                }
                Integer port = portAfter(line, m.end());
                byKey.put(key, new WarIpHit(
                        entryName, i + 1, ip, port, jdbc,
                        IpAddresses.isPublicIpv4(ip), snippet(line)));
            }
        }
    }

    private Integer portAfter(String line, int ipEnd) {
        Matcher pm = PORT_AFTER.matcher(line.substring(ipEnd));
        if (pm.find()) {
            int port = Integer.parseInt(pm.group(1));
            if (port >= 1 && port <= 65535) {
                return port;
            }
        }
        return null;
    }

    private String snippet(String line) {
        String s = line.strip();
        return s.length() > MAX_SNIPPET ? s.substring(0, MAX_SNIPPET) + "…" : s;
    }

    static boolean isConfigFile(String name) {
        int slash = name.lastIndexOf('/');
        String base = slash >= 0 ? name.substring(slash + 1) : name;
        int dot = base.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        return CONFIG_EXT.contains(base.substring(dot + 1).toLowerCase());
    }
}
