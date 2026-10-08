package com.moveguard.warscan;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JdbcDriversTest {

    @Test
    @DisplayName("알려진 드라이버는 매트릭스 제품명으로 정규화")
    void recognizes() {
        assertThat(JdbcDrivers.canonical("mysql-connector-j")).isEqualTo("mysql-connector-j");
        assertThat(JdbcDrivers.canonical("mysql-connector-java")).isEqualTo("mysql-connector-j");
        assertThat(JdbcDrivers.canonical("mariadb-java-client")).isEqualTo("mariadb-java-client");
        assertThat(JdbcDrivers.canonical("postgresql")).isEqualTo("postgresql");
        assertThat(JdbcDrivers.canonical("ojdbc8")).isEqualTo("ojdbc8");
        assertThat(JdbcDrivers.canonical("ojdbc11")).isEqualTo("ojdbc11");
    }

    @Test
    @DisplayName("드라이버가 아니면 null")
    void ignoresNonDrivers() {
        assertThat(JdbcDrivers.canonical("spring-web")).isNull();
        assertThat(JdbcDrivers.canonical("jackson-databind")).isNull();
        assertThat(JdbcDrivers.canonical(null)).isNull();
    }
}
