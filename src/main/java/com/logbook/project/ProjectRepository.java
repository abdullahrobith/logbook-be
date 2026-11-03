package com.logbook.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query(value = "SELECT p.id AS project_id, p.name AS project_name, c.name AS client_name, c.color AS color " +
                   "FROM project_members pm " +
                   "JOIN projects p ON pm.project_id = p.id " +
                   "JOIN clients c ON p.client_id = c.id " +
                   "WHERE pm.user_id = :userId AND pm.left_at IS NULL " +
                   "ORDER BY pm.last_used_at DESC", nativeQuery = true)
    List<ProjectDto> findProjectsByUserId(@Param("userId") Long userId);

        @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM project_members pm
            WHERE pm.project_id = :projectId
            AND pm.user_id = :userId
            AND pm.left_at IS NULL
        )
        """, nativeQuery = true)
    boolean existsActiveProjectForUser(
        @Param("projectId") Long projectId,
        @Param("userId") Long userId
    );
}