package com.hostel.management.repository;

import com.hostel.management.model.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findTopByEmailAndOtpUsedFalseOrderByCreatedAtDesc(String email);

    Optional<EmailVerification> findByRegistrationTokenHashAndTokenUsedFalse(String registrationTokenHash);

    long countByEmailAndCreatedAtAfter(String email, LocalDateTime after);

    @Modifying
    @Query("UPDATE EmailVerification v SET v.otpUsed = true WHERE v.email = :email AND v.otpUsed = false")
    void invalidateAllActiveForEmail(@Param("email") String email);

    @Modifying
    @Query("UPDATE EmailVerification v SET v.tokenUsed = true, v.tokenUsedAt = :usedAt WHERE v.id = :id AND v.tokenUsed = false")
    int consumeTokenAtomically(@Param("id") Long id, @Param("usedAt") LocalDateTime usedAt);

    @Modifying
    @Query("DELETE FROM EmailVerification v WHERE v.createdAt < :cutoffDate AND (v.tokenUsed = true OR v.otpUsed = true OR v.expiresAt < :cutoffDate)")
    int deleteExpiredRecordsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}
