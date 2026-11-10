package com.logbook.dto;

import java.util.List;
import java.util.Map;

public record AdminImportResponse(
    int created,
    int updated,
    List<Map<String, String>> tempPasswords
) {}