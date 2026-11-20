package com.logbook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.logbook.entity.User;

public record AdminTalentOption(
        Long id,

        @JsonProperty("full_name")
        String fullName
) {
    public static AdminTalentOption from(User user) {
        return new AdminTalentOption(user.getId(), user.getFullName());
    }
}
