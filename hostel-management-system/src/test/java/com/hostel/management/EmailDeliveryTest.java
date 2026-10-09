package com.hostel.management;

import com.hostel.management.exception.ApiException;
import com.hostel.management.model.Role;
import com.hostel.management.model.User;
import com.hostel.management.service.delivery.EmailOtpDeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EmailDeliveryTest {

    private JavaMailSender mailSender;
    private EmailOtpDeliveryService emailService;
    private User testUser;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        emailService = new EmailOtpDeliveryService(mailSender);

        ReflectionTestUtils.setField(emailService, "mailHost", "smtp.gmail.com");
        ReflectionTestUtils.setField(emailService, "mailUsername", "admin@hostel.com");
        ReflectionTestUtils.setField(emailService, "mailPassword", "app-password-1234");
        ReflectionTestUtils.setField(emailService, "fromEmail", "admin@hostel.com");
        ReflectionTestUtils.setField(emailService, "expiryMinutes", 5);

        testUser = User.builder()
                .id(1L)
                .fullName("John Doe")
                .username("johndoe")
                .email("student@domain.com")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("Email OTP dispatch formats professional message with OTP, expiry, and security notice")
    void testEmailContentFormatting() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailService.sendOtp(testUser, "student@domain.com", "849201"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertEquals("admin@hostel.com", sent.getFrom());
        assertNotNull(sent.getTo());
        assertEquals("student@domain.com", sent.getTo()[0]);
        assertEquals("Hostel Management System - Password Reset OTP", sent.getSubject());

        String body = sent.getText();
        assertNotNull(body);
        assertTrue(body.contains("Hello John Doe"));
        assertTrue(body.contains("849201"));
        assertTrue(body.contains("expires in 5 minutes") || body.contains("expire in 5 minutes"));
        assertTrue(body.contains("Security Notice:"));
        assertTrue(body.contains("Do not share this OTP"));
        assertFalse(body.contains("password") && body.contains("Password@")); // Never exposes actual passwords
    }

    @Test
    @DisplayName("Missing MAIL_USERNAME or MAIL_PASSWORD produces clear server-side configuration error")
    void testMissingCredentialsThrowsConfigurationError() {
        ReflectionTestUtils.setField(emailService, "mailUsername", "");
        ReflectionTestUtils.setField(emailService, "mailPassword", "");

        ApiException ex = assertThrows(ApiException.class, () ->
                emailService.sendOtp(testUser, "student@domain.com", "123456")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        assertTrue(ex.getMessage().contains("Email delivery is currently not configured"));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("SMTP authentication failure (invalid App Password) produces 503 error without leaking credentials")
    void testSmtpAuthenticationFailure() {
        doThrow(new MailAuthenticationException("Authentication failed for user admin@hostel.com"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        ApiException ex = assertThrows(ApiException.class, () ->
                emailService.sendOtp(testUser, "student@domain.com", "123456")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        assertTrue(ex.getMessage().contains("Failed to authenticate with mail server"));
        assertFalse(ex.getMessage().contains("app-password-1234")); // Secrets never leaked
    }

    @Test
    @DisplayName("General mail server connection failure produces 503 error")
    void testMailServerConnectionFailure() {
        doThrow(new MailSendException("Connection refused to smtp.gmail.com:587"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        ApiException ex = assertThrows(ApiException.class, () ->
                emailService.sendOtp(testUser, "student@domain.com", "123456")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        assertTrue(ex.getMessage().contains("Failed to deliver OTP email"));
    }
}
