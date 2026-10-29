package com.hostel.management.service;

import com.hostel.management.dto.SendEmailVerificationOtpRequest;
import com.hostel.management.dto.SendEmailVerificationOtpResponse;
import com.hostel.management.dto.VerifyEmailOtpRequest;
import com.hostel.management.dto.VerifyEmailOtpResponse;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.EmailVerification;
import com.hostel.management.repository.EmailVerificationRepository;
import com.hostel.management.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationRepository verificationRepository;
    private final UserRepository userRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.registration.otp.expiry-minutes:10}")
    private int expiryMinutes;

    @Value("${app.registration.otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.registration.otp.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    @Value("${app.registration.otp.max-requests:5}")
    private int rateLimitMaxRequests;

    @Value("${app.registration.otp.window-minutes:15}")
    private int rateLimitWindowMinutes;

    @Value("${app.registration.token.expiry-minutes:10}")
    private int tokenExpiryMinutes;

    @Value("${app.otp.hash-pepper:${app.jwt.secret:DedicatedRegistrationPepperKeyMustBeAtLeast32Bytes!}}")
    private String hashPepper;

    @Value("${app.otp.email.mode:simulation}")
    private String emailMode;

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.mail.from:${spring.mail.username:noreply@hostelmanagement.com}}")
    private String fromEmail;

    // Rate limiting map for client IP requests (window-based)
    private final Map<String, java.util.List<Long>> ipRateLimitMap = new ConcurrentHashMap<>();

    // For local dev simulation mode
    private final Map<String, String> devSimulationOtps = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (isRealSmtpMode()) {
            if (mailUsername == null || mailUsername.isBlank() || mailPassword == null || mailPassword.isBlank()) {
                log.warn("================================================================================");
                log.warn("[EMAIL REGISTRATION OTP] Real email delivery is enabled, but MAIL_USERNAME or MAIL_PASSWORD is not set.");
                log.warn("Registration OTP email dispatch will fail until valid SMTP credentials are provided.");
                log.warn("================================================================================");
            } else {
                log.info("[EMAIL REGISTRATION OTP] Configured successfully for host '{}' with account '{}'",
                        mailHost, maskEmail(mailUsername));
            }
        } else {
            log.info("[EMAIL REGISTRATION OTP] Running in LOCAL/DEV SIMULATION MODE (OTP_EMAIL_MODE=simulation). Real emails will NOT be dispatched.");
        }
    }

    public boolean isRealSmtpMode() {
        if (emailMode != null && "simulation".equalsIgnoreCase(emailMode.trim())) {
            return false;
        }
        return true;
    }


    @Transactional
    public SendEmailVerificationOtpResponse sendOtp(SendEmailVerificationOtpRequest request, String clientIp) {
        String rawEmail = request.getEmail();
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new ApiException("Email is required.", HttpStatus.BAD_REQUEST);
        }
        if (rawEmail.chars().anyMatch(Character::isUpperCase)) {
            throw new ApiException("Email must be in lowercase.", HttpStatus.BAD_REQUEST);
        }

        String normalizedEmail = rawEmail.trim().toLowerCase();
        if (!normalizedEmail.matches("^[a-z0-9._%+-]+@([a-z0-9-]+\\.)+[a-z]{2,}$")) {
            throw new ApiException("Please enter a valid email address (e.g., username@domain.com).", HttpStatus.BAD_REQUEST);
        }

        // Check if email is already registered in the system
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ApiException("Email is already registered. Please login instead.", HttpStatus.CONFLICT);
        }

        // Apply rate limits
        checkRateLimits(normalizedEmail, clientIp);

        // Invalidate prior active unverified OTPs for this email
        verificationRepository.invalidateAllActiveForEmail(normalizedEmail);

        // Generate 6-digit cryptographically secure OTP
        String plainOtp = generateSecure6DigitOtp();
        devSimulationOtps.put(normalizedEmail, plainOtp);
        String otpHash = hashWithPepper(normalizedEmail + ":" + plainOtp);

        EmailVerification entity = EmailVerification.builder()
                .email(normalizedEmail)
                .otpHash(otpHash)
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .attemptCount(0)
                .resendCount(0)
                .lastResendAt(LocalDateTime.now())
                .verified(false)
                .otpUsed(false)
                .tokenUsed(false)
                .createdAt(LocalDateTime.now())
                .build();

        verificationRepository.save(entity);

        // Dispatch OTP via email delivery
        dispatchVerificationEmail(normalizedEmail, plainOtp);

        log.info("Student registration verification OTP requested for email domain ending in @{}",
                normalizedEmail.contains("@") ? normalizedEmail.substring(normalizedEmail.indexOf("@") + 1) : "***");

        return SendEmailVerificationOtpResponse.builder()
                .message("Verification code sent to your email address.")
                .email(normalizedEmail)
                .destinationMasked(maskEmail(normalizedEmail))
                .expiresInSeconds(expiryMinutes * 60L)
                .resendCooldownSeconds(resendCooldownSeconds)
                .build();
    }

    @Transactional(noRollbackFor = ApiException.class)
    public VerifyEmailOtpResponse verifyOtp(VerifyEmailOtpRequest request) {
        String rawEmail = request.getEmail();
        String rawOtp = request.getOtp();

        if (rawEmail == null || rawEmail.isBlank()) {
            throw new ApiException("Email is required.", HttpStatus.BAD_REQUEST);
        }
        if (rawOtp == null || rawOtp.isBlank()) {
            throw new ApiException("OTP is required.", HttpStatus.BAD_REQUEST);
        }

        String normalizedEmail = rawEmail.trim().toLowerCase();
        String trimmedOtp = rawOtp.trim();

        if (!trimmedOtp.matches("^\\d{6}$")) {
            throw new ApiException("OTP must be a 6-digit number.", HttpStatus.BAD_REQUEST);
        }

        EmailVerification entity = verificationRepository.findTopByEmailAndOtpUsedFalseOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(() -> new ApiException("No active verification code found for this email. Please request a new code.", HttpStatus.BAD_REQUEST));

        if (entity.isVerified() || entity.isOtpUsed()) {
            throw new ApiException("This verification session is no longer active. Please request a new code.", HttpStatus.BAD_REQUEST);
        }

        if (LocalDateTime.now().isAfter(entity.getExpiresAt())) {
            entity.setOtpUsed(true);
            verificationRepository.save(entity);
            throw new ApiException("Verification code has expired. Please request a new code.", HttpStatus.BAD_REQUEST);
        }

        if (entity.getAttemptCount() >= maxAttempts) {
            entity.setOtpUsed(true);
            verificationRepository.save(entity);
            throw new ApiException("Maximum verification attempts exceeded. Please request a new code.", HttpStatus.TOO_MANY_REQUESTS);
        }

        // Increment attempt count
        entity.setAttemptCount(entity.getAttemptCount() + 1);

        String submittedHash = hashWithPepper(normalizedEmail + ":" + trimmedOtp);

        if (!MessageDigest.isEqual(entity.getOtpHash().getBytes(StandardCharsets.UTF_8), submittedHash.getBytes(StandardCharsets.UTF_8))) {
            verificationRepository.save(entity);
            int remaining = maxAttempts - entity.getAttemptCount();
            if (remaining <= 0) {
                entity.setOtpUsed(true);
                verificationRepository.save(entity);
                throw new ApiException("Maximum verification attempts exceeded. Please request a new code.", HttpStatus.TOO_MANY_REQUESTS);
            }
            throw new ApiException("Invalid verification code. " + remaining + " attempt(s) remaining.", HttpStatus.BAD_REQUEST);
        }

        // Verification successful: Invalidate OTP and issue single-use registration authorization token (256+ bits entropy)
        String registrationToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        String tokenHash = hashWithPepper(registrationToken);

        entity.setVerified(true);
        entity.setOtpUsed(true); // Consumed OTP so it cannot be verified again
        entity.setVerifiedAt(LocalDateTime.now());
        entity.setRegistrationTokenHash(tokenHash);
        entity.setTokenExpiresAt(LocalDateTime.now().plusMinutes(tokenExpiryMinutes));
        entity.setTokenUsed(false);

        verificationRepository.save(entity);

        log.info("Email ownership successfully verified for registration of domain ending in @{}",
                normalizedEmail.contains("@") ? normalizedEmail.substring(normalizedEmail.indexOf("@") + 1) : "***");

        return VerifyEmailOtpResponse.builder()
                .message("Email verified successfully.")
                .verified(true)
                .registrationToken(registrationToken)
                .expiresInSeconds(tokenExpiryMinutes * 60L)
                .build();
    }

    @Transactional
    public void validateAndConsumeRegistrationToken(String normalizedEmail, String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ApiException("Email verification token is required. Please verify your email first.", HttpStatus.BAD_REQUEST);
        }

        String tokenHash = hashWithPepper(rawToken.trim());

        EmailVerification entity = verificationRepository.findByRegistrationTokenHashAndTokenUsedFalse(tokenHash)
                .orElseThrow(() -> new ApiException("Invalid or expired email verification token. Please verify your email again.", HttpStatus.BAD_REQUEST));

        if (!entity.isVerified() || entity.isTokenUsed()) {
            throw new ApiException("Invalid or expired email verification token. Please verify your email again.", HttpStatus.BAD_REQUEST);
        }

        if (!entity.getEmail().equalsIgnoreCase(normalizedEmail)) {
            throw new ApiException("Email verification token does not match the registration email.", HttpStatus.BAD_REQUEST);
        }

        if (entity.getTokenExpiresAt() != null && entity.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException("Email verification token has expired. Please verify your email again.", HttpStatus.BAD_REQUEST);
        }

        // Atomically consume token to prevent race conditions & double-usage
        int updatedRows = verificationRepository.consumeTokenAtomically(entity.getId(), LocalDateTime.now());
        if (updatedRows == 0) {
            throw new ApiException("Email verification token has already been used. Please verify your email again.", HttpStatus.BAD_REQUEST);
        }

        log.info("Registration authorization token consumed for email domain ending in @{}",
                normalizedEmail.contains("@") ? normalizedEmail.substring(normalizedEmail.indexOf("@") + 1) : "***");
    }

    private void checkRateLimits(String email, String clientIp) {
        // Check cooldown from last active request
        Optional<EmailVerification> lastRecord = verificationRepository.findTopByEmailAndOtpUsedFalseOrderByCreatedAtDesc(email);
        if (lastRecord.isPresent()) {
            LocalDateTime lastTime = lastRecord.get().getLastResendAt() != null ? lastRecord.get().getLastResendAt() : lastRecord.get().getCreatedAt();
            if (lastTime != null) {
                LocalDateTime nextAllowed = lastTime.plusSeconds(resendCooldownSeconds);
                if (LocalDateTime.now().isBefore(nextAllowed)) {
                    long waitSeconds = java.time.Duration.between(LocalDateTime.now(), nextAllowed).getSeconds() + 1;
                    throw new ApiException("Please wait " + waitSeconds + " seconds before requesting another verification code.", HttpStatus.TOO_MANY_REQUESTS);
                }
            }
        }

        // Check sliding window rate limit for this email
        LocalDateTime windowCutoff = LocalDateTime.now().minusMinutes(rateLimitWindowMinutes);
        long emailCount = verificationRepository.countByEmailAndCreatedAtAfter(email, windowCutoff);
        if (emailCount >= rateLimitMaxRequests) {
            throw new ApiException("Too many verification requests for this email. Please try again after " + rateLimitWindowMinutes + " minutes.", HttpStatus.TOO_MANY_REQUESTS);
        }

        // Check IP rate limit if clientIp is present (exclude loopback addresses for local testing)
        if (clientIp != null && !clientIp.isBlank() && !"127.0.0.1".equals(clientIp) && !"0:0:0:0:0:0:0:1".equals(clientIp) && !"localhost".equalsIgnoreCase(clientIp)) {
            long now = System.currentTimeMillis();
            long windowMs = rateLimitWindowMinutes * 60 * 1000L;
            ipRateLimitMap.compute(clientIp, (ip, timestamps) -> {
                if (timestamps == null) {
                    timestamps = new java.util.concurrent.CopyOnWriteArrayList<>();
                }
                timestamps.removeIf(t -> now - t > windowMs);
                if (timestamps.size() >= rateLimitMaxRequests * 5) {
                    throw new ApiException("Too many requests from your IP address. Please try again later.", HttpStatus.TOO_MANY_REQUESTS);
                }
                timestamps.add(now);
                return timestamps;
            });
        }
    }

    public void clearRateLimits() {
        ipRateLimitMap.clear();
        devSimulationOtps.clear();
    }


    private void dispatchVerificationEmail(String destination, String otp) {
        if (isRealSmtpMode()) {
            if (mailUsername == null || mailUsername.isBlank() || mailPassword == null || mailPassword.isBlank()) {
                log.error("Email verification dispatch failed: Mail credentials (MAIL_USERNAME / MAIL_PASSWORD) are not configured on the server.");
                throw new ApiException("Email delivery is currently not configured on the server. Please configure SMTP credentials (MAIL_USERNAME and MAIL_PASSWORD) or contact administrator.", HttpStatus.SERVICE_UNAVAILABLE);
            }

            if (mailSender == null) {
                log.error("Email verification dispatch failed: JavaMailSender bean is not available.");
                throw new ApiException("Email delivery service is unavailable. Please check mail server configuration.", HttpStatus.SERVICE_UNAVAILABLE);
            }

            try {
                MimeMessage mimeMessage;
                try {
                    mimeMessage = mailSender.createMimeMessage();
                } catch (Exception ex) {
                    mimeMessage = null;
                }
                if (mimeMessage == null) {
                    mimeMessage = new MimeMessage((jakarta.mail.Session) null);
                }

                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                String effectiveSender = (fromEmail != null && !fromEmail.isBlank() && fromEmail.contains("@"))
                        ? fromEmail.trim()
                        : mailUsername.trim();

                helper.setFrom(effectiveSender, "Hostel Management System");
                helper.setTo(destination);
                helper.setSubject("Verify Your Email – Hostel Management System");
                helper.setText(buildHtmlEmailContent(otp, destination), true);

                mailSender.send(mimeMessage);
                log.info("Student registration verification email dispatched successfully to recipient domain ending in @{}",
                        destination.contains("@") ? destination.substring(destination.indexOf("@") + 1) : "***");
                return;
            } catch (MailAuthenticationException ex) {
                log.error("SMTP authentication failed for host {}. Verify MAIL_USERNAME and MAIL_PASSWORD (e.g. Google 16-char App Password): {}",
                        mailHost, ex.getMessage());
                throw new ApiException("Failed to authenticate with mail server. Please verify that your Google account has 2-Step Verification enabled and you are using a valid 16-character Google App Password (without spaces) in MAIL_PASSWORD.", HttpStatus.SERVICE_UNAVAILABLE);
            } catch (MailException ex) {
                log.error("Failed to deliver verification email via {}: {}", mailHost, ex.getMessage());
                String diagnostic = extractDiagnosticMessage(ex);
                if (diagnostic != null) {
                    throw new ApiException(diagnostic, HttpStatus.SERVICE_UNAVAILABLE);
                }
                throw new ApiException("Failed to deliver verification email. Please check mail server settings and internet connectivity.", HttpStatus.SERVICE_UNAVAILABLE);
            } catch (ApiException ex) {
                throw ex;
            } catch (Exception ex) {
                log.error("Unexpected error sending verification email: {}", ex.getMessage(), ex);
                throw new ApiException("Failed to send verification email. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        // Simulation / development mode: log details if explicitly configured
        log.info("==================================================");
        log.info("[REGISTRATION EMAIL OTP - SIMULATION MODE]");
        log.info("To: {}", maskEmail(destination));
        log.info("Purpose: Student Account Registration Verification");
        log.info("OTP Code: [PROTECTED]");
        log.info("Expiry: {} minutes", expiryMinutes);
        log.info("Note: Running in simulation mode because OTP_EMAIL_MODE=simulation is configured.");
        log.info("==================================================");
    }

    private String extractDiagnosticMessage(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String msg = current.getMessage() != null ? current.getMessage().toLowerCase() : "";
            String clsName = current.getClass().getName().toLowerCase();
            if (clsName.contains("authenticationfailed") || msg.contains("535") || msg.contains("authentication failed") || msg.contains("badcredentials") || msg.contains("username and password not accepted")) {
                return "Failed to authenticate with mail server. Please verify that your Google account has 2-Step Verification enabled and you are using a valid 16-character Google App Password (without spaces) in MAIL_PASSWORD.";
            }
            if (clsName.contains("connectexception") || clsName.contains("sockettimeoutexception") || msg.contains("connection timed out") || msg.contains("connect timed out") || msg.contains("connection refused")) {
                return "Failed to deliver OTP email: Connection to mail server (smtp.gmail.com:587) timed out or was refused. Please check your internet connection and firewall.";
            }
            if (clsName.contains("sslhandshake") || msg.contains("pkix") || msg.contains("could not convert socket to tls") || msg.contains("handshake_failure")) {
                return "Failed to deliver OTP email: SSL/TLS handshake failed with mail server. Ensure valid TLS/SSL settings and certificates.";
            }
            current = current.getCause();
        }
        return null;
    }


    public String getLatestDevOtp(String email) {
        return email != null ? devSimulationOtps.get(email.trim().toLowerCase()) : null;
    }

    private String buildHtmlEmailContent(String otp, String recipientEmail) {
        return "<!DOCTYPE html>"
                + "<html lang=\"en\">"
                + "<head><meta charset=\"UTF-8\"></head>"
                + "<body style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f1f5f9; margin: 0; padding: 24px 0;\">"
                + "  <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">"
                + "    <tr>"
                + "      <td align=\"center\">"
                + "        <table width=\"540\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #ffffff; border-radius: 12px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.05);\">"
                + "          <tr>"
                + "            <td style=\"background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%); padding: 24px; text-align: center;\">"
                + "              <h1 style=\"color: #ffffff; margin: 0; font-size: 22px; font-weight: 700; letter-spacing: -0.02em;\">Hostel Management System</h1>"
                + "              <p style=\"color: #bfdbfe; margin: 4px 0 0; font-size: 13px;\">Student Registration Verification</p>"
                + "            </td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style=\"padding: 32px 28px;\">"
                + "              <p style=\"font-size: 15px; color: #1e293b; margin: 0 0 16px;\">Hello,</p>"
                + "              <p style=\"font-size: 14px; color: #475569; margin: 0 0 24px; line-height: 1.5;\">"
                + "                Thank you for creating an account with <strong>Hostel Management System</strong>. "
                + "Please use the following 6-digit verification code to complete your email ownership verification:"
                + "              </p>"
                + "              <div style=\"text-align: center; margin: 28px 0;\">"
                + "                <div style=\"display: inline-block; font-size: 34px; font-weight: 800; font-family: 'Courier New', Courier, monospace; letter-spacing: 8px; color: #2563eb; background-color: #eff6ff; padding: 14px 28px; border-radius: 8px; border: 2px dashed #93c5fd;\">"
                + "                  " + escapeHtml(otp)
                + "                </div>"
                + "              </div>"
                + "              <div style=\"background-color: #f8fafc; border-radius: 8px; padding: 14px 16px; border-left: 4px solid #3b82f6; margin-bottom: 24px;\">"
                + "                <p style=\"font-size: 13px; color: #475569; margin: 0 0 6px;\">"
                + "                  ⏳ <strong>Valid for " + expiryMinutes + " minutes.</strong> Single-use code."
                + "                </p>"
                + "                <p style=\"font-size: 13px; color: #64748b; margin: 0;\">"
                + "                  Security Notice: Do not share this OTP with anyone. If you did not initiate this registration, please ignore this email."
                + "                </p>"
                + "              </div>"
                + "              <p style=\"font-size: 13px; color: #64748b; margin: 0;\">"
                + "                Best regards,<br/>"
                + "                <strong>Hostel Management System Team</strong>"
                + "              </p>"
                + "            </td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style=\"background-color: #f8fafc; padding: 16px; text-align: center; border-top: 1px solid #e2e8f0;\">"
                + "              <p style=\"font-size: 12px; color: #94a3b8; margin: 0;\">"
                + "                This is an automated system email. Please do not reply directly to this message."
                + "              </p>"
                + "            </td>"
                + "          </tr>"
                + "        </table>"
                + "      </td>"
                + "    </tr>"
                + "  </table>"
                + "</body>"
                + "</html>";
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    @Scheduled(cron = "${app.otp.cleanup-cron:0 0 2 * * ?}")
    @Transactional
    public void cleanupExpiredRecords() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        int deleted = verificationRepository.deleteExpiredRecordsOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} expired/used email verification records older than {}", deleted, cutoff);
        }
    }

    private String generateSecure6DigitOtp() {
        int code = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.format("%06d", code);
    }

    private String hashWithPepper(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = value + ":" + hashPepper;
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIdx = email.indexOf('@');
        if (atIdx <= 1) {
            return email;
        }
        String local = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (local.length() <= 2) {
            return local.charAt(0) + "*" + domain;
        }
        return local.charAt(0) + "*****" + domain;
    }
}
