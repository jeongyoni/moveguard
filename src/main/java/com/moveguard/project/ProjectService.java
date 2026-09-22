package com.moveguard.project;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectMapper projectMapper;

    public Optional<Project> findProject(Long projectId) {
        return projectMapper.findById(projectId);
    }

    public List<Project> findProjects() {
        return projectMapper.findAll();
    }
}
