package com.logbook.service;

import com.logbook.dto.LeaveRequest;
import com.logbook.entity.Leave;
import com.logbook.exception.ApiException;
import com.logbook.repository.LeaveRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class LeaveService {

    private final LeaveRepository leaveRepository;

    public LeaveService(LeaveRepository leaveRepository) {
        this.leaveRepository = leaveRepository;
    }

    public void submitLeave(Long userId, LeaveRequest request) {
        List<Leave> overlapping = leaveRepository.findOverlappingForUser(
                userId,
                request.getFromDate(),
                request.getToDate()
        );

        if (!overlapping.isEmpty()) {
            // UBAH KE ApiException
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Gagal! Kamu sudah memiliki cuti di tanggal tersebut."
            );
        }

        Leave leave = new Leave();
        leave.setUserId(userId);
        leave.setStartDate(request.getFromDate());  
        leave.setEndDate(request.getToDate());      
        
        leave.setCreatedAt(OffsetDateTime.now());

        leaveRepository.save(leave);
    }

    public List<Leave> getLeavesByUser(Long userId) {
        return leaveRepository.findByUserId(userId);
    }
}