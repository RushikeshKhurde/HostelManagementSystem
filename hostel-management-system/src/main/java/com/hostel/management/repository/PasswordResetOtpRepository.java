package com.hostel.management.repository;

import com.hostel.management.model.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp> findByRequestId(String requestId);

    Optional<PasswordResetOtp> findByResetToken(String resetToken);

    List<PasswordResetOtp> findByUserIdAndUsedFalse(Long userId);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);

    long countByDestinationAndCreatedAtAfter(String destination, LocalDateTime after);

    @Modifying
    @Query("UPDATE PasswordResetOtp o SET o.used = true WHERE o.user.id = :userId AND o.used = false")
    void invalidateAllActiveForUser(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM PasswordResetOtp o WHERE o.createdAt < :cutoffDate AND (o.used = true OR o.expiresAt < :cutoffDate)")
    int deleteExpiredRecordsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}
