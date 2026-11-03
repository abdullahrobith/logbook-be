package com.logbook.project;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository repository;

    public ProjectController(ProjectRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getProjects() {
        // Sementara kita pakai ID 1 dulu buat ngetes (nanti diganti dengan ID dari Login)
        Long currentUserId = 1L; 
        
        List<ProjectDto> projects = repository.findProjectsByUserId(currentUserId);
        return ResponseEntity.ok(projects);
    }
}