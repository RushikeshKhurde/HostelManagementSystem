package com.hostel.management;

import com.hostel.management.config.DotenvLoader;
import com.hostel.management.model.Role;
import com.hostel.management.model.User;
import com.hostel.management.service.delivery.EmailOtpDeliveryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class EmailOtpDeliveryLiveTest {

    @Test
    @DisplayName("Verify real email delivery through Gmail SMTP using .env credentials")
    void testLiveEmailOtpDeliveryWithEnvCredentials() {
        Map<String, String> env = DotenvLoader.load();
        String mailUser = env.get("MAIL_USERNAME");
        String mailPass = env.get("MAIL_PASSWORD");
        String mailHost = env.getOrDefault("MAIL_HOST", "smtp.gmail.com");
        String mailPort = env.getOrDefault("MAIL_PORT", "587");

        // Only run live test if valid credentials are present and not placeholder
        assumeTrue(mailUser != null && !mailUser.isBlank() && !mailUser.contains("your.email"), "Valid Gmail username not set");
        assumeTrue(mailPass != null && !mailPass.isBlank() && !mailPass.contains("your16char"), "Valid Gmail password not set");

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(Integer.parseInt(mailPort));
        mailSender.setUsername(mailUser);
        mailSender.setPassword(mailPass);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        EmailOtpDeliveryService deliveryService = new EmailOtpDeliveryService(mailSender);
        ReflectionTestUtils.setField(deliveryService, "mailHost", mailHost);
        ReflectionTestUtils.setField(deliveryService, "mailUsername", mailUser);
        ReflectionTestUtils.setField(deliveryService, "mailPassword", mailPass);
        ReflectionTestUtils.setField(deliveryService, "fromEmail", mailUser);
        ReflectionTestUtils.setField(deliveryService, "expiryMinutes", 5);

        User recipient = User.builder()
                .id(1L)
                .fullName("Hostel Administrator")
                .email(mailUser)
                .role(Role.ADMIN)
                .build();

        // Dispatch verification OTP email to configured Gmail account
        assertDoesNotThrow(() -> {
            deliveryService.sendOtp(recipient, mailUser, "583921");
        });
    }
}
