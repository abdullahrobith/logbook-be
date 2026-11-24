package com.logbook.dto;

import java.util.List;

public record AdminImportResponse(
    int created,
    int updated,
    List<String> importedEmails
) {}