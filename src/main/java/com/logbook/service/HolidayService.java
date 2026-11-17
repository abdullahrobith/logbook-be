package com.logbook.service;

import com.logbook.dto.HolidayRequest;
import com.logbook.entity.Holiday;
import com.logbook.exception.ApiException;
import com.logbook.repository.HolidayRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class HolidayService {

    private final HolidayRepository holidayRepository;

    public HolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    public List<Holiday> getAllHolidays() {
        return holidayRepository.findAll();
    }

    public Holiday createHoliday(HolidayRequest request) {
        if (holidayRepository.existsByHolidayDate(request.getHolidayDate())) {
            // UBAH KE ApiException
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tanggal libur sudah ada di database");
        }

        Holiday holiday = new Holiday();
        holiday.setHolidayDate(request.getHolidayDate());
        holiday.setName(request.getName());      
        holiday.setCreatedAt(OffsetDateTime.now());

        return holidayRepository.save(holiday);
    }
}