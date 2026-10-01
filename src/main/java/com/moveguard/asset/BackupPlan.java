package com.moveguard.asset;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 자산별 백업 계획 (이전 전/후) */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BackupPlan {

    private Long backupId;
    private Long assetId;
    private LocalDate lastBackupAt;
    private boolean restoreTested;
    private boolean offsite;
    private Phase phase;
}
