package com.moveguard.diagnosis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DiagnosisContextLoader {

    private final DiagnosisMapper diagnosisMapper;

    @Transactional(readOnly = true)
    public DiagnosisContext load(Long projectId) {
        return new DiagnosisContext(
                projectId,
                diagnosisMapper.findAssets(projectId),
                diagnosisMapper.findAssetIps(projectId),
                diagnosisMapper.findDependencies(projectId));
    }
}
