package com.moveguard.compat;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CompatReleaseFactoryTest {

    private final ObjectMapper om = new ObjectMapper();
    private final LocalDate today = LocalDate.of(2026, 9, 29);

    private JsonNode cycle(String json) throws Exception {
        return om.readTree(json);
    }

    @Test
    @DisplayName("eol이 boolean false면 지원 중, eol_date 없음")
    void eolBooleanFalse() throws Exception {
        CompatRelease r = CompatReleaseFactory.fromCycle("tomcat",
                cycle("{\"cycle\":\"11.0\",\"eol\":false,\"minJavaVersion\":\"17\",\"latest\":\"11.0.26\"}"), today);

        assertThat(r.getVersion()).isEqualTo("11.0");
        assertThat(r.isEol()).isFalse();
        assertThat(r.isMaintained()).isTrue();
        assertThat(r.getEolDate()).isNull();
        assertThat(r.getMinJavaVersion()).isEqualTo("17");
        assertThat(r.getLatestVersion()).isEqualTo("11.0.26");
    }

    @Test
    @DisplayName("eol이 과거 날짜면 이미 EOL, eol_date 채워짐")
    void eolPastDate() throws Exception {
        CompatRelease r = CompatReleaseFactory.fromCycle("mysql",
                cycle("{\"cycle\":\"5.7\",\"eol\":\"2023-10-31\"}"), today);

        assertThat(r.isEol()).isTrue();
        assertThat(r.isMaintained()).isFalse();
        assertThat(r.getEolDate()).isEqualTo(LocalDate.of(2023, 10, 31));
    }

    @Test
    @DisplayName("eol이 미래 날짜면 아직 지원 중이지만 eol_date는 채워짐")
    void eolFutureDate() throws Exception {
        CompatRelease r = CompatReleaseFactory.fromCycle("tomcat",
                cycle("{\"cycle\":\"9.0\",\"eol\":\"2027-03-31\"}"), today);

        assertThat(r.isEol()).isFalse();
        assertThat(r.getEolDate()).isEqualTo(LocalDate.of(2027, 3, 31));
    }

    @Test
    @DisplayName("lts가 날짜 문자열이면 LTS로 간주하고 라벨에 표기")
    void ltsAsDate() throws Exception {
        CompatRelease r = CompatReleaseFactory.fromCycle("java",
                cycle("{\"cycle\":\"21\",\"lts\":\"2023-09-19\",\"eol\":false}"), today);

        assertThat(r.isLts()).isTrue();
        assertThat(r.getLabel()).contains("(LTS)");
    }

    @Test
    @DisplayName("extendedSupport 날짜(oracle 등)를 ext_support_date로 파싱")
    void extendedSupportDate() throws Exception {
        CompatRelease r = CompatReleaseFactory.fromCycle("oracle-database",
                cycle("{\"cycle\":\"19\",\"lts\":true,\"eol\":\"2029-12-31\",\"extendedSupport\":\"2032-12-31\"}"), today);

        assertThat(r.isLts()).isTrue();
        assertThat(r.getExtSupportDate()).isEqualTo(LocalDate.of(2032, 12, 31));
    }
}
