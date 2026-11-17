package com.logbook.repository;

import com.logbook.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCaseAndActiveTrue(String email);
    Optional<User> findByIdAndActiveTrue(Long id);
    List<User> findByActiveTrueOrderByFullNameAsc();
    boolean existsByEmailAtasanIgnoreCaseAndActiveTrue(String email);
    long countByEmailAtasanIgnoreCaseAndActiveTrue(String email);
    List<User> findByEmailAtasanIgnoreCaseAndActiveTrueOrderByFullNameAsc(String email);

    // Untuk mengecek apakah email sudah terdaftar (tanpa peduli active atau tidak)
    boolean existsByEmailIgnoreCase(String email);
    
    // Untuk mencari user berdasarkan email
    Optional<User> findByEmailIgnoreCase(String email);
    
    // Untuk mengecek apakah masih ada admin di sistem
    long countByAdminTrue();
    
    // Untuk dropdown talent (Hanya yang aktif DAN bukan admin)
    List<User> findByActiveTrueAndAdminFalseOrderByFullNameAsc();
}