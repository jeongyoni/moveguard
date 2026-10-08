package com.moveguard.warscan;

import com.moveguard.project.SoftwareEditService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/** WAR/JAR 업로드 → 하드코딩 IP + 애플리케이션 분석 → 확인 → 의존성·소프트웨어 저장 흐름. */
@Controller
@RequiredArgsConstructor
public class WarScanController {

    private final WarConfigScanner scanner;
    private final WarAppAnalyzer analyzer;
    private final WarScanService service;
    private final SoftwareEditService softwareEditService;

    @GetMapping("/projects/{projectId}/war-scan")
    public String form(@PathVariable long projectId, Model model) {
        model.addAttribute("projectId", projectId);
        return "war-scan";
    }

    @PostMapping("/projects/{projectId}/war-scan")
    public String scan(@PathVariable long projectId,
                       @RequestParam("file") MultipartFile file, Model model) {
        model.addAttribute("projectId", projectId);
        if (file == null || file.isEmpty()) {
            model.addAttribute("fileError", "WAR 또는 JAR 파일을 선택하세요.");
            return "war-scan";
        }
        try {
            List<WarIpHit> hits = scanner.scan(file.getInputStream());
            WarAppReport report = analyzer.analyze(file.getInputStream());
            model.addAttribute("scanned", true);
            model.addAttribute("fileName", file.getOriginalFilename());
            model.addAttribute("hits", hits);
            model.addAttribute("report", report);
            model.addAttribute("assets", service.assets(projectId));
        } catch (Exception e) {
            model.addAttribute("fileError", "파일을 읽을 수 없습니다. WAR/JAR(ZIP) 파일인지 확인하세요.");
        }
        return "war-scan";
    }

    @PostMapping("/projects/{projectId}/war-scan/add-java")
    public String addJava(@PathVariable long projectId,
                          @RequestParam(value = "assetId", required = false) Long assetId,
                          @RequestParam("version") String version,
                          @RequestParam("releaseLine") String releaseLine,
                          @RequestParam("phase") String phase, Model model) {
        List<String> errors = softwareEditService.add(projectId, assetId, "java", null,
                version, releaseLine, phase);
        if (!errors.isEmpty()) {
            model.addAttribute("projectId", projectId);
            model.addAttribute("applyError", String.join(" / ", errors));
            return "war-scan";
        }
        return "redirect:/projects/" + projectId;
    }

    @PostMapping("/projects/{projectId}/war-scan/apply")
    public String apply(@PathVariable long projectId,
                        @RequestParam(value = "fromAssetId", required = false) Long fromAssetId,
                        @RequestParam(value = "selected", required = false) List<String> selected,
                        Model model) {
        try {
            int added = service.addDependencies(projectId, fromAssetId, selected);
            if (added == 0) {
                model.addAttribute("projectId", projectId);
                model.addAttribute("applyError", "추가할 항목과 자산을 선택하세요.");
                return "war-scan";
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("projectId", projectId);
            model.addAttribute("applyError", e.getMessage());
            return "war-scan";
        }
        return "redirect:/projects/" + projectId;
    }
}
