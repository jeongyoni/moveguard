package com.moveguard.project;

import com.moveguard.diagnosis.DiagnosisContextLoader;
import com.moveguard.diagnosis.DiagnosisInput;
import com.moveguard.imports.ImportError;
import com.moveguard.imports.ImportValues;
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
 * 웹 폼으로 이전사업을 만들고(2a) 상세에서 항목을 편집한다(2b~). 저장·검증은 1단계 서비스를 재사용한다.
 */
@Controller
@RequiredArgsConstructor
public class ProjectFormController {

    private final ProjectService projectService;
    private final ProjectImportService importService;
    private final ProjectEditMapper editMapper;
    private final AssetEditService assetEditService;
    private final IpEditService ipEditService;
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
        return populateDetail(projectId, model);
    }

    // ----- 자산 CRUD (2b) -----

    @PostMapping("/projects/{projectId}/assets")
    public String addAsset(@PathVariable Long projectId,
                           @RequestParam String name,
                           @RequestParam(required = false) String assetType,
                           @RequestParam(required = false) String role,
                           @RequestParam(required = false) String osName,
                           @RequestParam(required = false) String osVersion,
                           Model model) {
        List<String> errors = assetEditService.add(projectId, name, assetType, role, osName, osVersion);
        if (errors.isEmpty()) {
            return "redirect:/projects/" + projectId;
        }
        model.addAttribute("errors", errors);
        return populateDetail(projectId, model);
    }

    @GetMapping("/projects/{projectId}/assets/{assetId}/edit")
    public String editAssetForm(@PathVariable Long projectId, @PathVariable Long assetId, Model model) {
        AssetDetail asset = editMapper.findAsset(assetId);
        if (asset == null) {
            return "redirect:/projects/" + projectId;
        }
        model.addAttribute("projectId", projectId);
        model.addAttribute("asset", asset);
        return "asset-form";
    }

    @PostMapping("/projects/{projectId}/assets/{assetId}/edit")
    public String editAsset(@PathVariable Long projectId, @PathVariable Long assetId,
                            @RequestParam String name,
                            @RequestParam(required = false) String assetType,
                            @RequestParam(required = false) String role,
                            @RequestParam(required = false) String osName,
                            @RequestParam(required = false) String osVersion,
                            Model model) {
        List<String> errors = assetEditService.update(assetId, name, assetType, role, osName, osVersion);
        if (errors.isEmpty()) {
            return "redirect:/projects/" + projectId;
        }
        model.addAttribute("errors", errors);
        model.addAttribute("projectId", projectId);
        model.addAttribute("asset", new AssetDetail(assetId, projectId, name, assetType, role, osName, osVersion));
        return "asset-form";
    }

    @PostMapping("/projects/{projectId}/assets/{assetId}/delete")
    public String deleteAsset(@PathVariable Long projectId, @PathVariable Long assetId) {
        assetEditService.delete(assetId);
        return "redirect:/projects/" + projectId;
    }

    // ----- IP CRUD (2c) -----

    @PostMapping("/projects/{projectId}/ips")
    public String addIp(@PathVariable Long projectId,
                        @RequestParam(required = false) Long assetId,
                        @RequestParam String address,
                        @RequestParam(required = false) String ipType,
                        @RequestParam(required = false) String phase,
                        @RequestParam(required = false) String extWhitelisted,
                        @RequestParam(required = false) String note,
                        Model model) {
        List<String> errors = ipEditService.add(projectId, assetId, address, ipType, phase,
                extWhitelisted, note);
        if (errors.isEmpty()) {
            return "redirect:/projects/" + projectId;
        }
        model.addAttribute("errors", errors);
        return populateDetail(projectId, model);
    }

    @GetMapping("/projects/{projectId}/ips/{ipId}/edit")
    public String editIpForm(@PathVariable Long projectId, @PathVariable Long ipId, Model model) {
        IpView ip = editMapper.findIp(ipId);
        if (ip == null) {
            return "redirect:/projects/" + projectId;
        }
        model.addAttribute("projectId", projectId);
        model.addAttribute("ip", ip);
        model.addAttribute("assets", editMapper.findAssets(projectId));
        return "ip-form";
    }

    @PostMapping("/projects/{projectId}/ips/{ipId}/edit")
    public String editIp(@PathVariable Long projectId, @PathVariable Long ipId,
                         @RequestParam(required = false) Long assetId,
                         @RequestParam String address,
                         @RequestParam(required = false) String ipType,
                         @RequestParam(required = false) String phase,
                         @RequestParam(required = false) String extWhitelisted,
                         @RequestParam(required = false) String note,
                         Model model) {
        List<String> errors = ipEditService.update(ipId, assetId, address, ipType, phase,
                extWhitelisted, note);
        if (errors.isEmpty()) {
            return "redirect:/projects/" + projectId;
        }
        model.addAttribute("errors", errors);
        model.addAttribute("projectId", projectId);
        model.addAttribute("ip", new IpView(ipId, assetId, null, address, ipType, phase,
                ImportValues.toBool(extWhitelisted), note));
        model.addAttribute("assets", editMapper.findAssets(projectId));
        return "ip-form";
    }

    @PostMapping("/projects/{projectId}/ips/{ipId}/delete")
    public String deleteIp(@PathVariable Long projectId, @PathVariable Long ipId) {
        ipEditService.delete(ipId);
        return "redirect:/projects/" + projectId;
    }

    private String populateDetail(Long projectId, Model model) {
        Project project = projectService.findProject(projectId).orElse(null);
        if (project == null) {
            return "redirect:/";
        }
        model.addAttribute("project", project);
        model.addAttribute("assets", editMapper.findAssets(projectId));
        model.addAttribute("ips", editMapper.findIps(projectId));
        model.addAttribute("input", DiagnosisInput.of(contextLoader.load(projectId)));
        return "project-detail";
    }
}
