package com.hostel.management.service;

import com.hostel.management.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class OtpDeliveryModeTest {

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService();
    }

    @Test
    @DisplayName("Email in simulation mode should store dev OTP without error")
    void testEmailSimulationMode() {
        ReflectionTestUtils.setField(emailService, "emailMode", "simulation");
        assertFalse(emailService.isRealMode());

        emailService.sendOtpEmail("test@college.edu", "123456", "Registration");
        assertEquals("123456", emailService.getLatestDevOtp("test@college.edu"));
    }

    @Test
    @DisplayName("Email in real mode without SMTP credentials should throw 503 error")
    void testEmailRealModeUnconfigured() {
        ReflectionTestUtils.setField(emailService, "emailMode", "real");
        ReflectionTestUtils.setField(emailService, "mailHost", "");
        ReflectionTestUtils.setField(emailService, "mailUsername", "");
        ReflectionTestUtils.setField(emailService, "mailPassword", "");
        assertTrue(emailService.isRealMode());

        ApiException ex = assertThrows(ApiException.class, () ->
                emailService.sendOtpEmail("test@college.edu", "123456", "Registration")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        assertEquals("Unable to send OTP email. Please check email configuration and try again.", ex.getMessage());
    }

    @Test
    @DisplayName("Email in smtp mode should be recognized as real mode and throw 503 if credentials missing")
    void testEmailSmtpModeUnconfigured() {
        ReflectionTestUtils.setField(emailService, "emailMode", "smtp");
        ReflectionTestUtils.setField(emailService, "mailHost", "");
        ReflectionTestUtils.setField(emailService, "mailUsername", "");
        ReflectionTestUtils.setField(emailService, "mailPassword", "");
        assertTrue(emailService.isRealMode());

        ApiException ex = assertThrows(ApiException.class, () ->
                emailService.sendOtpEmail("test@college.edu", "123456", "Email Address Change")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        assertEquals("Unable to send OTP email. Please check email configuration and try again.", ex.getMessage());
    }
}
