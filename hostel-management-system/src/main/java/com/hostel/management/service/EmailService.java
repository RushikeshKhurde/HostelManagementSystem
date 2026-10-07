package com.hostel.management.service;

import com.hostel.management.exception.ApiException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.otp.email.mode:simulation}")
    private String emailMode;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    // For local development / verification testing in simulation mode
    private final Map<String, String> latestDevOtps = new ConcurrentHashMap<>();

    public boolean isRealMode() {
        if (emailMode == null) return false;
        String mode = emailMode.trim().toLowerCase();
        return "real".equals(mode) || "smtp".equals(mode);
    }

    public void sendOtpEmail(String toEmail, String otp, String purpose) {
        String normalizedEmail = toEmail != null ? toEmail.trim().toLowerCase() : "";

        if (isRealMode()) {
            log.info("Sending email-change OTP to: {}", maskEmail(normalizedEmail));

            // Validate SMTP configuration
            if (mailSender == null || isBlank(mailHost) || isBlank(mailUsername) || isBlank(mailPassword)) {
                log.warn("Email service is configured in SMTP/real mode (OTP_EMAIL_MODE={}), but SMTP credentials are not configured. MAIL_HOST={}, MAIL_PORT={}, MAIL_USERNAME={}, MAIL_PASSWORD is {}",
                        emailMode, isBlank(mailHost) ? "MISSING" : mailHost, mailPort, isBlank(mailUsername) ? "MISSING" : mailUsername, isBlank(mailPassword) ? "MISSING" : "PRESENT");
                throw new ApiException("Unable to send OTP email. Please check email configuration and try again.", HttpStatus.SERVICE_UNAVAILABLE);
            }

            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                helper.setFrom(mailUsername, "Smart Hostel Management System");
                helper.setTo(normalizedEmail);
                helper.setSubject("Hostel Management System - " + purpose + " Verification Code");
                helper.setText(buildHtmlEmail(purpose, otp, normalizedEmail), true);

                mailSender.send(mimeMessage);
                log.info("OTP email successfully dispatched via SMTP to {}", maskEmail(normalizedEmail));
                return;
            } catch (ApiException ae) {
                throw ae;
            } catch (org.springframework.mail.MailAuthenticationException mae) {
                log.error("SMTP Authentication failed when dispatching to {}: {}", maskEmail(normalizedEmail), mae.getMessage());
                throw new ApiException("Unable to send OTP email. SMTP authentication failed. Please check your credentials.", HttpStatus.UNAUTHORIZED);
            } catch (Exception e) {
                log.error("Failed to dispatch live email to {}. Error: {}", maskEmail(normalizedEmail), e.getMessage());
                throw new ApiException("Unable to send OTP email. Please check email configuration and try again.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        // Development / simulation mode
        latestDevOtps.put(normalizedEmail, otp);

        log.info("==================================================");
        log.info("[EMAIL SERVICE - LOCAL/DEV SIMULATION MODE]");
        log.info("To: {}", maskEmail(normalizedEmail));
        log.info("Purpose: {}", purpose);
        log.info("OTP Code: [PROTECTED]");
        log.info("Expiry: 5 minutes");
        log.info("Note: Set OTP_EMAIL_MODE=smtp (or real) and configure MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD to enable live delivery.");
        log.info("==================================================");
    }

    public String getLatestDevOtp(String email) {
        return email != null ? latestDevOtps.get(email.trim().toLowerCase()) : null;
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIndex = email.indexOf('@');
        String user = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (user.length() <= 1) {
            return user + "***" + domain;
        }
        return user.charAt(0) + "***" + domain;
    }

    private String buildHtmlEmail(String purpose, String otp, String recipientEmail) {
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
                + "              <p style=\"color: #bfdbfe; margin: 4px 0 0; font-size: 13px;\">Secure Student Verification</p>"
                + "            </td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style=\"padding: 32px 28px;\">"
                + "              <p style=\"font-size: 15px; color: #1e293b; margin: 0 0 16px;\">Hello,</p>"
                + "              <p style=\"font-size: 14px; color: #475569; margin: 0 0 24px; line-height: 1.5;\">"
                + "                Your one-time verification code (OTP) for <strong>" + escapeHtml(purpose) + "</strong>"
                + (recipientEmail != null && !recipientEmail.isEmpty() ? " to <strong>" + escapeHtml(recipientEmail) + "</strong>" : "")
                + " is:"
                + "              </p>"
                + "              <div style=\"text-align: center; margin: 28px 0;\">"
                + "                <div style=\"display: inline-block; font-size: 34px; font-weight: 800; font-family: 'Courier New', Courier, monospace; letter-spacing: 8px; color: #2563eb; background-color: #eff6ff; padding: 14px 28px; border-radius: 8px; border: 2px dashed #93c5fd;\">"
                + "                  " + escapeHtml(otp)
                + "                </div>"
                + "              </div>"
                + "              <div style=\"background-color: #f8fafc; border-radius: 8px; padding: 14px 16px; border-left: 4px solid #3b82f6; margin-bottom: 24px;\">"
                + "                <p style=\"font-size: 13px; color: #475569; margin: 0 0 6px;\">"
                + "                  ⏳ <strong>Valid for 5 minutes.</strong> Single-use code."
                + "                </p>"
                + "                <p style=\"font-size: 13px; color: #64748b; margin: 0;\">"
                + "                  If you did not request this verification, please ignore this email. No changes will be made to your account."
                + "                </p>"
                + "              </div>"
                + "              <p style=\"font-size: 13px; color: #64748b; margin: 0;\">"
                + "                Best regards,<br/>"
                + "                <strong>Smart Hostel Management Team</strong>"
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
}
