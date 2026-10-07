package com.moveguard.project;

import com.moveguard.diagnosis.DiagnosisContextLoader;
import com.moveguard.diagnosis.DiagnosisInput;
import com.moveguard.imports.ImportError;
import com.moveguard.imports.ImportValidationException;
import com.moveguard.imports.ProjectImport;
import com.moveguard.imports.ProjectImport.ProjectRow;
import com.moveguard.imports.ProjectImportService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 웹 폼으로 이전사업을 만들고(2a) 상세를 본다. 저장·검증은 1단계 ProjectImportService를 재사용한다.
 * 항목 추가/수정/삭제는 후속 슬라이스에서 이 상세 화면에 붙인다.
 */
@Controller
@RequiredArgsConstructor
public class ProjectFormController {

    private final ProjectService projectService;
    private final ProjectImportService importService;
    private final DiagnosisContextLoader contextLoader;

    @GetMapping("/projects/new")
    public String newForm() {
        return "project-form";
    }

    @PostMapping("/projects")
    public String create(@RequestParam String name,
                         @RequestParam(required = false) String customerName,
                         @RequestParam(required = false) String sourceEnv,
                         @RequestParam(required = false) String targetEnv,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false) String plannedDate,
                         Model model) {
        ProjectRow row = new ProjectRow(1, name, customerName, sourceEnv, targetEnv, status, plannedDate);
        ProjectImport input = new ProjectImport(row, List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());
        try {
            long projectId = importService.importProject(input);
            return "redirect:/projects/" + projectId;
        } catch (ImportValidationException e) {
            model.addAttribute("errors", e.getErrors().stream().map(ImportError::describe).toList());
            model.addAttribute("name", name);
            model.addAttribute("customerName", customerName);
            model.addAttribute("sourceEnv", sourceEnv);
            model.addAttribute("targetEnv", targetEnv);
            model.addAttribute("status", status);
            model.addAttribute("plannedDate", plannedDate);
            return "project-form";
        }
    }

    @GetMapping("/projects/{projectId}")
    public String detail(@PathVariable Long projectId, Model model) {
        Project project = projectService.findProject(projectId).orElse(null);
        if (project == null) {
            return "redirect:/";
        }
        model.addAttribute("project", project);
        model.addAttribute("input", DiagnosisInput.of(contextLoader.load(projectId)));
        return "project-detail";
    }
}
