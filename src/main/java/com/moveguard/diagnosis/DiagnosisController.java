package com.moveguard.diagnosis;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/diagnoses")
@RequiredArgsConstructor
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    @PostMapping
    public ResponseEntity<DiagnosisResult> diagnose(@PathVariable Long projectId) {
        return diagnosisService.diagnose(projectId)
                .map(result -> ResponseEntity.status(HttpStatus.CREATED).body(result))
                .orElse(ResponseEntity.notFound().build());
    }
}
