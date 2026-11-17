package com.logbook.service;

import com.logbook.entity.LogTask;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskDurationUtilTest {

    @Test
    void overlappingIntervalsAreCountedOnceInUnion() {
        List<LogTask> tasks = List.of(
                task(480, 120),
                task(540, 60),
                task(700, 60)
        );

        assertEquals(180, TaskDurationUtil.unionMinutes(tasks));
    }

    @Test
    void fullMinutesStillCountEveryTask() {
        List<LogTask> tasks = List.of(task(480, 120), task(480, 120));

        assertEquals(120, TaskDurationUtil.unionMinutes(tasks));
        assertEquals(240, TaskDurationUtil.minutesOf(tasks.get(0))
                + TaskDurationUtil.minutesOf(tasks.get(1)));
    }

    @Test
    void emptyAndNullDurationsAreSafe() {
        assertEquals(0, TaskDurationUtil.unionMinutes(List.of()));
        assertEquals(0, TaskDurationUtil.unionMinutes(null));

        LogTask withoutDuration = task(480, 60);
        ReflectionTestUtils.setField(withoutDuration, "durationMinutes", null);
        assertEquals(0, TaskDurationUtil.minutesOf(withoutDuration));
    }

    private LogTask task(int startMinute, int durationMinutes) {
        OffsetDateTime now = OffsetDateTime.parse("2031-03-03T09:00:00+07:00");
        return new LogTask(
                null,
                1L,
                (short) 1,
                (short) 1,
                "ABC-1",
                startMinute,
                (short) durationMinutes,
                "Ringkasan pekerjaan yang cukup panjang.",
                null,
                now,
                now
        );
    }
}
