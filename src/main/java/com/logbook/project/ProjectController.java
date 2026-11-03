package com.logbook.project;

import com.logbook.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository repository;
    private final AuthService authService;

    public ProjectController(ProjectRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getProjects(HttpSession session) {
        Long currentUserId = authService.requireUserId(session);

        List<ProjectDto> projects = repository.findProjectsByUserId(currentUserId);
        return ResponseEntity.ok(projects);
    }
}
