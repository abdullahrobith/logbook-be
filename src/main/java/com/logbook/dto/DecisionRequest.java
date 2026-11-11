package com.logbook.dto;

/**
 * Alasan pada keputusan Daily Log: alasan penolakan oleh atasan/admin atau
 * alasan pengajuan perbaikan oleh talent.
 */
public record DecisionRequest(String reason) {
}
