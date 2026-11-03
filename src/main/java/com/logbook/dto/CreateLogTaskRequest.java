
package com.logbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.LocalTime;

public record CreateLogTaskRequest(

        @NotNull(message = "Project ID wajib diisi")
        @Positive(message = "Project ID harus lebih dari 0")
        Long projectId,

        @NotNull(message = "Task type ID wajib diisi")
        @Positive(message = "Task type ID harus lebih dari 0")
        Short taskTypeId,

        @NotNull(message = "Work status ID wajib diisi")
        Short workStatusId,

        @Pattern(
                regexp = "^(?:[A-Za-z][A-Za-z0-9]*-[0-9]+)?$",
                message = "Format tiket tidak valid. Contoh: ABC-154"
        )
        String ticket,

        @NotNull(message = "Jam mulai wajib diisi")
        LocalTime startTime,

        @NotNull(message = "Durasi wajib diisi")
        @Positive(message = "Durasi harus lebih dari 0 menit")
        Short durationMinutes,

        @NotBlank(message = "Ringkasan wajib diisi")
        String summary

) {
}
