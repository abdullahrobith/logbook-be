package com.logbook.example;

import com.logbook.service.ExampleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/examples")
@Tag(name = "Talent API", description = "Endpoint untuk konten Contoh Log Book")
public class ExampleController {

    private final ExampleService exampleService;

    public ExampleController(ExampleService exampleService) {
        this.exampleService = exampleService;
    }

    @GetMapping
    public Map<String, List<Map<String, Object>>> getExamples() {
        
        log.info("Menerima request GET untuk data Contoh Log Book");

        Map<String, List<Map<String, Object>>> result = exampleService.getExamplesData();

        log.info("Berhasil mengambil data Contoh Log Book");

        return result;
    }
}