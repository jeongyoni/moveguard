package com.moveguard.project;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 상세 화면의 편집용 백업 행 (자산명 포함). */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BackupView {

    private Long backupId;
    private Long assetId;
    private String assetName;
    private LocalDate lastBackupAt;
    private boolean restoreTested;
    private boolean offsite;
    private String phase;
}
