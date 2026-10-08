package com.moveguard.diagnosis.rule;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VersionsTest {

    @Test
    @DisplayName("세그먼트 단위 숫자 비교")
    void compare() {
        assertThat(Versions.compare("8.0.33", "8.0.11")).isPositive();
        assertThat(Versions.compare("5.1.49", "8.0.11")).isNegative();
        assertThat(Versions.compare("19.3", "19.3.0.0")).isZero();
        assertThat(Versions.compare("8.4.0", "8.4.0")).isZero();
        assertThat(Versions.compare("8.4", "8.10")).isNegative(); // 숫자 비교(4 < 10)
    }

    @Test
    @DisplayName("접두 문자열·접미가 있어도 숫자만 비교")
    void compareWithNoise() {
        assertThat(Versions.compare("v8.0.33", "8.0.11")).isPositive();
        assertThat(Versions.compare("42.6.0-RELEASE", "42.2.0")).isPositive();
    }
}
