package com.hostel.management.service;

import com.hostel.management.exception.ApiException;
import com.hostel.management.model.OtpVerification;
import com.hostel.management.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-z0-9._%+-]+@([a-z0-9-]+\\.)+[a-z]{2,}$");

    private final OtpVerificationRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SmsService smsService;

    private final SecureRandom secureRandom = new SecureRandom();

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 10;
    private static final int RATE_LIMIT_SECONDS = 30; // Minimum seconds between new requests
    private static final int MAX_ATTEMPTS = 5;



    @Transactional
    public void sendPasswordResetOtp(String identifier, OtpVerification.OtpType type) {
        String cleanId = cleanIdentifier(identifier, type);

        checkRateLimit(cleanId, type);

        String otp = generateOtpCode();
        String hashedOtp = passwordEncoder.encode(otp);

        invalidatePreviousOtps(cleanId, type);

        OtpVerification record = OtpVerification.builder()
                .identifier(cleanId)
                .otpHash(hashedOtp)
                .type(type)
                .expiryTime(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .used(false)
                .attempts(0)
                .build();
        otpRepository.save(record);

        if (type == OtpVerification.OtpType.PASSWORD_RESET_EMAIL) {
            emailService.sendOtpEmail(cleanId, otp, "Password Reset");
        } else {
            smsService.sendOtp(cleanId, otp, "Password Reset");
        }
    }

    @Transactional
    public String verifyPasswordResetOtp(String identifier, OtpVerification.OtpType type, String otp) {
        String cleanId = cleanIdentifier(identifier, type);
        return verifyOtpInternal(cleanId, type, otp, true);
    }

    @Transactional
    public void validateAndConsumeResetToken(String identifier, OtpVerification.OtpType type, String resetToken) {
        if (resetToken == null || resetToken.isBlank()) {
            throw new ApiException("Invalid or missing password reset token", HttpStatus.BAD_REQUEST);
        }

        String cleanId = cleanIdentifier(identifier, type);

        OtpVerification record = otpRepository.findTopByIdentifierAndTypeAndResetTokenAndResetTokenExpiryAfter(
                cleanId, type, resetToken, LocalDateTime.now()
        ).orElseThrow(() -> new ApiException("Password reset session has expired or is invalid. Please request a new OTP.", HttpStatus.BAD_REQUEST));

        // Invalidate token so it cannot be reused
        record.setResetToken(null);
        record.setResetTokenExpiry(null);
        otpRepository.save(record);
    }

    private String cleanIdentifier(String identifier, OtpVerification.OtpType type) {
        if (identifier == null) return "";
        if (type == OtpVerification.OtpType.PASSWORD_RESET_EMAIL || type == OtpVerification.OtpType.REGISTRATION_EMAIL || type == OtpVerification.OtpType.EMAIL_CHANGE) {
            return identifier.trim().toLowerCase();
        }
        String digits = identifier.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            digits = digits.substring(2);
        } else if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        return digits;
    }

    private String verifyOtpInternal(String identifier, OtpVerification.OtpType type, String otp, boolean generateResetToken) {
        if (otp == null || otp.trim().isBlank()) {
            throw new ApiException("Please enter the 6-digit OTP", HttpStatus.BAD_REQUEST);
        }

        OtpVerification record = otpRepository.findTopByIdentifierAndTypeAndUsedFalseOrderByCreatedAtDesc(identifier, type)
                .orElseThrow(() -> new ApiException("No active OTP found. Please request a new OTP.", HttpStatus.BAD_REQUEST));

        if (LocalDateTime.now().isAfter(record.getExpiryTime())) {
            throw new ApiException("OTP expired. Please request a new OTP.", HttpStatus.BAD_REQUEST);
        }

        if (record.getAttempts() >= MAX_ATTEMPTS) {
            record.setUsed(true);
            otpRepository.save(record);
            throw new ApiException("Maximum verification attempts exceeded. Please request a new OTP.", HttpStatus.BAD_REQUEST);
        }

        if (!passwordEncoder.matches(otp.trim(), record.getOtpHash())) {
            record.setAttempts(record.getAttempts() + 1);
            otpRepository.save(record);
            throw new ApiException("Invalid OTP.", HttpStatus.BAD_REQUEST);
        }

        record.setUsed(true);

        String resetToken = null;
        if (generateResetToken) {
            resetToken = UUID.randomUUID().toString();
            record.setResetToken(resetToken);
            record.setResetTokenExpiry(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES));
        }

        otpRepository.save(record);
        return resetToken;
    }

    private void checkRateLimit(String identifier, OtpVerification.OtpType type) {
        Optional<OtpVerification> lastOpt = otpRepository.findTopByIdentifierAndTypeOrderByCreatedAtDesc(identifier, type);
        if (lastOpt.isPresent()) {
            OtpVerification last = lastOpt.get();
            if (last.getCreatedAt() != null &&
                last.getCreatedAt().plusSeconds(RATE_LIMIT_SECONDS).isAfter(LocalDateTime.now())) {
                throw new ApiException("Please wait a moment before requesting another OTP.", HttpStatus.TOO_MANY_REQUESTS);
            }
        }
    }

    private void invalidatePreviousOtps(String identifier, OtpVerification.OtpType type) {
        Optional<OtpVerification> previousOpt = otpRepository.findTopByIdentifierAndTypeAndUsedFalseOrderByCreatedAtDesc(identifier, type);
        previousOpt.ifPresent(prev -> {
            prev.setUsed(true);
            otpRepository.save(prev);
        });
    }

    private String generateOtpCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    public EmailService getEmailService() {
        return this.emailService;
    }
}
