package com.logbook.project;

import com.logbook.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    private final ProjectRepository repository;
    private final AuthService authService;

    public ProjectController(ProjectRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getProjects(HttpSession session) {
  
        logger.info("Menerima request GET untuk data proyek");

        try {
            Long currentUserId = authService.requireUserId(session);
            logger.debug("Mengambil data proyek untuk User ID: {}", currentUserId);

            List<ProjectDto> projects = repository.findProjectsByUserId(currentUserId);
            
            logger.info("Berhasil mengambil {} data proyek", projects.size());
            return ResponseEntity.ok(projects);

        } catch (Exception e) {
            logger.error("Gagal mengambil data proyek: {}", e.getMessage(), e);
            throw e; 
        }
    }
}