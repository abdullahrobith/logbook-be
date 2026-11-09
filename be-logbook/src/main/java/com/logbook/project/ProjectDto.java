package com.logbook.project;

public record ProjectDto(
    Long project_id,
    String project_name,
    String client_name,
    String color
) {}