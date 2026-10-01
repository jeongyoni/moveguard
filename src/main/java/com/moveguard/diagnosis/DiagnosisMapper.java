package com.moveguard.diagnosis;

import com.moveguard.asset.Asset;
import com.moveguard.asset.AssetIp;
import com.moveguard.asset.AssetSoftware;
import com.moveguard.asset.BackupPlan;
import com.moveguard.asset.Certificate;
import com.moveguard.asset.Dependency;
import com.moveguard.asset.DnsRecord;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DiagnosisMapper {

    List<Asset> findAssets(Long projectId);

    List<AssetIp> findAssetIps(Long projectId);

    List<Dependency> findDependencies(Long projectId);

    List<DnsRecord> findDnsRecords(Long projectId);

    List<Certificate> findCertificates(Long projectId);

    List<AssetSoftware> findAssetSoftware(Long projectId);

    List<BackupPlan> findBackupPlans(Long projectId);

    LocalDate findPlannedDate(Long projectId);

    List<RuleDefinition> findEnabledRules();

    void insertRun(DiagnosisRun run);

    void insertFindings(@Param("findings") List<FindingRecord> findings);
}
