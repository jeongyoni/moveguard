package com.moveguard.warscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WarConfigScannerTest {

    private final WarConfigScanner scanner = new WarConfigScanner();

    private byte[] war(Map<String, String> entries) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                zos.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return bos.toByteArray();
    }

    private Map<String, WarIpHit> byIp(List<WarIpHit> hits) {
        return hits.stream().collect(Collectors.toMap(WarIpHit::getIp, Function.identity()));
    }

    @Test
    @DisplayName("JDBC URL의 공인 IP를 포트·JDBC 플래그와 함께 탐지")
    void detectsJdbcPublicIp() throws IOException {
        byte[] war = war(Map.of(
                "WEB-INF/classes/application.properties",
                "spring.datasource.url=jdbc:mysql://52.79.11.22:3306/shop\nspring.datasource.username=app\n"));

        List<WarIpHit> hits = scanner.scan(new ByteArrayInputStream(war));

        assertThat(hits).hasSize(1);
        WarIpHit h = hits.get(0);
        assertThat(h.getIp()).isEqualTo("52.79.11.22");
        assertThat(h.getPort()).isEqualTo(3306);
        assertThat(h.isJdbc()).isTrue();
        assertThat(h.isPublicIp()).isTrue();
        assertThat(h.getEntry()).isEqualTo("WEB-INF/classes/application.properties");
        assertThat(h.getLine()).isEqualTo(1);
    }

    @Test
    @DisplayName("사설 IP도 탐지하되 공인/사설을 구분한다")
    void classifiesPrivate() throws IOException {
        byte[] war = war(Map.of(
                "application.yml", "redis:\n  host: 10.0.1.5\n  port: 6379\n"));

        List<WarIpHit> hits = scanner.scan(new ByteArrayInputStream(war));

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getIp()).isEqualTo("10.0.1.5");
        assertThat(hits.get(0).isPublicIp()).isFalse();
        assertThat(hits.get(0).isJdbc()).isFalse();
    }

    @Test
    @DisplayName("설정 파일이 아닌 항목(.jar/.class)은 스캔하지 않는다")
    void ignoresNonConfigEntries() throws IOException {
        byte[] war = war(Map.of(
                "WEB-INF/lib/driver.jar", "host=198.51.100.9\n",
                "WEB-INF/classes/com/app/Main.class", "connect 8.8.4.4\n"));

        assertThat(scanner.scan(new ByteArrayInputStream(war))).isEmpty();
    }

    @Test
    @DisplayName("여러 파일·IP를 모으고, 같은 파일 내 중복 IP는 1건으로 합친다")
    void collectsAndDedupes() throws IOException {
        byte[] war = war(Map.of(
                "WEB-INF/classes/application.properties",
                "db.url=jdbc:oracle:thin:@1.2.3.4:1521:ORCL\nbackup.url=jdbc:oracle:thin:@1.2.3.4:1521:ORCL\n",
                "WEB-INF/classes/app.conf",
                "cache=192.168.0.10\n"));

        List<WarIpHit> hits = scanner.scan(new ByteArrayInputStream(war));

        assertThat(hits).hasSize(2);
        Map<String, WarIpHit> m = byIp(hits);
        assertThat(m).containsKeys("1.2.3.4", "192.168.0.10");
        assertThat(m.get("1.2.3.4").getPort()).isEqualTo(1521);
        // 공인 IP가 먼저 정렬된다
        assertThat(hits.get(0).isPublicIp()).isTrue();
    }
}
