package com.logbook.repository;

import com.logbook.entity.RateLimitAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RateLimitAttemptRepository extends JpaRepository<RateLimitAttempt, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select attempt from RateLimitAttempt attempt where attempt.bucketKey = :bucketKey")
    Optional<RateLimitAttempt> findByBucketKeyForUpdate(@Param("bucketKey") String bucketKey);

    void deleteByBucketKey(String bucketKey);
}
