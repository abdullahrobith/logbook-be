package com.logbook.service;

import com.logbook.dto.AdminImportResponse;
import com.logbook.dto.AdminLeaveResponse;
import com.logbook.dto.AdminTalentOption;
import com.logbook.entity.Holiday;
import com.logbook.entity.Leave;
import com.logbook.entity.User;
import com.logbook.exception.ApiException;
import com.logbook.repository.HolidayRepository;
import com.logbook.repository.LeaveRepository;
import com.logbook.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final HolidayRepository holidayRepository;
    private final LeaveRepository leaveRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Clock clock;

    public AdminService(
            UserRepository userRepository,
            HolidayRepository holidayRepository,
            LeaveRepository leaveRepository,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.holidayRepository = holidayRepository;
        this.leaveRepository = leaveRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public AdminImportResponse importUsers(MultipartFile file, String currentUserEmail) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File CSV tidak boleh kosong");
        }

        List<CSVRecord> records;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                     .setHeader("email", "nama", "jenis", "email_atasan", "admin")
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .build())) {

            records = parser.getRecords();
        } catch (Exception e) {
            log.warn("Gagal membaca file CSV: {}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_REQUEST, "Format CSV tidak valid. Pastikan header: email,nama,jenis,email_atasan,admin");
        }

        // 1. VALIDASI
        int lineNumber = 2;
        for (CSVRecord record : records) {
            String email = record.get("email");
            String emailAtasan = record.get("email_atasan");
            String adminStr = record.get("admin");

            if (email == null || email.isBlank() || record.get("nama").isBlank() || record.get("jenis").isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Baris ke-" + lineNumber + " salah: Email, Nama, dan Jenis wajib diisi.");
            }

            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Baris ke-" + lineNumber + " salah: Format email tidak valid.");
            }

            if (emailAtasan != null && !emailAtasan.isBlank() && !userRepository.existsByEmailIgnoreCase(emailAtasan)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Baris ke-" + lineNumber + " salah: Email atasan '" + emailAtasan + "' tidak ditemukan.");
            }

            if (adminStr != null && !adminStr.isBlank() && !adminStr.equalsIgnoreCase("true") && !adminStr.equalsIgnoreCase("false")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Baris ke-" + lineNumber + " salah: Kolom admin harus 'true' atau 'false'.");
            }
            lineNumber++;
        }

        // 2. SIMPAN DATA
        int created = 0;
        int updated = 0;
        List<String> importedEmails = new ArrayList<>();

        for (CSVRecord record : records) {
            String email = record.get("email").trim().toLowerCase();
            String nama = record.get("nama").trim();
            String jenis = record.get("jenis").trim();
            String emailAtasan = record.get("email_atasan");
            boolean isAdmin = Boolean.parseBoolean(record.get("admin"));

            if (currentUserEmail != null && email.equalsIgnoreCase(currentUserEmail) && !isAdmin) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Anda tidak dapat mencabut hak admin milik sendiri melalui CSV.");
            }

            User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

            if (user != null) {
                user.setFullName(nama);
                user.setJenis(jenis);
                user.setEmailAtasan((emailAtasan == null || emailAtasan.isBlank()) ? null : emailAtasan.trim().toLowerCase());
                user.setAdmin(isAdmin);
                userRepository.save(user);
                updated++;
            } else {
                String tempPassword = generateTemporaryPassword();
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setFullName(nama);
                newUser.setJenis(jenis);
                newUser.setEmailAtasan((emailAtasan == null || emailAtasan.isBlank()) ? null : emailAtasan.trim().toLowerCase());
                newUser.setAdmin(isAdmin);
                newUser.setPasswordHash(passwordEncoder.encode(tempPassword));
                newUser.setMustChangePassword(true);
                newUser.setActive(true);
                newUser.setCreatedAt(OffsetDateTime.now(clock));
                userRepository.save(newUser);

                importedEmails.add(email);
                created++;
            }
        }

        // 3. PROTEKSI MINIMAL 1 ADMIN
        if (userRepository.countByAdminTrue() == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Impor dibatalkan: Sistem harus memiliki minimal 1 Admin.");
        }

        return new AdminImportResponse(created, updated, importedEmails);
    }

    private String generateTemporaryPassword() {
        byte[] bytes = new byte[12];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public List<AdminTalentOption> getTalentOptions() {
        return userRepository.findByActiveTrueAndAdminFalseOrderByFullNameAsc()
                .stream()
                .map(AdminTalentOption::from)
                .toList();
    }

    public List<Holiday> getHolidays() {
        return holidayRepository.findAllByOrderByHolidayDateAsc();
    }

    public void addHoliday(LocalDate date, String name) {
        Holiday holiday = holidayRepository.findByHolidayDate(date).orElse(new Holiday());
        holiday.setHolidayDate(date);
        holiday.setName(name.trim());
        if (holiday.getCreatedAt() == null) {
            holiday.setCreatedAt(OffsetDateTime.now(clock));
        }
        holidayRepository.save(holiday);
    }

    public void deleteHoliday(LocalDate date) {
        Holiday holiday = holidayRepository.findByHolidayDate(date)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Libur nasional tidak ditemukan"));
        holidayRepository.delete(holiday);
    }

    public List<AdminLeaveResponse> getLeaves() {
        List<Leave> leaves = leaveRepository.findAllByOrderByStartDateAsc();
        List<Long> userIds = leaves.stream().map(Leave::getUserId).distinct().toList();
        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        return leaves.stream()
                .map(leave -> AdminLeaveResponse.from(leave, usersById.get(leave.getUserId())))
                .toList();
    }

    public void addLeave(Long userId, LocalDate start, LocalDate end, String note) {
        if (start.isAfter(end)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tanggal mulai tidak boleh melebihi tanggal selesai");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User tidak ditemukan"));

        Leave leave = new Leave();
        leave.setUserId(user.getId());
        leave.setStartDate(start);
        leave.setEndDate(end);
        leave.setReason(note);
        leave.setCreatedAt(OffsetDateTime.now(clock));
        leaveRepository.save(leave);
    }

    public void deleteLeave(Long id) {
        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cuti tidak ditemukan"));
        leaveRepository.delete(leave);
    }
}