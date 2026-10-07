package com.moveguard.imports;

/** 검증 오류 1건: 어느 시트 몇 행에서 왜 실패했는지. */
public record ImportError(String sheet, int row, String reason) {

    /** "자산 시트 3행: IP 형식 오류" 형태 */
    public String describe() {
        return row > 0 ? "%s 시트 %d행: %s".formatted(sheet, row, reason)
                : "%s 시트: %s".formatted(sheet, reason);
    }
}
