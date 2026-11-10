package com.logbook.dto;

import com.logbook.entity.User;
import com.logbook.security.Role;

import java.util.Collection;
import java.util.List;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String jenis,
        String emailAtasan,
        boolean admin,
        boolean mustChangePassword,
        List<String> roles
) {
    public static UserResponse from(User user, Collection<Role> roles) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getJenis(),
                user.getEmailAtasan(),
                user.isAdmin(),
                user.isMustChangePassword(),
                roles.stream().map(Enum::name).toList()
        );
    }
}
