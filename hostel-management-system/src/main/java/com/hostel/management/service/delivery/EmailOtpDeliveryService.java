package com.hostel.management.service.delivery;

import com.hostel.management.exception.ApiException;
import com.hostel.management.model.User;
import com.hostel.management.model.VerificationChannel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailOtpDeliveryService implements OtpDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(EmailOtpDeliveryService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username:noreply@hostelmanagement.com}}")
    private String fromEmail;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost;

    @Value("${app.otp.expiry-minutes:5}")
    private int expiryMinutes;

    @PostConstruct
    public void validateConfiguration() {
        if (mailUsername == null || mailUsername.isBlank() || mailPassword == null || mailPassword.isBlank()) {
            log.warn("================================================================================");
            log.warn("[EMAIL OTP SERVICE] MAIL_USERNAME or MAIL_PASSWORD is not set in environment.");
            log.warn("Forgot Password Email OTP delivery will remain unavailable until valid SMTP");
            log.warn("credentials (e.g. Gmail 16-character App Password) are provided.");
            log.warn("================================================================================");
        } else {
            log.info("[EMAIL OTP SERVICE] Configured successfully for host '{}' with account '{}'",
                    mailHost, maskEmail(mailUsername));
        }
    }

    @Override
    public VerificationChannel getSupportedChannel() {
        return VerificationChannel.EMAIL;
    }

    @Override
    public void sendOtp(User user, String destination, String otp) {
        // Pre-flight check: produce a clear configuration error if credentials are empty
        if (mailUsername == null || mailUsername.isBlank() || mailPassword == null || mailPassword.isBlank()) {
            log.error("Email OTP delivery failed: Mail server credentials (MAIL_USERNAME / MAIL_PASSWORD) are not configured on the server.");
            throw new ApiException(
                    "Email delivery is currently not configured on the server. Please contact the administrator.",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            String effectiveSender = (fromEmail != null && !fromEmail.isBlank() && fromEmail.contains("@"))
                    ? fromEmail.trim()
                    : mailUsername.trim();
            message.setFrom(effectiveSender);
            message.setTo(destination);
            message.setSubject("Hostel Management System - Password Reset OTP");
            message.setText("Hello " + (user != null && user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : "User") + ",\n\n"
                    + "A request was received to reset the password for your Hostel Management System account.\n\n"
                    + "Your 6-digit verification code is:\n"
                    + otp + "\n\n"
                    + "This code will expire in " + expiryMinutes + " minutes.\n\n"
                    + "Security Notice:\n"
                    + "- Do not share this OTP with anyone. Hostel staff and administrators will never ask for your verification code.\n"
                    + "- If you did not request a password reset, please ignore this email or contact the hostel administrator immediately if you suspect unauthorized access.\n\n"
                    + "Regards,\n"
                    + "Hostel Management System Team");

            mailSender.send(message);
            log.info("Password reset OTP successfully dispatched via Email to recipient domain ending in @{}",
                    destination.contains("@") ? destination.substring(destination.indexOf("@") + 1) : "***");
        } catch (MailAuthenticationException ex) {
            log.error("SMTP authentication failed for host {}. Verify MAIL_USERNAME and MAIL_PASSWORD (e.g. Google 16-char App Password): {}",
                    mailHost, ex.getMessage());
            throw new ApiException("Failed to authenticate with mail server. Please verify that your Google account has 2-Step Verification enabled and you are using a valid 16-character Google App Password (without spaces).", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (MailException ex) {
            log.error("Failed to deliver password reset OTP email via {}: {}", mailHost, ex.getMessage(), ex);
            String diagnostic = extractDiagnosticMessage(ex);
            if (diagnostic != null) {
                throw new ApiException(diagnostic, HttpStatus.SERVICE_UNAVAILABLE);
            }
            throw new ApiException("Failed to deliver OTP email. Please ensure your mail server configuration and App Password are valid.", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception ex) {
            log.error("Unexpected error delivering password reset OTP email: {}", ex.getMessage(), ex);
            throw new ApiException("Failed to send OTP email. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
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

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf("@");
        if (at <= 2) {
            return email.charAt(0) + "***" + email.substring(at);
        }
        return email.substring(0, 2) + "***" + email.substring(at);
    }
}
