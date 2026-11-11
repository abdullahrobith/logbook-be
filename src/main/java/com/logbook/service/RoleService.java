package com.logbook.service;

import com.logbook.dto.RoleResponse;
import com.logbook.entity.User;
import com.logbook.repository.UserRepository;
import com.logbook.security.Role;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.Set;

/**
 * Menentukan peran pengguna pada setiap permintaan dan menegakkan otorisasi
 * dasar: talent hanya miliknya, atasan hanya timnya, admin semua.
 */
@Slf4j
@Service
public class RoleService {

    private final UserRepository userRepository;

    public RoleService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User requireCurrentUser(HttpSession session) {
        Object value = session.getAttribute(AuthService.SESSION_USER_ID);

        if (!(value instanceof Long userId)) {
            log.debug("Request tanpa sesi login yang valid");
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Silakan login terlebih dahulu"
            );
        }

        return userRepository
                .findByIdAndActiveTrue(userId)
                .orElseThrow(() -> {
                    log.warn("Sesi tidak valid: User ID {} tidak ditemukan atau tidak aktif", userId);
                    return new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Sesi tidak valid"
                    );
                });
    }

    @Transactional(readOnly = true)
    public RoleResponse currentRoles(HttpSession session) {
        User user = requireCurrentUser(session);
        Set<Role> roles = rolesOf(user);

        long teamSize = roles.contains(Role.ATASAN)
                ? userRepository.countByEmailAtasanIgnoreCaseAndActiveTrue(user.getEmail())
                : 0;

        return RoleResponse.from(user, roles, teamSize);
    }

    /**
     * Peran dihitung ulang dari data setiap kali dipanggil: TALENT selalu ada,
     * ATASAN bila ada pengguna aktif yang email_atasan-nya email pengguna ini,
     * dan ADMIN bila flag is_admin bernilai benar.
     */
    @Transactional(readOnly = true)
    public Set<Role> rolesOf(User user) {
        Set<Role> roles = EnumSet.of(Role.TALENT);

        if (user.isAdmin()) {
            roles.add(Role.ADMIN);
        }

        if (isSupervisor(user)) {
            roles.add(Role.ATASAN);
        }

        return roles;
    }

    @Transactional(readOnly = true)
    public boolean isSupervisor(User user) {
        return user.getEmail() != null
                && userRepository.existsByEmailAtasanIgnoreCaseAndActiveTrue(user.getEmail());
    }

    public boolean isSupervisorOf(User supervisor, User talent) {
        return supervisor.getEmail() != null
                && talent.getEmailAtasan() != null
                && talent.getEmailAtasan().equalsIgnoreCase(supervisor.getEmail());
    }

    public boolean canViewTalent(User actor, User talent) {
        return actor.isAdmin()
                || actor.getId().equals(talent.getId())
                || isSupervisorOf(actor, talent);
    }

    /**
     * Atasan langsung atau admin boleh memutuskan Daily Log talent, tetapi
     * tidak ada yang boleh memutuskan Daily Log miliknya sendiri.
     */
    public boolean canDecideTalent(User actor, User talent) {
        if (actor.getId().equals(talent.getId())) {
            return false;
        }

        return actor.isAdmin() || isSupervisorOf(actor, talent);
    }

    @Transactional(readOnly = true)
    public User requireAdmin(HttpSession session) {
        User user = requireCurrentUser(session);

        if (!user.isAdmin()) {
            log.warn("Akses admin ditolak untuk User ID {} ({})", user.getId(), user.getEmail());
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Hanya admin yang dapat mengakses fitur ini"
            );
        }

        return user;
    }

    @Transactional(readOnly = true)
    public User requireSupervisorOrAdmin(HttpSession session) {
        User user = requireCurrentUser(session);

        if (!user.isAdmin() && !isSupervisor(user)) {
            log.warn("Akses atasan/admin ditolak untuk User ID {} ({})", user.getId(), user.getEmail());
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Hanya atasan atau admin yang dapat mengakses fitur ini"
            );
        }

        return user;
    }

    @Transactional(readOnly = true)
    public User requireCanViewTalent(User actor, Long talentUserId) {
        User talent = userRepository
                .findByIdAndActiveTrue(talentUserId)
                .orElseThrow(() -> {
                    log.warn("Talent User ID {} tidak ditemukan (diminta oleh User ID {})",
                            talentUserId, actor.getId());
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Pengguna tidak ditemukan"
                    );
                });

        if (!canViewTalent(actor, talent)) {
            log.warn("User ID {} ({}) tidak berhak melihat data User ID {}",
                    actor.getId(), actor.getEmail(), talentUserId);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tidak memiliki akses ke data pengguna ini"
            );
        }

        return talent;
    }

    @Transactional(readOnly = true)
    public User requireCanDecideTalent(User actor, Long talentUserId) {
        User talent = userRepository
                .findByIdAndActiveTrue(talentUserId)
                .orElseThrow(() -> {
                    log.warn("Talent User ID {} tidak ditemukan (diminta oleh User ID {})",
                            talentUserId, actor.getId());
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Pengguna tidak ditemukan"
                    );
                });

        if (!canDecideTalent(actor, talent)) {
            log.warn("User ID {} ({}) tidak berwenang memutuskan Daily Log milik User ID {}",
                    actor.getId(), actor.getEmail(), talentUserId);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tidak berwenang memutuskan Daily Log pengguna ini"
            );
        }

        return talent;
    }
}
