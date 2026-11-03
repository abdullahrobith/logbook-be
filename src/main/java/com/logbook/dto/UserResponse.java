package com.logbook.dto;

import com.logbook.entity.User;

public record UserResponse(
        Long id,
        String email,
        String fullName
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName());
    }
}
