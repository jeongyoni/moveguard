package com.moveguard.imports;

import java.util.List;

/** 시트 이름과 열 순서 정의 — 리더(ExcelProjectReader)와 양식 생성(ImportTemplateWriter)이 공유한다. */
final class ImportSheets {

    record Sheet(String name, List<String> headers) {
    }

    static final Sheet PROJECT = new Sheet("사업",
            List.of("사업명", "고객사", "기존환경", "신규환경", "상태(PLAN/READY/BLOCKED/DONE)", "전환예정일(yyyy-MM-dd)"));
    static final Sheet ASSET = new Sheet("자산",
            List.of("자산명", "유형(SERVER/EXTERNAL)", "역할(WEB/WAS/DB/ETC)", "OS이름", "OS버전"));
    static final Sheet IP = new Sheet("IP",
            List.of("자산명", "주소", "종류(PUBLIC/PRIVATE)", "단계(BEFORE/AFTER)", "외부허용목록(Y/N)", "비고"));
    static final Sheet DEPENDENCY = new Sheet("의존관계",
            List.of("출발자산", "대상자산(없으면 비움)", "접속주소", "포트", "프로토콜", "설정위치"));
    static final Sheet DNS = new Sheet("DNS",
            List.of("도메인", "레코드종류(A/AAAA/CNAME/MX/TXT)", "값", "TTL", "단계(BEFORE/AFTER)"));
    static final Sheet CERT = new Sheet("인증서",
            List.of("도메인", "발급자", "만료일(yyyy-MM-dd)", "단계(BEFORE/AFTER)"));
    static final Sheet SOFTWARE = new Sheet("소프트웨어",
            List.of("자산명", "제품", "역할", "버전", "릴리스라인", "단계(BEFORE/AFTER)"));
    static final Sheet BACKUP = new Sheet("백업",
            List.of("자산명", "마지막백업일(yyyy-MM-dd)", "복구테스트(Y/N)", "오프사이트(Y/N)", "단계(BEFORE/AFTER)"));

    static final List<Sheet> ALL =
            List.of(PROJECT, ASSET, IP, DEPENDENCY, DNS, CERT, SOFTWARE, BACKUP);

    private ImportSheets() {
    }
}
