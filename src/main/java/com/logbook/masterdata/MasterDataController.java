package com.logbook.masterdata;

import com.logbook.service.MasterDataService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/tasks/master-data")
@Tag(name = "Master Data API", description = "Endpoint untuk data referensi (Jenis Task, Durasi)")
public class MasterDataController {

    private final MasterDataService masterDataService;

    public MasterDataController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @GetMapping
    public Map<String, List<Map<String, Object>>> getMasterData() {
        
        log.info("Menerima request GET untuk mengambil Master Data (Task Types, Statuses, Durations)");

        Map<String, List<Map<String, Object>>> result = masterDataService.getMasterData();

        log.info("Berhasil mengambil data Master Data");

        return result;
    }
}