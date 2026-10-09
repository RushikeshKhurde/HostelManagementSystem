package com.hostel.management.service;

import com.hostel.management.dto.*;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.PasswordResetOtp;
import com.hostel.management.model.User;
import com.hostel.management.model.VerificationChannel;
import com.hostel.management.repository.PasswordResetOtpRepository;
import com.hostel.management.repository.UserRepository;
import com.hostel.management.service.delivery.OtpDeliveryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordResetOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final List<OtpDeliveryService> deliveryServices;

    @Value("${app.otp.expiry-minutes:5}")
    private int expiryMinutes;

    @Value("${app.otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.otp.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    @Value("${app.otp.max-resends:3}")
    private int maxResends;

    @Value("${app.otp.rate-limit.max-requests:3}")
    private int rateLimitMaxRequests;

    @Value("${app.otp.rate-limit.window-minutes:15}")
    private int rateLimitWindowMinutes;

    @Value("${app.otp.hash-pepper:${app.jwt.secret:DedicatedOtpPepperKeyMustBeAtLeast32BytesLong!}}")
    private String hashPepper;

    @Transactional
    public ForgotPasswordResponse requestOtp(ForgotPasswordRequest request) {
        VerificationChannel channel = request.getMethod() != null ? request.getMethod() : VerificationChannel.EMAIL;
        String identifier = request.getIdentifier() != null ? request.getIdentifier().trim() : "";

        if (identifier.isEmpty()) {
            throw new ApiException("Registered email address is required.", HttpStatus.BAD_REQUEST);
        }

        if (channel != VerificationChannel.EMAIL) {
            throw new ApiException("Unsupported verification method. Only Email verification is supported.", HttpStatus.BAD_REQUEST);
        }

        String destination = identifier.toLowerCase();
        if (!destination.matches("^[a-zA-Z0-9._%+-]+@([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$")) {
            throw new ApiException("Please enter a valid email address.", HttpStatus.BAD_REQUEST);
        }

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(destination);
        String masked = maskDestination(destination, channel);

        // Account enumeration protection: If user not found, log warning and return generic response
        if (userOpt.isEmpty()) {
            log.warn("Forgot password requested for non-existent {} destination", channel);
            return ForgotPasswordResponse.builder()
                    .message("If an account exists for the provided information, an OTP has been sent.")
                    .requestId(null)
                    .destinationMasked(masked)
                    .expiresInSeconds(expiryMinutes * 60L)
                    .resendCooldownSeconds(resendCooldownSeconds)
                    .build();
        }

        User user = userOpt.get();

        if (!user.isEnabled() || "INACTIVE".equalsIgnoreCase(user.getStatus())) {
            log.warn("Forgot password requested for inactive user ID {}", user.getId());
            return ForgotPasswordResponse.builder()
                    .message("If an account exists for the provided information, an OTP has been sent.")
                    .requestId(null)
                    .destinationMasked(masked)
                    .expiresInSeconds(expiryMinutes * 60L)
                    .resendCooldownSeconds(resendCooldownSeconds)
                    .build();
        }

        // Server-side Rate Limiting (per user and per destination)
        LocalDateTime rateLimitCutoff = LocalDateTime.now().minusMinutes(rateLimitWindowMinutes);
        long userRequestCount = otpRepository.countByUserIdAndCreatedAtAfter(user.getId(), rateLimitCutoff);
        long destinationRequestCount = otpRepository.countByDestinationAndCreatedAtAfter(destination, rateLimitCutoff);

        if (userRequestCount >= rateLimitMaxRequests || destinationRequestCount >= rateLimitMaxRequests) {
            log.warn("Rate limit exceeded for user ID {} on forgot password request", user.getId());
            throw new ApiException("Too many OTP requests. Please wait " + rateLimitWindowMinutes + " minutes before trying again.", HttpStatus.TOO_MANY_REQUESTS);
        }

        // Invalidate prior active OTP requests for this user
        otpRepository.invalidateAllActiveForUser(user.getId());

        String requestId = UUID.randomUUID().toString();
        String plainOtp = generateSecure6DigitOtp();
        String otpHash = hashOtp(plainOtp, requestId);

        PasswordResetOtp otpEntity = PasswordResetOtp.builder()
                .user(user)
                .channel(channel)
                .destination(destination)
                .destinationMasked(masked)
                .requestId(requestId)
                .otpHash(otpHash)
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .attemptCount(0)
                .resendCount(0)
                .lastResendAt(LocalDateTime.now())
                .verified(false)
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        otpRepository.save(otpEntity);

        // Dispatch OTP via channel-specific service
        dispatchOtp(user, destination, plainOtp, channel);

        log.info("Password reset OTP requested successfully for user ID {} via {}", user.getId(), channel);

        return ForgotPasswordResponse.builder()
                .message("If an account exists for the provided information, an OTP has been sent.")
                .requestId(requestId)
                .destinationMasked(masked)
                .expiresInSeconds(expiryMinutes * 60L)
                .resendCooldownSeconds(resendCooldownSeconds)
                .build();
    }

    @Transactional(noRollbackFor = ApiException.class)
    public ForgotPasswordResponse resendOtp(ResendOtpRequest request) {
        String requestId = request.getRequestId() != null ? request.getRequestId().trim() : "";

        PasswordResetOtp otpEntity = otpRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ApiException("Invalid or expired password reset session.", HttpStatus.BAD_REQUEST));

        if (otpEntity.isUsed() || otpEntity.isVerified()) {
            throw new ApiException("This password reset session is no longer active. Please start over.", HttpStatus.BAD_REQUEST);
        }

        // Check resend attempt count limit
        if (otpEntity.getResendCount() >= maxResends) {
            log.warn("Max resend limit reached for requestId {}", requestId);
            throw new ApiException("Maximum OTP resend limit reached (" + maxResends + "). Please request a new OTP.", HttpStatus.TOO_MANY_REQUESTS);
        }

        // Check cooldown period (e.g. 60 seconds)
        if (otpEntity.getLastResendAt() != null) {
            LocalDateTime nextAllowedAt = otpEntity.getLastResendAt().plusSeconds(resendCooldownSeconds);
            if (LocalDateTime.now().isBefore(nextAllowedAt)) {
                long waitSeconds = java.time.Duration.between(LocalDateTime.now(), nextAllowedAt).getSeconds() + 1;
                throw new ApiException("Please wait " + waitSeconds + " seconds before requesting a new OTP.", HttpStatus.TOO_MANY_REQUESTS);
            }
        }

        // Generate fresh OTP and update hash
        String newPlainOtp = generateSecure6DigitOtp();
        String newHash = hashOtp(newPlainOtp, requestId);

        otpEntity.setOtpHash(newHash);
        otpEntity.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes));
        otpEntity.setAttemptCount(0); // Reset incorrect attempts on new OTP
        otpEntity.setResendCount(otpEntity.getResendCount() + 1);
        otpEntity.setLastResendAt(LocalDateTime.now());

        otpRepository.save(otpEntity);

        // Dispatch OTP via delivery service
        dispatchOtp(otpEntity.getUser(), otpEntity.getDestination(), newPlainOtp, otpEntity.getChannel());

        log.info("Password reset OTP resent for user ID {} via {}", otpEntity.getUser().getId(), otpEntity.getChannel());

        return ForgotPasswordResponse.builder()
                .message("A new OTP has been sent.")
                .requestId(requestId)
                .destinationMasked(otpEntity.getDestinationMasked())
                .expiresInSeconds(expiryMinutes * 60L)
                .resendCooldownSeconds(resendCooldownSeconds)
                .build();
    }

    @Transactional(noRollbackFor = ApiException.class)
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        String requestId = request.getRequestId() != null ? request.getRequestId().trim() : "";
        String submittedOtp = request.getOtp() != null ? request.getOtp().trim() : "";

        PasswordResetOtp otpEntity = otpRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ApiException("Invalid or expired password reset session.", HttpStatus.BAD_REQUEST));

        if (otpEntity.isUsed()) {
            throw new ApiException("This password reset session has already been used. Please start over.", HttpStatus.BAD_REQUEST);
        }

        if (otpEntity.isVerified()) {
            throw new ApiException("OTP has already been verified. Please proceed to reset your password.", HttpStatus.BAD_REQUEST);
        }

        if (otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException("The OTP has expired. Please request a new OTP.", HttpStatus.BAD_REQUEST);
        }

        if (otpEntity.getAttemptCount() >= maxAttempts) {
            otpEntity.setUsed(true);
            otpRepository.save(otpEntity);
            throw new ApiException("Too many incorrect attempts. Please request a new OTP.", HttpStatus.TOO_MANY_REQUESTS);
        }

        // Increment attempt count
        otpEntity.setAttemptCount(otpEntity.getAttemptCount() + 1);

        String submittedHash = hashOtp(submittedOtp, requestId);

        if (!MessageDigest.isEqual(otpEntity.getOtpHash().getBytes(StandardCharsets.UTF_8), submittedHash.getBytes(StandardCharsets.UTF_8))) {
            otpRepository.save(otpEntity);
            int remaining = maxAttempts - otpEntity.getAttemptCount();
            if (remaining <= 0) {
                otpEntity.setUsed(true);
                otpRepository.save(otpEntity);
                throw new ApiException("Too many incorrect attempts. Please request a new OTP.", HttpStatus.TOO_MANY_REQUESTS);
            }
            throw new ApiException("Invalid OTP. " + remaining + " attempt(s) remaining.", HttpStatus.BAD_REQUEST);
        }

        // Mark OTP as verified and generate short-lived single-use reset token
        String resetToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        otpEntity.setVerified(true);
        otpEntity.setVerifiedAt(LocalDateTime.now());
        otpEntity.setResetToken(resetToken);
        otpEntity.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(10));

        otpRepository.save(otpEntity);

        log.info("OTP verified successfully for password reset on user ID {}", otpEntity.getUser().getId());

        return VerifyOtpResponse.builder()
                .verified(true)
                .resetToken(resetToken)
                .message("OTP verified successfully. Please create a new password.")
                .resetTokenExpiresInSeconds(600)
                .build();
    }

    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        String resetToken = request.getResetToken() != null ? request.getResetToken().trim() : "";

        PasswordResetOtp otpEntity = otpRepository.findByResetToken(resetToken)
                .orElseThrow(() -> new ApiException("Invalid or expired password reset token.", HttpStatus.BAD_REQUEST));

        // If requestId was supplied in the request, ensure it matches the session
        if (request.getRequestId() != null && !request.getRequestId().isBlank()) {
            if (!otpEntity.getRequestId().equals(request.getRequestId().trim())) {
                throw new ApiException("Invalid password reset session.", HttpStatus.BAD_REQUEST);
            }
        }

        if (!otpEntity.isVerified() || otpEntity.isUsed()) {
            throw new ApiException("Invalid or expired password reset token.", HttpStatus.BAD_REQUEST);
        }

        if (otpEntity.getResetTokenExpiresAt() != null && otpEntity.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException("Password reset session has expired. Please start over.", HttpStatus.BAD_REQUEST);
        }

        String newPassword = request.getNewPassword() != null ? request.getNewPassword().trim() : "";
        String confirmPassword = request.getConfirmPassword() != null ? request.getConfirmPassword().trim() : "";

        if (newPassword.isEmpty()) {
            throw new ApiException("New password is required.", HttpStatus.BAD_REQUEST);
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new ApiException("Passwords do not match.", HttpStatus.BAD_REQUEST);
        }

        if (!newPassword.matches(ValidationConstants.PASSWORD_PATTERN)) {
            throw new ApiException(ValidationConstants.PASSWORD_MESSAGE, HttpStatus.BAD_REQUEST);
        }

        User user = otpEntity.getUser();

        // Update password using BCrypt
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Mark OTP record as used
        otpEntity.setUsed(true);
        otpEntity.setUsedAt(LocalDateTime.now());
        otpRepository.save(otpEntity);

        // Invalidate any other active reset tokens for this user
        otpRepository.invalidateAllActiveForUser(user.getId());

        log.info("Password successfully reset for user ID {}", user.getId());

        return ResetPasswordResponse.builder()
                .message("Password reset successful. Please login with your new password.")
                .build();
    }

    @Scheduled(cron = "${app.otp.cleanup-cron:0 0 2 * * ?}")
    @Transactional
    public void cleanupExpiredRecords() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        int deleted = otpRepository.deleteExpiredRecordsOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} expired/used password reset records older than {}", deleted, cutoff);
        }
    }

    private void dispatchOtp(User user, String destination, String otp, VerificationChannel channel) {
        OtpDeliveryService deliveryService = deliveryServices.stream()
                .filter(s -> s.getSupportedChannel() == channel)
                .findFirst()
                .orElseThrow(() -> new ApiException("Delivery service for " + channel + " is not available.", HttpStatus.INTERNAL_SERVER_ERROR));

        deliveryService.sendOtp(user, destination, otp);
    }

    private String generateSecure6DigitOtp() {
        int code = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.format("%06d", code);
    }

    private String hashOtp(String otp, String requestId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = requestId + ":" + otp + ":" + hashPepper;
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String maskDestination(String destination, VerificationChannel channel) {
        if (destination == null || !destination.contains("@")) {
            return "***";
        }
        int atIdx = destination.indexOf('@');
        if (atIdx <= 1) {
            return destination;
        }
        String local = destination.substring(0, atIdx);
        String domain = destination.substring(atIdx);
        if (local.length() <= 2) {
            return local.charAt(0) + "*" + domain;
        }
        return local.charAt(0) + "*****" + domain;
    }
}
