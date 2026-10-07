package com.moveguard.imports;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 리더 + 검증기 단위 테스트. 양식 생성물(POI)을 메모리에서 읽어 검증한다(앱·DB 불필요). */
class ExcelProjectImportTest {

    private final ImportTemplateWriter templateWriter = new ImportTemplateWriter();
    private final ExcelProjectReader reader = new ExcelProjectReader();
    private final ProjectImportValidator validator = new ProjectImportValidator();

    @Test
    @DisplayName("제공 양식(샘플 데이터)은 그대로 읽히고 검증을 통과한다")
    void sampleTemplateIsValid() throws Exception {
        ProjectImport in = reader.read(new ByteArrayInputStream(templateWriter.build()));

        assertThat(in.project().name()).isEqualTo("쇼핑몰 클라우드 이전");
        assertThat(in.assets()).hasSize(3);
        assertThat(in.ips()).hasSize(4);
        assertThat(in.software()).hasSize(4);
        assertThat(validator.validate(in)).isEmpty();
    }

    @Test
    @DisplayName("IP 형식 오류·없는 자산 참조를 시트·행·이유로 보고한다")
    void reportsErrorsWithSheetAndRow() throws Exception {
        byte[] xlsx = buildBroken();
        ProjectImport in = reader.read(new ByteArrayInputStream(xlsx));

        List<ImportError> errors = validator.validate(in);
        List<String> described = errors.stream().map(ImportError::describe).toList();

        // IP 시트 2행: 잘못된 주소 'not-an-ip'
        assertThat(described).anyMatch(s -> s.startsWith("IP 시트 2행") && s.contains("IP 형식 오류"));
        // IP 시트 3행: 자산 시트에 없는 자산 참조
        assertThat(described).anyMatch(s -> s.startsWith("IP 시트 3행") && s.contains("없는 자산명"));
    }

    @Test
    @DisplayName("오류가 하나라도 있으면 전부(목록)를 반환한다")
    void collectsAllErrors() throws Exception {
        ProjectImport in = reader.read(new ByteArrayInputStream(buildBroken()));
        assertThat(validator.validate(in)).isNotEmpty();
    }

    /** 사업·자산 1개(web01) + IP 2행(형식오류, 없는 자산참조) 짜리 깨진 워크북 */
    private byte[] buildBroken() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            header(wb, ImportSheets.PROJECT);
            row(wb, "사업", "테스트사업", "고객사", "", "", "", "");
            header(wb, ImportSheets.ASSET);
            row(wb, "자산", "web01", "SERVER", "WEB", "", "");
            header(wb, ImportSheets.IP);
            row(wb, "IP", "web01", "not-an-ip", "PUBLIC", "BEFORE", "N", "");     // 2행: 형식 오류
            row(wb, "IP", "nope", "10.0.0.1", "PRIVATE", "BEFORE", "N", "");       // 3행: 없는 자산
            // 나머지 시트는 헤더만
            header(wb, ImportSheets.DEPENDENCY);
            header(wb, ImportSheets.DNS);
            header(wb, ImportSheets.CERT);
            header(wb, ImportSheets.SOFTWARE);
            header(wb, ImportSheets.BACKUP);
            wb.write(out);
            return out.toByteArray();
        }
    }

    private void header(XSSFWorkbook wb, ImportSheets.Sheet def) {
        Sheet sheet = wb.createSheet(def.name());
        Row row = sheet.createRow(0);
        for (int i = 0; i < def.headers().size(); i++) {
            row.createCell(i).setCellValue(def.headers().get(i));
        }
    }

    private void row(XSSFWorkbook wb, String sheetName, String... values) {
        Sheet sheet = wb.getSheet(sheetName);
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }
}
