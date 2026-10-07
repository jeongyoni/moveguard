package com.moveguard.imports;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;

/** 입력 문자열 파싱·판정 공통 로직. 검증기와 저장 서비스가 같은 규칙을 쓰도록 한곳에 둔다. */
public final class ImportValues {

    private static final String OCTET = "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)";
    private static final Pattern IPV4 = Pattern.compile("^" + OCTET + "(\\." + OCTET + "){3}$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:]+:[0-9a-fA-F:.]+$");
    private static final Set<String> TRUE = Set.of("1", "true", "y", "yes", "o");
    private static final Set<String> FALSE = Set.of("0", "false", "n", "no", "x");

    private ImportValues() {
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    public static boolean isIp(String s) {
        return s != null && (IPV4.matcher(s.trim()).matches() || IPV6.matcher(s.trim()).matches());
    }

    public static boolean isBool(String s) {
        String v = trim(s).toLowerCase();
        return TRUE.contains(v) || FALSE.contains(v);
    }

    public static boolean toBool(String s) {
        return TRUE.contains(trim(s).toLowerCase());
    }

    public static Integer toIntOrNull(String s) {
        try {
            return Integer.valueOf(trim(s));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static LocalDate toDateOrNull(String s) {
        try {
            return LocalDate.parse(trim(s));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
