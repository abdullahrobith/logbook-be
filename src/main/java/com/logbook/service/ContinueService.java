package com.logbook.service;

import com.logbook.dto.TaskDto;
import com.logbook.entity.LogTask;
import com.logbook.entity.WorkStatus;
import com.logbook.repository.LogTaskRepository;
import com.logbook.repository.WorkStatusRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Menyediakan saran "Lanjutkan" untuk form task (LBK-TSK-07): task berstatus
 * Berjalan atau Tertahan dari hari terakhir yang berisi catatan pengguna
 * sebelum hari ini.
 *
 * <p>Hari ini tidak ikut dipertimbangkan karena saran ini dipakai saat mengisi
 * catatan baru: yang dilanjutkan adalah pekerjaan dari hari sebelumnya (OCD
 * 5.3.4). Form memakai hasil ini untuk mengisi proyek, tiket, dan jenis task.</p>
 */
@Slf4j
@Service
public class ContinueService {

    private static final Set<String> CONTINUABLE_STATUS_CODES = Set.of("BERJALAN", "TERTAHAN");

    private final LogTaskRepository logTaskRepository;
    private final WorkStatusRepository workStatusRepository;
    private final DayDtoMapper dayDtoMapper;
    private final DeadlineService deadlineService;

    public ContinueService(
            LogTaskRepository logTaskRepository,
            WorkStatusRepository workStatusRepository,
            DayDtoMapper dayDtoMapper,
            DeadlineService deadlineService
    ) {
        this.logTaskRepository = logTaskRepository;
        this.workStatusRepository = workStatusRepository;
        this.dayDtoMapper = dayDtoMapper;
        this.deadlineService = deadlineService;
    }

    /**
     * Task Berjalan/Tertahan dari hari terakhir yang ada isinya sebelum hari
     * ini. Urutan mengikuti jam mulai pada hari itu; daftar kosong bila belum
     * ada catatan sebelumnya.
     */
    @Transactional(readOnly = true)
    public List<TaskDto> findContinueTasks(Long userId) {
        List<Short> continuableStatusIds = workStatusRepository.findAll().stream()
                .filter(status -> CONTINUABLE_STATUS_CODES.contains(codeOf(status)))
                .map(WorkStatus::getId)
                .toList();

        if (continuableStatusIds.isEmpty()) {
            log.warn("Status pekerjaan Berjalan/Tertahan tidak tersedia; saran Lanjutkan dikosongkan untuk User ID {}",
                    userId);
            return List.of();
        }

        List<LogTask> tasks = logTaskRepository.findFromLatestDayWithContentBefore(
                userId,
                deadlineService.today(),
                continuableStatusIds
        );
        log.info("Menemukan {} task Lanjutkan untuk User ID {}", tasks.size(), userId);

        return dayDtoMapper.toTaskDtos(tasks);
    }

    private String codeOf(WorkStatus status) {
        return status.getCode() == null ? "" : status.getCode().trim().toUpperCase(Locale.ROOT);
    }
}
