package com.logbook.service;

import com.logbook.repository.MasterDataRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MasterDataService {

    private final MasterDataRepository masterDataRepository;

    public MasterDataService(MasterDataRepository masterDataRepository) {
        this.masterDataRepository = masterDataRepository;
    }

    public Map<String, List<Map<String, Object>>> getMasterData() {
        List<Map<String, Object>> types = masterDataRepository.getActiveTaskTypes();
        List<Map<String, Object>> statuses = masterDataRepository.getWorkStatuses();
        List<Map<String, Object>> durations = masterDataRepository.getActiveDurations();

        return Map.of(
            "types", types,
            "statuses", statuses,
            "durations", durations
        );
    }
}