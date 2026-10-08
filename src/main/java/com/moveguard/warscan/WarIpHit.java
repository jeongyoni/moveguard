package com.moveguard.warscan;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** WAR/JAR 설정 파일에서 찾은 하드코딩 IP 1건. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WarIpHit {

    private String entry;      // 압축 내부 경로 (예: WEB-INF/classes/application.properties)
    private int line;          // 발견 줄 번호 (1부터)
    private String ip;         // 하드코딩된 IPv4
    private Integer port;      // 함께 적힌 포트 (없으면 null)
    private boolean jdbc;      // JDBC URL 안에서 발견되었는지
    private boolean publicIp;  // 공인 IP 여부
    private String snippet;    // 해당 줄(앞뒤 공백 제거, 길이 제한)

    /** 확인 폼 체크박스 값으로 쓰는 직렬화 토큰 (ip \t port \t jdbc \t entry). */
    public String getToken() {
        return ip + "\t" + (port == null ? "" : port) + "\t" + jdbc + "\t" + entry;
    }
}
