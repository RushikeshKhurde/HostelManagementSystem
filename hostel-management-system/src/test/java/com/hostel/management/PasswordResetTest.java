package com.hostel.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.management.dto.*;
import com.hostel.management.model.PasswordResetOtp;
import com.hostel.management.model.Role;
import com.hostel.management.model.User;
import com.hostel.management.model.VerificationChannel;
import com.hostel.management.repository.PasswordResetOtpRepository;
import com.hostel.management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PasswordResetTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetOtpRepository otpRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JavaMailSender mailSender;

    private User testStudent;
    private String studentEmail;
    private String studentMobile;

    @BeforeEach
    void setUp() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        long ts = System.currentTimeMillis();
        studentEmail = "student_" + ts + "@hostel.com";
        studentMobile = "9" + String.valueOf(ts).substring(String.valueOf(ts).length() - 9);

        testStudent = userRepository.save(User.builder()
                .fullName("Reset Test Student")
                .username("student_reset_" + ts)
                .email(studentEmail)
                .mobileNumber(studentMobile)
                .password(passwordEncoder.encode("OldPassword@123"))
                .role(Role.USER)
                .status("ACTIVE")
                .enabled(true)
                .build());
    }

    private String extractOtpFromMail() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, atLeastOnce()).send(captor.capture());
        String body = captor.getValue().getText();
        assertNotNull(body);
        Pattern p = Pattern.compile("\\b\\d{6}\\b");
        Matcher m = p.matcher(body);
        assertTrue(m.find(), "OTP not found in email body");
        return m.group();
    }

    @Test
    @DisplayName("1. Valid email OTP request dispatches email, masks destination, and does NOT return OTP")
    void testValidEmailOtpRequest() throws Exception {
        ForgotPasswordRequest req = ForgotPasswordRequest.builder()
                .method(VerificationChannel.EMAIL)
                .identifier(studentEmail)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/forgot-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.destinationMasked").value(containsString("@hostel.com")))
                .andExpect(jsonPath("$.message").value(containsString("account exists")))
                .andExpect(jsonPath("$.otp").doesNotExist())
                .andReturn();

        ForgotPasswordResponse res = objectMapper.readValue(result.getResponse().getContentAsString(), ForgotPasswordResponse.class);
        assertNotNull(res.getRequestId());

        // Verify DB contains hashed OTP and NEVER plaintext OTP
        PasswordResetOtp savedOtp = otpRepository.findByRequestId(res.getRequestId()).orElseThrow();
        assertEquals(64, savedOtp.getOtpHash().length());
        assertFalse(savedOtp.getOtpHash().matches("^[0-9]{6}$"));
    }

    @Test
    @DisplayName("2. Unknown email returns generic response without leaking account existence")
    void testUnknownEmailReturnsGenericResponse() throws Exception {
        ForgotPasswordRequest req = ForgotPasswordRequest.builder()
                .method(VerificationChannel.EMAIL)
                .identifier("nonexistent_user_9999@domain.com")
                .build();

        mockMvc.perform(post("/api/auth/forgot-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").doesNotExist())
                .andExpect(jsonPath("$.message").value(containsString("account exists")));
    }

    @Test
    @DisplayName("3. Expired OTP cannot be verified")
    void testExpiredOtpFails() throws Exception {
        ForgotPasswordRequest req = ForgotPasswordRequest.builder()
                .method(VerificationChannel.EMAIL)
                .identifier(studentEmail)
                .build();

        MvcResult requestResult = mockMvc.perform(post("/api/auth/forgot-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        ForgotPasswordResponse requestRes = objectMapper.readValue(
                requestResult.getResponse().getContentAsString(), ForgotPasswordResponse.class
        );
        String plainOtp = extractOtpFromMail();

        // Expire the OTP record in the database
        PasswordResetOtp otpEntity = otpRepository.findByRequestId(requestRes.getRequestId()).orElseThrow();
        otpEntity.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        otpRepository.save(otpEntity);

        VerifyOtpRequest verifyReq = VerifyOtpRequest.builder()
                .requestId(requestRes.getRequestId())
                .otp(plainOtp)
                .build();

        mockMvc.perform(post("/api/auth/forgot-password/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("expired")));
    }

    @Test
    @DisplayName("4. Incorrect OTP returns 400 with remaining attempts counter")
    void testIncorrectOtpFails() throws Exception {
        ForgotPasswordRequest req = ForgotPasswordRequest.builder()
                .method(VerificationChannel.EMAIL)
                .identifier(studentEmail)
                .build();

        MvcResult requestResult = mockMvc.perform(post("/api/auth/forgot-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        ForgotPasswordResponse requestRes = objectMapper.readValue(
                requestResult.getResponse().getContentAsString(), ForgotPasswordResponse.class
        );

        VerifyOtpRequest verifyReq = VerifyOtpRequest.builder()
                .requestId(requestRes.getRequestId())
                .otp("000000")
                .build();

        mockMvc.perform(post("/api/auth/forgot-password/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("4 attempt(s) remaining")));
    }

    @Test
    @DisplayName("5. 5 failed OTP attempts invalidates the OTP and returns 429")
    void testFiveFailedAttemptsInvalidateOtp() throws Exception {
        ForgotPasswordRequest req = ForgotPasswordRequest.builder()
                .method(VerificationChannel.EMAIL)
                .identifier(studentEmail)
                .build();

        MvcResult requestResult = mockMvc.perform(post("/api/auth/forgot-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        ForgotPasswordResponse requestRes = objectMapper.readValue(
                requestResult.getResponse().getContentAsString(), ForgotPasswordResponse.class
        );

        VerifyOtpRequest wrongOtpReq = VerifyOtpRequest.builder()
                .requestId(requestRes.getRequestId())
                .otp("111111")
                .build();

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/forgot-password/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongOtpReq)))
                    .andExpect(status().isBadRequest());
        }

        // 5th attempt must return 429 Too Many Requests
        mockMvc.perform(post("/api/auth/forgot-password/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongOtpReq)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(containsString("Too many incorrect attempts")));
    }

    @Test
    @DisplayName("6. OTP cannot be verified again after successful verification")
    void testOtpCannotBeReusedAfterVerification() throws Exception {
        ForgotPasswordRequest req = ForgotPasswordRequest.builder()
                .method(VerificationChannel.EMAIL)
                .identifier(studentEmail)
                .build();

        MvcResult requestResult = mockMvc.perform(post("/api/auth/forgot-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        ForgotPasswordResponse requestRes = objectMapper.readValue(
                requestResult.getResponse().getContentAsString(), ForgotPasswordResponse.class
        );
        String plainOtp = extractOtpFromMail();

        VerifyOtpRequest verifyReq = VerifyOtpRequest.builder()
                .requestId(requestRes.getRequestId())
                .otp(plainOtp)
                .build();

        // First verification succeeds
        mockMvc.perform(post("/api/auth/forgot-password/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));

        // Second verification attempt with same OTP is rejected
        mockMvc.perform(post("/api/auth/forgot-password/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already been verified")));
    }

    @Test
    @DisplayName("7. Resend OTP generates new code after cooldown and resets attempts")
    void testResendOtpGeneratesNewOtpAndSucceeds() throws Exception {
        ForgotPasswordRequest req = ForgotPasswordRequest.builder()
                .method(VerificationChannel.EMAIL)
                .identifier(studentEmail)
                .build();

        MvcResult requestResult = mockMvc.perform(post("/api/auth/forgot-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        ForgotPasswordResponse requestRes = objectMapper.readValue(
                requestResult.getResponse().getContentAsString(), ForgotPasswordResponse.class
        );
        String firstOtp = extractOtpFromMail();

        // Simulate cooldown elapsed
        PasswordResetOtp otpEntity = otpRepository.findByRequestId(requestRes.getRequestId()).orElseThrow();
        otpEntity.setLastResendAt(LocalDateTime.now().minusSeconds(65));
        otpEntity.setAttemptCount(3);
        otpRepository.save(otpEntity);

        ResendOtpRequest resendReq = ResendOtpRequest.builder()
                .requestId(requestRes.getRequestId())
                .build();

        mockMvc.perform(post("/api/auth/forgot-password/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("A new OTP has been sent")));

        // Verify attempt count was reset
        PasswordResetOtp updated = otpRepository.findByRequestId(requestRes.getRequestId()).orElseThrow();
        assertEquals(0, updated.getAttemptCount());
        assertEquals(1, updated.getResendCount());
    }

    @Test
    @DisplayName("8. Resend OTP enforces 60-second cooldown period")
    void testResendCooldownEnforced() throws Exception {
        ForgotPasswordRequest req = ForgotPasswordRequest.builder()
                .method(VerificationChannel.EMAIL)
                .identifier(studentEmail)
                .build();

        MvcResult requestResult = mockMvc.perform(post("/api/auth/forgot-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        ForgotPasswordResponse requestRes = objectMapper.readValue(
                requestResult.getResponse().getContentAsString(), ForgotPasswordResponse.class
        );

        ResendOtpRequest resendReq = ResendOtpRequest.builder()
                .requestId(requestRes.getRequestId())
                .build();

        // Immediate resend must be rejected with 429 Too Many Requests
        mockMvc.perform(post("/api/auth/forgot-password/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(containsString("Please wait")));
    }

    @Test
    @DisplayName("9. Complete password reset flow: request -> verify -> reset -> login")
    void testCompletePasswordResetFlow() throws Exception {
        // Step 1: Request OTP
        ForgotPasswordRequest req = ForgotPasswordRequest.builder()
                .method(VerificationChannel.EMAIL)
                .identifier(studentEmail)
                .build();

        MvcResult requestResult = mockMvc.perform(post("/api/auth/forgot-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        ForgotPasswordResponse requestRes = objectMapper.readValue(
                requestResult.getResponse().getContentAsString(), ForgotPasswordResponse.class
        );
        String requestId = requestRes.getRequestId();
        String plainOtp = extractOtpFromMail();

        // Step 2: Verify OTP
        VerifyOtpRequest verifyReq = VerifyOtpRequest.builder()
                .requestId(requestId)
                .otp(plainOtp)
                .build();

        MvcResult verifyResult = mockMvc.perform(post("/api/auth/forgot-password/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.resetToken").isString())
                .andExpect(jsonPath("$.token").doesNotExist()) // Must NOT return JWT auth token
                .andReturn();

        VerifyOtpResponse verifyRes = objectMapper.readValue(
                verifyResult.getResponse().getContentAsString(), VerifyOtpResponse.class
        );
        String resetToken = verifyRes.getResetToken();

        // Step 3: Reset password
        ResetPasswordRequest resetReq = ResetPasswordRequest.builder()
                .resetToken(resetToken)
                .newPassword("NewPassword@123")
                .confirmPassword("NewPassword@123")
                .build();

        mockMvc.perform(post("/api/auth/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("Password reset successful")));

        // Step 4: Login with old password fails
        LoginRequest oldLoginReq = new LoginRequest();
        oldLoginReq.setUsername(testStudent.getUsername());
        oldLoginReq.setPassword("OldPassword@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLoginReq)))
                .andExpect(status().isUnauthorized());

        // Step 5: Login with new password succeeds
        LoginRequest newLoginReq = new LoginRequest();
        newLoginReq.setUsername(testStudent.getUsername());
        newLoginReq.setPassword("NewPassword@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLoginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    @DisplayName("10. Invalid reset token is rejected")
    void testInvalidResetTokenRejected() throws Exception {
        ResetPasswordRequest resetReq = ResetPasswordRequest.builder()
                .resetToken("invalid-fake-token-12345")
                .newPassword("NewPassword@123")
                .confirmPassword("NewPassword@123")
                .build();

        mockMvc.perform(post("/api/auth/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid or expired")));
    }

    @Test
    @DisplayName("11. Expired reset token is rejected")
    void testExpiredResetTokenRejected() throws Exception {
        ForgotPasswordRequest req = ForgotPasswordRequest.builder()
                .method(VerificationChannel.EMAIL)
                .identifier(studentEmail)
                .build();

        MvcResult requestResult = mockMvc.perform(post("/api/auth/forgot-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        ForgotPasswordResponse requestRes = objectMapper.readValue(
                requestResult.getResponse().getContentAsString(), ForgotPasswordResponse.class
        );
        String plainOtp = extractOtpFromMail();

        VerifyOtpRequest verifyReq = VerifyOtpRequest.builder()
                .requestId(requestRes.getRequestId())
                .otp(plainOtp)
                .build();

        MvcResult verifyResult = mockMvc.perform(post("/api/auth/forgot-password/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andReturn();

        VerifyOtpResponse verifyRes = objectMapper.readValue(
                verifyResult.getResponse().getContentAsString(), VerifyOtpResponse.class
        );

        // Expire the reset token in DB
        PasswordResetOtp otpEntity = otpRepository.findByResetToken(verifyRes.getResetToken()).orElseThrow();
        otpEntity.setResetTokenExpiresAt(LocalDateTime.now().minusMinutes(1));
        otpRepository.save(otpEntity);

        ResetPasswordRequest resetReq = ResetPasswordRequest.builder()
                .resetToken(verifyRes.getResetToken())
                .newPassword("NewPassword@123")
                .confirmPassword("NewPassword@123")
                .build();

        mockMvc.perform(post("/api/auth/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("expired")));
    }

    @Test
    @DisplayName("12. Passwords in database are strictly BCrypt-hashed")
    void testPasswordIsHashedWithBCryptInDatabase() {
        User user = userRepository.findByEmail(studentEmail).orElseThrow();
        assertNotNull(user.getPassword());
        assertTrue(user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$"));
        assertFalse(user.getPassword().contains("OldPassword"));
        assertTrue(passwordEncoder.matches("OldPassword@123", user.getPassword()));
    }

    @Test
    @DisplayName("13. Weak or mismatched passwords are rejected during reset")
    void testWeakOrMismatchedPasswordRejected() throws Exception {
        ForgotPasswordRequest req = ForgotPasswordRequest.builder()
                .method(VerificationChannel.EMAIL)
                .identifier(studentEmail)
                .build();

        MvcResult requestResult = mockMvc.perform(post("/api/auth/forgot-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        ForgotPasswordResponse requestRes = objectMapper.readValue(
                requestResult.getResponse().getContentAsString(), ForgotPasswordResponse.class
        );
        String plainOtp = extractOtpFromMail();

        VerifyOtpRequest verifyReq = VerifyOtpRequest.builder()
                .requestId(requestRes.getRequestId())
                .otp(plainOtp)
                .build();

        MvcResult verifyResult = mockMvc.perform(post("/api/auth/forgot-password/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andReturn();

        VerifyOtpResponse verifyRes = objectMapper.readValue(
                verifyResult.getResponse().getContentAsString(), VerifyOtpResponse.class
        );
        String resetToken = verifyRes.getResetToken();

        // Mismatched passwords
        ResetPasswordRequest mismatchReq = ResetPasswordRequest.builder()
                .resetToken(resetToken)
                .newPassword("Password@123")
                .confirmPassword("DifferentPassword@123")
                .build();

        mockMvc.perform(post("/api/auth/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mismatchReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Passwords do not match")));

        // Weak password (fails regex)
        ResetPasswordRequest weakReq = ResetPasswordRequest.builder()
                .resetToken(resetToken)
                .newPassword("simple")
                .confirmPassword("simple")
                .build();

        mockMvc.perform(post("/api/auth/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(weakReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Password must be 8-20 characters")));
    }

    @Test
    @DisplayName("14. Authorization check: Forgot password endpoints are publicly accessible, while secured endpoints require auth")
    void testAuthorizationRegression() throws Exception {
        // Protected endpoint without token returns 401
        mockMvc.perform(get("/api/dashboard/student"))
                .andExpect(status().isUnauthorized());

        // Forgot password endpoint is publicly accessible
        ForgotPasswordRequest req = ForgotPasswordRequest.builder()
                .method(VerificationChannel.EMAIL)
                .identifier("any_unregistered_email@domain.com")
                .build();

        mockMvc.perform(post("/api/auth/forgot-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
