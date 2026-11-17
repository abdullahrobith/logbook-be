package com.logbook.service;

import com.logbook.entity.LogTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Perhitungan durasi task. Durasi penuh dipakai untuk rekap per jenis dan per
 * proyek, sedangkan durasi gabungan (union antar rentang yang tumpang tindih)
 * dipakai untuk total jam harian dan mingguan/bulanan (SRS LBK-PAR-01).
 */
public final class TaskDurationUtil {

    private TaskDurationUtil() {
    }

    public static int minutesOf(LogTask task) {
        return task.getDurationMinutes() == null ? 0 : task.getDurationMinutes();
    }

    /**
     * Panjang gabungan rentang task dalam satu hari. Task yang berjalan bersamaan
     * dihitung sekali.
     */
    public static int unionMinutes(List<LogTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }

        List<int[]> intervals = new ArrayList<>();
        for (LogTask task : tasks) {
            int start = task.getStartMinute() == null ? 0 : task.getStartMinute();
            intervals.add(new int[]{start, start + minutesOf(task)});
        }
        intervals.sort(Comparator.comparingInt(interval -> interval[0]));

        int total = 0;
        int currentStart = intervals.get(0)[0];
        int currentEnd = intervals.get(0)[1];
        for (int i = 1; i < intervals.size(); i++) {
            int[] next = intervals.get(i);
            if (next[0] <= currentEnd) {
                currentEnd = Math.max(currentEnd, next[1]);
            } else {
                total += currentEnd - currentStart;
                currentStart = next[0];
                currentEnd = next[1];
            }
        }
        return total + (currentEnd - currentStart);
    }
}
