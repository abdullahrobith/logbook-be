package com.logbook.repository;

import com.logbook.entity.WorkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WorkStatusRepository extends JpaRepository<WorkStatus, Short> {
    Optional<WorkStatus> findByNameIgnoreCase(String name);
    Optional<WorkStatus> findByCodeIgnoreCase(String code);
}
