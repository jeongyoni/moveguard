package com.moveguard.imports;

import com.moveguard.imports.ImportSheets.Sheet;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/** 샘플 데이터가 채워진 업로드 양식(.xlsx)을 생성한다. 시트·열 순서는 ImportSheets·리더와 일치. */
@Component
public class ImportTemplateWriter {

    public byte[] build() {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle header = headerStyle(wb);
            for (Sheet def : ImportSheets.ALL) {
                XSSFSheet sheet = wb.createSheet(def.name());
                writeHeader(sheet, def.headers(), header);
            }
            fillSamples(wb);
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                autoSize(wb.getSheetAt(i), ImportSheets.ALL.get(i).headers().size());
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("양식 생성 실패", e);
        }
    }

    private void writeHeader(XSSFSheet sheet, List<String> headers, CellStyle style) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            row.createCell(i).setCellValue(headers.get(i));
            row.getCell(i).setCellStyle(style);
        }
    }

    /** 더미 쇼핑몰 이전사업을 예시로 채운다 — 사용자가 형식을 바로 이해하도록 */
    private void fillSamples(XSSFWorkbook wb) {
        sample(wb, "사업", new String[][]{
                {"쇼핑몰 클라우드 이전", "가나다커머스", "IDC-A", "AWS ap-northeast-2", "PLAN", "2026-10-24"}});
        sample(wb, "자산", new String[][]{
                {"web01", "SERVER", "WEB", "Rocky Linux", "8.9"},
                {"db01", "SERVER", "DB", "Rocky Linux", "8.9"},
                {"pg-api", "EXTERNAL", "", "", ""}});
        sample(wb, "IP", new String[][]{
                {"web01", "203.0.113.11", "PUBLIC", "BEFORE", "Y", "PG사 허용목록 등록"},
                {"web01", "198.51.100.21", "PUBLIC", "AFTER", "N", "EIP 예정"},
                {"db01", "203.0.113.21", "PUBLIC", "BEFORE", "N", ""},
                {"db01", "172.31.20.21", "PRIVATE", "AFTER", "N", ""}});
        sample(wb, "의존관계", new String[][]{
                {"web01", "db01", "203.0.113.21", "3306", "JDBC", "application.yml spring.datasource.url"},
                {"web01", "pg-api", "api.pg-example.com", "443", "HTTPS", "application.yml pg.api.base-url"}});
        sample(wb, "DNS", new String[][]{
                {"shop.example.com", "A", "203.0.113.11", "3600", "BEFORE"},
                {"shop.example.com", "A", "198.51.100.21", "300", "AFTER"}});
        sample(wb, "인증서", new String[][]{
                {"shop.example.com", "Lets Encrypt R3", "2026-11-10", "BEFORE"},
                {"shop.example.com", "Lets Encrypt R3", "2027-11-10", "AFTER"}});
        sample(wb, "소프트웨어", new String[][]{
                {"web01", "tomcat", "WAS", "9.0.80", "9.0", "BEFORE"},
                {"web01", "tomcat", "WAS", "11.0.22", "11.0", "AFTER"},
                {"db01", "mysql", "DB", "5.7.44", "5.7", "BEFORE"},
                {"db01", "mysql", "DB", "9.6.1", "9.6", "AFTER"}});
        sample(wb, "백업", new String[][]{
                {"db01", "2026-10-22", "N", "Y", "BEFORE"}});
    }

    private void sample(XSSFWorkbook wb, String sheetName, String[][] rows) {
        XSSFSheet sheet = wb.getSheet(sheetName);
        for (int r = 0; r < rows.length; r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < rows[r].length; c++) {
                row.createCell(c).setCellValue(rows[r][c]);
            }
        }
    }

    private CellStyle headerStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void autoSize(XSSFSheet sheet, int cols) {
        for (int c = 0; c < cols; c++) {
            sheet.autoSizeColumn(c);
        }
    }
}
