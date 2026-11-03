
package com.logbook.repository;

import com.logbook.entity.LogTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogTaskRepository extends JpaRepository<LogTask, Long> {
}
