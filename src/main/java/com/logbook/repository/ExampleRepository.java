package com.logbook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface ExampleRepository extends JpaRepository<Object, Long> {

    @Query(value = "SELECT * FROM example_cards ORDER BY sort_order", nativeQuery = true)
    List<Map<String, Object>> getCards();

    @Query(value = "SELECT * FROM example_entries ORDER BY sort_order", nativeQuery = true)
    List<Map<String, Object>> getEntries();

    @Query(value = "SELECT title, body FROM writing_tips WHERE is_active = true ORDER BY sort_order", nativeQuery = true)
    List<Map<String, Object>> getTips();
}