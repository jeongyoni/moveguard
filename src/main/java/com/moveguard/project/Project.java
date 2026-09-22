package com.moveguard.project;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class Project {

    private Long projectId;
    private String name;
    private String customerName;
    private String sourceEnv;
    private String targetEnv;
    private String status;
    private LocalDate plannedDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
