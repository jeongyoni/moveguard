package com.moveguard.warscan;

import java.util.regex.Pattern;

/** IPv4 리터럴 판별 및 공인/사설(예약) 분류 유틸. */
public final class IpAddresses {

    private static final String OCTET = "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)";
    /** 앞뒤가 숫자·점이 아닌 경계에서 IPv4를 찾는다 (버전 문자열 1.2.3.4 등과의 오탐을 줄이기 위해 호출부에서 문맥 사용) */
    static final Pattern IPV4_TOKEN =
            Pattern.compile("(?<![\\d.])" + OCTET + "(\\." + OCTET + "){3}(?![\\d.])");
    private static final Pattern IPV4_FULL =
            Pattern.compile("^" + OCTET + "(\\." + OCTET + "){3}$");

    private IpAddresses() {
    }

    public static boolean isIpv4Literal(String s) {
        return s != null && IPV4_FULL.matcher(s).matches();
    }

    /**
     * 주소를 PUBLIC / PRIVATE로 자동 분류한다.
     * IPv4는 사설·예약 대역 판정, IPv6는 루프백(::1)·링크로컬(fe80)·유니크로컬(fc/fd)만 PRIVATE.
     * 형식을 알 수 없으면 null.
     */
    public static String classify(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim().toLowerCase();
        if (isIpv4Literal(t)) {
            return isPublicIpv4(t) ? "PUBLIC" : "PRIVATE";
        }
        if (t.contains(":")) { // IPv6
            if (t.equals("::1") || t.startsWith("fe80") || t.startsWith("fc") || t.startsWith("fd")) {
                return "PRIVATE";
            }
            return "PUBLIC";
        }
        return null;
    }

    /** 공인 IPv4인지. 유효한 IPv4이면서 사설·루프백·링크로컬·예약 대역이 아니면 true. */
    public static boolean isPublicIpv4(String s) {
        if (!isIpv4Literal(s)) {
            return false;
        }
        String[] p = s.split("\\.");
        int a = Integer.parseInt(p[0]);
        int b = Integer.parseInt(p[1]);

        if (a == 10) return false;                       // 10.0.0.0/8 사설
        if (a == 172 && b >= 16 && b <= 31) return false; // 172.16.0.0/12 사설
        if (a == 192 && b == 168) return false;          // 192.168.0.0/16 사설
        if (a == 127) return false;                      // 127.0.0.0/8 루프백
        if (a == 169 && b == 254) return false;          // 169.254.0.0/16 링크로컬
        if (a == 100 && b >= 64 && b <= 127) return false; // 100.64.0.0/10 CGNAT
        if (a == 0) return false;                        // 0.0.0.0/8
        if (a >= 224) return false;                      // 멀티캐스트/예약(224+)
        return true;
    }
}
