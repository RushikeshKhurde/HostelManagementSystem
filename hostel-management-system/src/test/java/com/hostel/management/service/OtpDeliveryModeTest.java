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
    private ProviderIndependentSmsService smsService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService();
        smsService = new ProviderIndependentSmsService();
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

    @Test
    @DisplayName("SMS in simulation mode should store dev OTP with masked number")
    void testSmsSimulationMode() {
        ReflectionTestUtils.setField(smsService, "smsMode", "simulation");
        assertFalse(smsService.isRealMode());

        smsService.sendOtp("9876543210", "654321", "Password Reset");
        assertEquals("654321", smsService.getLatestDevOtp("9876543210"));
    }

    @Test
    @DisplayName("SMS in real mode with Twilio but missing credentials should throw 503 error")
    void testTwilioRealModeUnconfigured() {
        ReflectionTestUtils.setField(smsService, "smsMode", "real");
        ReflectionTestUtils.setField(smsService, "smsProvider", "twilio");
        ReflectionTestUtils.setField(smsService, "twilioAccountSid", "");
        ReflectionTestUtils.setField(smsService, "twilioAuthToken", "");
        ReflectionTestUtils.setField(smsService, "twilioPhoneNumber", "");
        assertTrue(smsService.isRealMode());

        ApiException ex = assertThrows(ApiException.class, () ->
                smsService.sendOtp("9876543210", "654321", "Password Reset")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        assertEquals("Mobile OTP service is not configured. Please contact the administrator.", ex.getMessage());
    }

    @Test
    @DisplayName("SMS in real mode with Fast2SMS but missing credentials should throw 503 error")
    void testFast2SmsRealModeUnconfigured() {
        ReflectionTestUtils.setField(smsService, "smsMode", "real");
        ReflectionTestUtils.setField(smsService, "smsProvider", "fast2sms");
        ReflectionTestUtils.setField(smsService, "fast2smsApiKey", "");
        assertTrue(smsService.isRealMode());

        ApiException ex = assertThrows(ApiException.class, () ->
                smsService.sendOtp("9876543210", "654321", "Password Reset")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        assertEquals("Mobile OTP service is not configured. Please contact the administrator.", ex.getMessage());
    }

    @Test
    @DisplayName("SMS invalid mobile number format should throw 400 error")
    void testInvalidMobileNumberFormat() {
        ApiException ex1 = assertThrows(ApiException.class, () ->
                smsService.sendOtp("12345", "654321", "Test")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatus());

        ApiException ex2 = assertThrows(ApiException.class, () ->
                smsService.sendOtp("abcdefghij", "654321", "Test")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatus());
    }

    @Test
    @DisplayName("SMS accepts valid 10-digit Indian numbers starting with 6, 7, 8, 9")
    void testValidIndianMobileNumbers() {
        ReflectionTestUtils.setField(smsService, "smsMode", "simulation");

        assertDoesNotThrow(() -> smsService.sendOtp("9876543210", "111111", "Test"));
        assertDoesNotThrow(() -> smsService.sendOtp("+919876543210", "222222", "Test"));
        assertDoesNotThrow(() -> smsService.sendOtp("09876543210", "333333", "Test"));
        assertDoesNotThrow(() -> smsService.sendOtp("8123456789", "444444", "Test"));
        assertDoesNotThrow(() -> smsService.sendOtp("7123456789", "555555", "Test"));
        assertDoesNotThrow(() -> smsService.sendOtp("6123456789", "666666", "Test"));
    }
}
