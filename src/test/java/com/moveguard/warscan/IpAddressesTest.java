package com.moveguard.warscan;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IpAddressesTest {

    @Test
    @DisplayName("공인 IPv4 판별")
    void publicIps() {
        assertThat(IpAddresses.isPublicIpv4("52.79.11.22")).isTrue();
        assertThat(IpAddresses.isPublicIpv4("8.8.8.8")).isTrue();
        assertThat(IpAddresses.isPublicIpv4("1.2.3.4")).isTrue();
    }

    @Test
    @DisplayName("사설·예약 대역은 공인 아님")
    void privateOrReserved() {
        assertThat(IpAddresses.isPublicIpv4("10.0.1.5")).isFalse();     // 10/8
        assertThat(IpAddresses.isPublicIpv4("172.16.0.1")).isFalse();   // 172.16/12
        assertThat(IpAddresses.isPublicIpv4("172.31.255.1")).isFalse();
        assertThat(IpAddresses.isPublicIpv4("192.168.0.1")).isFalse();  // 192.168/16
        assertThat(IpAddresses.isPublicIpv4("127.0.0.1")).isFalse();    // 루프백
        assertThat(IpAddresses.isPublicIpv4("169.254.1.1")).isFalse();  // 링크로컬
        assertThat(IpAddresses.isPublicIpv4("100.64.0.1")).isFalse();   // CGNAT
        assertThat(IpAddresses.isPublicIpv4("224.0.0.1")).isFalse();    // 멀티캐스트
    }

    @Test
    @DisplayName("172.32.x는 사설이 아니라 공인")
    void justOutsidePrivate() {
        assertThat(IpAddresses.isPublicIpv4("172.32.0.1")).isTrue();
        assertThat(IpAddresses.isPublicIpv4("172.15.0.1")).isTrue();
    }

    @Test
    @DisplayName("잘못된 형식은 리터럴·공인 모두 false")
    void invalid() {
        assertThat(IpAddresses.isIpv4Literal("999.1.1.1")).isFalse();
        assertThat(IpAddresses.isIpv4Literal("1.2.3")).isFalse();
        assertThat(IpAddresses.isIpv4Literal("db.example.com")).isFalse();
        assertThat(IpAddresses.isPublicIpv4("999.1.1.1")).isFalse();
    }
}
