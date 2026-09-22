package com.moveguard.project;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectMapper {

    Optional<Project> findById(Long projectId);

    List<Project> findAll();
}
