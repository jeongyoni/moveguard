package com.moveguard.imports;

import com.moveguard.imports.ProjectImport.AssetRow;
import com.moveguard.imports.ProjectImport.BackupRow;
import com.moveguard.imports.ProjectImport.CertRow;
import com.moveguard.imports.ProjectImport.DependencyRow;
import com.moveguard.imports.ProjectImport.DnsRow;
import com.moveguard.imports.ProjectImport.IpRow;
import com.moveguard.imports.ProjectImport.ProjectRow;
import com.moveguard.imports.ProjectImport.SoftwareRow;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/** 업로드된 .xlsx를 ProjectImport(원시 문자열 + 행 번호)로 읽는다. 검증은 하지 않는다. */
@Component
public class ExcelProjectReader {

    public ProjectImport read(InputStream in) throws IOException {
        try (Workbook wb = new XSSFWorkbook(in)) {
            ProjectRow project = firstRow(wb, ImportSheets.PROJECT.name(), (c, r) ->
                    new ProjectRow(r, s(c, 0), s(c, 1), s(c, 2), s(c, 3), s(c, 4), s(c, 5)));

            List<AssetRow> assets = rows(wb, ImportSheets.ASSET.name(), (c, r) ->
                    new AssetRow(r, s(c, 0), s(c, 1), s(c, 2), s(c, 3), s(c, 4)));
            List<IpRow> ips = rows(wb, ImportSheets.IP.name(), (c, r) ->
                    new IpRow(r, s(c, 0), s(c, 1), s(c, 2), s(c, 3), s(c, 4), s(c, 5)));
            List<DependencyRow> deps = rows(wb, ImportSheets.DEPENDENCY.name(), (c, r) ->
                    new DependencyRow(r, s(c, 0), s(c, 1), s(c, 2), s(c, 3), s(c, 4), s(c, 5)));
            List<DnsRow> dns = rows(wb, ImportSheets.DNS.name(), (c, r) ->
                    new DnsRow(r, s(c, 0), s(c, 1), s(c, 2), s(c, 3), s(c, 4)));
            List<CertRow> certs = rows(wb, ImportSheets.CERT.name(), (c, r) ->
                    new CertRow(r, s(c, 0), s(c, 1), s(c, 2), s(c, 3)));
            List<SoftwareRow> sw = rows(wb, ImportSheets.SOFTWARE.name(), (c, r) ->
                    new SoftwareRow(r, s(c, 0), s(c, 1), s(c, 2), s(c, 3), s(c, 4), s(c, 5)));
            List<BackupRow> backups = rows(wb, ImportSheets.BACKUP.name(), (c, r) ->
                    new BackupRow(r, s(c, 0), s(c, 1), s(c, 2), s(c, 3), s(c, 4)));

            return new ProjectImport(project, assets, ips, deps, dns, certs, sw, backups);
        }
    }

    /** 헤더(1행) 다음 첫 데이터 행 하나 — 사업 시트용 */
    private <T> T firstRow(Workbook wb, String sheetName, java.util.function.BiFunction<Row, Integer, T> mapper) {
        Sheet sheet = wb.getSheet(sheetName);
        if (sheet == null) {
            return null;
        }
        Row row = sheet.getRow(1);
        return isBlankRow(row) ? null : mapper.apply(row, 2);
    }

    /** 헤더 다음 모든 비어있지 않은 데이터 행 */
    private <T> List<T> rows(Workbook wb, String sheetName,
                             java.util.function.BiFunction<Row, Integer, T> mapper) {
        List<T> result = new ArrayList<>();
        Sheet sheet = wb.getSheet(sheetName);
        if (sheet == null) {
            return result;
        }
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (!isBlankRow(row)) {
                result.add(mapper.apply(row, i + 1)); // 사람이 보는 행 번호(1-based)
            }
        }
        return result;
    }

    private boolean isBlankRow(Row row) {
        if (row == null) {
            return true;
        }
        for (int c = 0; c < row.getLastCellNum(); c++) {
            if (!s(row, c).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** 셀을 문자열로 — 날짜는 yyyy-MM-dd, 정수형 숫자는 소수점 없이 */
    private String s(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case BOOLEAN -> cell.getBooleanCellValue() ? "1" : "0";
            case NUMERIC -> numeric(cell);
            case FORMULA -> formula(cell);
            default -> "";
        };
    }

    private String numeric(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate().toString();
        }
        double d = cell.getNumericCellValue();
        return d == Math.rint(d) && !Double.isInfinite(d)
                ? Long.toString((long) d) : Double.toString(d);
    }

    private String formula(Cell cell) {
        try {
            return cell.getStringCellValue().trim();
        } catch (IllegalStateException e) {
            return numeric(cell);
        }
    }

}
