package com.logbook.repository;

import com.logbook.entity.Leave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRepository extends JpaRepository<Leave, Long> {

    /**
     * Jumlah cuti pengguna yang mencakup tanggal tersebut.
     */
    @Query("""
            select count(l)
            from Leave l
            where l.userId = :userId
              and l.startDate <= :date
              and l.endDate >= :date
            """)
    long countCovering(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );

    /**
     * Seluruh cuti satu pengguna yang beririsan dengan rentang tanggal.
     */
    @Query("""
            select l
            from Leave l
            where l.userId = :userId
              and l.startDate <= :endDate
              and l.endDate >= :startDate
            order by l.startDate
            """)
    List<Leave> findOverlappingForUser(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Seluruh cuti semua pengguna yang beririsan dengan rentang tanggal.
     */
    @Query("""
            select l
            from Leave l
            where l.startDate <= :endDate
              and l.endDate >= :startDate
            order by l.userId, l.startDate
            """)
    List<Leave> findOverlapping(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
