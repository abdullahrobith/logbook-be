package com.logbook.service;

import com.logbook.repository.ExampleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ExampleService {

    private final ExampleRepository exampleRepository;

    public ExampleService(ExampleRepository exampleRepository) {
        this.exampleRepository = exampleRepository;
    }

    public Map<String, List<Map<String, Object>>> getExamplesData() {
        List<Map<String, Object>> cards = exampleRepository.getCards();
        List<Map<String, Object>> entries = exampleRepository.getEntries();
        List<Map<String, Object>> tips = exampleRepository.getTips();

        return Map.of(
            "cards", cards,
            "entries", entries,
            "tips", tips
        );
    }
}