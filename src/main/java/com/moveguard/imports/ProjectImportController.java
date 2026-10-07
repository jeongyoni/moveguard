package com.moveguard.imports;

import com.moveguard.diagnosis.DiagnosisPage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/** 엑셀로 이전사업을 등록하는 화면 흐름: 양식 다운로드 → 업로드 → 검증 → 저장 → 진단 결과. */
@Controller
@RequiredArgsConstructor
public class ProjectImportController {

    private final ExcelProjectReader reader;
    private final ProjectImportService importService;
    private final ImportTemplateWriter templateWriter;
    private final DiagnosisPage diagnosisPage;

    @GetMapping("/projects/import")
    public String form() {
        return "import";
    }

    @GetMapping("/projects/import/template")
    public ResponseEntity<byte[]> template() {
        byte[] body = templateWriter.build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"moveguard-import-template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    @PostMapping("/projects/import")
    public String upload(@RequestParam("file") MultipartFile file, Model model) {
        if (file == null || file.isEmpty()) {
            model.addAttribute("fileError", "엑셀 파일을 선택하세요.");
            return "import";
        }
        try {
            ProjectImport input = reader.read(file.getInputStream());
            long projectId = importService.importProject(input);
            // 저장 성공 → 바로 진단 결과 화면
            return diagnosisPage.render(projectId, model);
        } catch (ImportValidationException e) {
            model.addAttribute("errors", e.getErrors().stream().map(ImportError::describe).toList());
            return "import";
        } catch (Exception e) {
            model.addAttribute("fileError", "엑셀을 읽을 수 없습니다. 양식 파일인지 확인하세요.");
            return "import";
        }
    }
}
