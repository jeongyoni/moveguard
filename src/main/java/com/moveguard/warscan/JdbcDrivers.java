package com.moveguard.warscan;

import java.util.Map;

/** WEB-INF/lib의 jar 이름을 알려진 JDBC 드라이버 제품명(매트릭스 키)으로 정규화한다. */
public final class JdbcDrivers {

    /** 라이브러리 파일명(버전 제거 전 base) 접두 → 매트릭스 driver_product */
    private static final Map<String, String> KNOWN = Map.of(
            "mysql-connector-j", "mysql-connector-j",
            "mysql-connector-java", "mysql-connector-j",
            "mariadb-java-client", "mariadb-java-client",
            "postgresql", "postgresql",
            "mssql-jdbc", "mssql-jdbc"
    );

    private JdbcDrivers() {
    }

    /** 드라이버면 정규화된 제품명, 아니면 null. (ojdbcN은 Oracle 드라이버로 그대로 사용) */
    public static String canonical(String libName) {
        if (libName == null) {
            return null;
        }
        String n = libName.toLowerCase();
        String hit = KNOWN.get(n);
        if (hit != null) {
            return hit;
        }
        // Oracle JDBC: ojdbc6/7/8/10/11/17 등은 파일명이 곧 제품명
        if (n.matches("ojdbc\\d+")) {
            return n;
        }
        return null;
    }
}
