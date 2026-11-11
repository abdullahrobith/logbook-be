package com.logbook.service;

import com.logbook.dto.DailyLogResponse;
import com.logbook.dto.TeamMemberResponse;
import com.logbook.entity.DailyLog;
import com.logbook.entity.User;
import com.logbook.repository.DailyLogRepository;
import com.logbook.repository.PendingDayCountProjection;
import com.logbook.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Menyusun daftar Tim untuk atasan/admin.
 *
 * <p>Atasan hanya melihat pengguna aktif yang email_atasan-nya adalah email
 * dirinya; admin melihat seluruh pengguna aktif. Status dan total jam berasal
 * dari Daily Log hari ini, sedangkan pendingDays menghitung seluruh Daily Log
 * berstatus MENUNGGU.</p>
 */
@Slf4j
@Service
public class TeamService {

    private static final String STATUS_WAITING = "MENUNGGU";
    private static final String STATUS_NOT_STARTED = "BELUM_MULAI";

    private final UserRepository userRepository;
    private final DailyLogRepository dailyLogRepository;

    public TeamService(
            UserRepository userRepository,
            DailyLogRepository dailyLogRepository
    ) {
        this.userRepository = userRepository;
        this.dailyLogRepository = dailyLogRepository;
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getTeam(User actor) {
        List<User> members = actor.isAdmin()
                ? userRepository.findByActiveTrueOrderByFullNameAsc()
                : userRepository.findByEmailAtasanIgnoreCaseAndActiveTrueOrderByFullNameAsc(
                        actor.getEmail()
                );

        if (members.isEmpty()) {
            log.info("Tidak ada anggota tim untuk User ID {} ({})", actor.getId(), actor.getEmail());
            return List.of();
        }

        LocalDate today = LocalDate.now();
        List<Long> memberIds = members.stream()
                .map(User::getId)
                .toList();

        Map<Long, DailyLog> todayLogs = dailyLogRepository
                .findByUserIdInAndLogDate(memberIds, today)
                .stream()
                .collect(Collectors.toMap(DailyLog::getUserId, Function.identity()));

        Map<Long, Long> pendingDays = dailyLogRepository
                .countByUserIdInAndStatus(memberIds, STATUS_WAITING)
                .stream()
                .collect(Collectors.toMap(
                        PendingDayCountProjection::getUserId,
                        PendingDayCountProjection::getTotal
                ));

        List<TeamMemberResponse> responses = members.stream()
                .map(member -> toResponse(
                        member,
                        today,
                        todayLogs.get(member.getId()),
                        pendingDays.getOrDefault(member.getId(), 0L)
                ))
                .toList();

        log.info("Berhasil menyusun {} anggota tim untuk User ID {} ({})",
                responses.size(), actor.getId(), actor.getEmail());
        return responses;
    }

    private TeamMemberResponse toResponse(
            User member,
            LocalDate today,
            DailyLog todayLog,
            long pendingDays
    ) {
        if (todayLog == null) {
            return new TeamMemberResponse(
                    member.getId(),
                    member.getFullName(),
                    member.getEmail(),
                    member.getJenis(),
                    today,
                    null,
                    STATUS_NOT_STARTED,
                    0,
                    pendingDays
            );
        }

        return new TeamMemberResponse(
                member.getId(),
                member.getFullName(),
                member.getEmail(),
                member.getJenis(),
                today,
                todayLog.getId(),
                todayLog.getStatus(),
                DailyLogResponse.calculateUnionDuration(todayLog.getTasks()),
                pendingDays
        );
    }
}
