package com.moveguard.imports;

import java.util.List;
import lombok.Getter;

/** 검증 실패 — 저장하지 않았음을 뜻한다. 오류 목록을 화면에 보여준다. */
@Getter
public class ImportValidationException extends RuntimeException {

    private final transient List<ImportError> errors;

    public ImportValidationException(List<ImportError> errors) {
        super("입력 검증 실패: " + errors.size() + "건");
        this.errors = errors;
    }
}
