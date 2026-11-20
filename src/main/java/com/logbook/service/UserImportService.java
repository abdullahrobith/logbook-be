package com.logbook.service;

import com.logbook.dto.ImportResultResponse;
import com.logbook.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class UserImportService {

    private final PasswordEncoder passwordEncoder;
    // private final UserRepository userRepository; // Buka komentar ini jika repository sudah siap

    public UserImportService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public List<ImportResultResponse> processCsvFile(BufferedReader reader) {
        List<ImportResultResponse> results = new ArrayList<>();
        String line;
        boolean isFirstLine = true;

        try {
            while ((line = reader.readLine()) != null) {
                // Lewati baris header CSV
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // Asumsi format CSV: email,full_name,jenis
                String[] data = line.split(",");
                if (data.length < 2) {
                    results.add(new ImportResultResponse("UNKNOWN", "FAILED", "Format baris tidak valid"));
                    continue;
                }

                String email = data[0].trim();
                String fullName = data[1].trim();
                String jenis = data.length > 2 ? data[2].trim() : "UMUM";

                // --- LOGIKA UTAMA ---
                // 1. Generate Password Sementara (Plaintext)
                String tempPassword = generateSecurePassword();

                // 2. Hash menggunakan BCrypt
                String hashedPassword = passwordEncoder.encode(tempPassword);

                // 3. Buat Entity User
                User user = new User();
                user.setEmail(email);
                user.setFullName(fullName);
                user.setJenis(jenis);
                user.setPasswordHash(hashedPassword); // Simpan Hash-nya
                user.setMustChangePassword(true);     // Paksa ganti password
                user.setActive(true);
                user.setAdmin(false);
                user.setCreatedAt(OffsetDateTime.now());

                // 4. Simpan ke Database (Buka komentar jika repo sudah siap)
                // userRepository.save(user);

                // 5. Kirim Plaintext ke Email (JANGAN kembalikan ke response API)
                // emailService.sendTempPassword(email, tempPassword);
                
                // Untuk testing di console (HAPUS INI DI PRODUCTION)
                System.out.println("SIMULASI KIRIM EMAIL ke " + email + " | Password: " + tempPassword);

                // 6. Masukkan hasil AMAN ke response
                results.add(new ImportResultResponse(email, "SUCCESS", "User berhasil diimpor. Cek email untuk password."));
            }
        } catch (Exception e) {
            results.add(new ImportResultResponse("SYSTEM", "FAILED", "Terjadi kesalahan: " + e.getMessage()));
        }

        return results;
    }

    // Fungsi untuk generate password random yang aman
    private String generateSecurePassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}