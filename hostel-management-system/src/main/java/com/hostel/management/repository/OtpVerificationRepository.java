package com.hostel.management.repository;

import com.hostel.management.model.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByIdentifierAndTypeAndUsedFalseOrderByCreatedAtDesc(
            String identifier, OtpVerification.OtpType type);

    Optional<OtpVerification> findTopByIdentifierAndTypeOrderByCreatedAtDesc(
            String identifier, OtpVerification.OtpType type);

    Optional<OtpVerification> findTopByIdentifierAndTypeAndResetTokenAndResetTokenExpiryAfter(
            String identifier, OtpVerification.OtpType type, String resetToken, LocalDateTime now);
}
