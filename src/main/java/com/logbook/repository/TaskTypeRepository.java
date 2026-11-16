package com.logbook.repository;

import com.logbook.entity.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TaskTypeRepository extends JpaRepository<TaskType, Short> {
    Optional<TaskType> findByNameIgnoreCase(String name);
    Optional<TaskType> findByCodeIgnoreCase(String code);
}
