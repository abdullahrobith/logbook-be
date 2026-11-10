package com.logbook.project;

import com.logbook.service.AuthService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Talent API", description = "Endpoint untuk Talent (Proyek, Task, Log Book)")
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

        logger.trace("Memulai method getProjects()");

        try {
            Long currentUserId = authService.requireUserId(session);
            

            logger.debug("Mengambil data proyek untuk User ID: {}", currentUserId);

            List<ProjectDto> projects = repository.findProjectsByUserId(currentUserId);
            
            logger.info("Berhasil mengambil {} data proyek", projects.size());
            return ResponseEntity.ok(projects);

        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                logger.warn("Gagal mengambil proyek: User belum login atau sesi habis");
            } else {
                logger.error("Gagal mengambil data proyek: {}", e.getMessage(), e);
            }
            
            throw e; 
        }
    }
}