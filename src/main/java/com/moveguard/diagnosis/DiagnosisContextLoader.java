package com.moveguard.diagnosis;

import com.moveguard.compat.CompatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DiagnosisContextLoader {

    private final DiagnosisMapper diagnosisMapper;
    private final CompatMapper compatMapper;

    @Transactional(readOnly = true)
    public DiagnosisContext load(Long projectId) {
        return new DiagnosisContext(
                projectId,
                diagnosisMapper.findAssets(projectId),
                diagnosisMapper.findAssetIps(projectId),
                diagnosisMapper.findDependencies(projectId),
                diagnosisMapper.findDnsRecords(projectId),
                diagnosisMapper.findCertificates(projectId),
                diagnosisMapper.findPlannedDate(projectId),
                diagnosisMapper.findAssetSoftware(projectId),
                compatMapper.findAllReleases(),
                diagnosisMapper.findBackupPlans(projectId),
                compatMapper.findDriverRequirements());
    }
}
