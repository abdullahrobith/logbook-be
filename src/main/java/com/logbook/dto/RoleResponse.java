package com.logbook.dto;

import com.logbook.entity.User;
import com.logbook.security.Role;

import java.util.List;
import java.util.Set;

/**
 * Informasi peran pengguna pada permintaan saat ini.
 *
 * <p>Dipakai frontend untuk menyusun menu per peran. Nilai peran selalu
 * dihitung ulang dari data pengguna sehingga perubahan hasil impor CSV
 * langsung berlaku tanpa login ulang.</p>
 */
public record RoleResponse(
        Long userId,
        String email,
        String fullName,
        List<String> roles,
        boolean talent,
        boolean atasan,
        boolean admin,
        String emailAtasan,
        long teamSize
) {
    public static RoleResponse from(User user, Set<Role> roles, long teamSize) {
        return new RoleResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                roles.stream().map(Enum::name).toList(),
                roles.contains(Role.TALENT),
                roles.contains(Role.ATASAN),
                roles.contains(Role.ADMIN),
                user.getEmailAtasan(),
                teamSize
        );
    }
}
