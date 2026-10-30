package com.hostel.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.management.dto.*;
import com.hostel.management.model.EmailVerification;
import com.hostel.management.model.Role;
import com.hostel.management.model.User;
import com.hostel.management.repository.EmailVerificationRepository;
import com.hostel.management.repository.UserRepository;
import com.hostel.management.service.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RegistrationEmailVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationRepository verificationRepository;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JavaMailSender mailSender;

    private String testEmail;
    private String testUsername;
    private String testMobile;

    @BeforeEach
    void setUp() {
        emailVerificationService.clearRateLimits();
        long ts = System.currentTimeMillis();
        testEmail = "newstudent_" + ts + "@hostel.com";
        testUsername = "student_reg_" + ts;
        testMobile = "9" + String.valueOf(ts).substring(String.valueOf(ts).length() - 9);
    }


    @Test
    @DisplayName("1. Valid email requests OTP: returns masked destination, does not return OTP or token, hashes OTP in DB")
    void testSendOtpSuccess() throws Exception {
        SendEmailVerificationOtpRequest req = SendEmailVerificationOtpRequest.builder()
                .email(testEmail)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/email-verification/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("Verification code sent")))
                .andExpect(jsonPath("$.email").value(testEmail))
                .andExpect(jsonPath("$.destinationMasked").value(containsString("@hostel.com")))
                .andExpect(jsonPath("$.expiresInSeconds").value(600))
                .andExpect(jsonPath("$.resendCooldownSeconds").value(60))
                .andExpect(jsonPath("$.otp").doesNotExist())
                .andExpect(jsonPath("$.registrationToken").doesNotExist())
                .andReturn();

        // Verify DB record
        EmailVerification saved = verificationRepository.findTopByEmailAndOtpUsedFalseOrderByCreatedAtDesc(testEmail).orElseThrow();
        assertEquals(64, saved.getOtpHash().length());
        assertFalse(saved.getOtpHash().matches("^\\d{6}$"));
        assertFalse(saved.isVerified());
        assertFalse(saved.isOtpUsed());
        assertFalse(saved.isTokenUsed());
        assertNull(saved.getRegistrationTokenHash());
    }

    @Test
    @DisplayName("2. Malformed email or uppercase email is rejected on send-otp")
    void testMalformedEmailRejected() throws Exception {
        // Uppercase
        SendEmailVerificationOtpRequest reqUpper = SendEmailVerificationOtpRequest.builder()
                .email("STUDENT@HOSTEL.COM")
                .build();

        mockMvc.perform(post("/api/auth/email-verification/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqUpper)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("lowercase")));

        // Invalid format
        SendEmailVerificationOtpRequest reqInvalid = SendEmailVerificationOtpRequest.builder()
                .email("not-an-email")
                .build();

        mockMvc.perform(post("/api/auth/email-verification/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqInvalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("valid email")));
    }

    @Test
    @DisplayName("3. Already registered email is rejected with 409 Conflict")
    void testAlreadyRegisteredEmailRejected() throws Exception {
        // Save existing user
        userRepository.save(User.builder()
                .fullName("Existing Student")
                .username("existing_" + System.currentTimeMillis())
                .email(testEmail)
                .mobileNumber(testMobile)
                .password(passwordEncoder.encode("Password@123"))
                .role(Role.USER)
                .status("ACTIVE")
                .enabled(true)
                .build());

        SendEmailVerificationOtpRequest req = SendEmailVerificationOtpRequest.builder()
                .email(testEmail)
                .build();

        mockMvc.perform(post("/api/auth/email-verification/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("already registered")));
    }

    @Test
    @DisplayName("4. Correct OTP verifies successfully and issues single-use registrationToken")
    void testVerifyOtpSuccess() throws Exception {
        // Step 1: Send OTP
        emailVerificationService.sendOtp(new SendEmailVerificationOtpRequest(testEmail), "127.0.0.1");
        String otp = emailVerificationService.getLatestDevOtp(testEmail);
        assertNotNull(otp);

        // Step 2: Verify OTP
        VerifyEmailOtpRequest verifyReq = VerifyEmailOtpRequest.builder()
                .email(testEmail)
                .otp(otp)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/email-verification/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.registrationToken").isString())
                .andExpect(jsonPath("$.expiresInSeconds").value(600))
                .andExpect(jsonPath("$.token").doesNotExist()) // Must NOT return JWT login token
                .andReturn();

        VerifyEmailOtpResponse res = objectMapper.readValue(result.getResponse().getContentAsString(), VerifyEmailOtpResponse.class);
        assertNotNull(res.getRegistrationToken());

        // Verify DB state
        EmailVerification updated = verificationRepository.findAll().stream()
                .filter(v -> v.getEmail().equals(testEmail))
                .findFirst().orElseThrow();
        assertTrue(updated.isVerified());
        assertTrue(updated.isOtpUsed());
        assertFalse(updated.isTokenUsed());
        assertNotNull(updated.getTokenExpiresAt());
        assertNotNull(updated.getRegistrationTokenHash());
    }


    @Test
    @DisplayName("5. Incorrect OTP returns 400 with remaining attempts count")
    void testIncorrectOtpFails() throws Exception {
        emailVerificationService.sendOtp(new SendEmailVerificationOtpRequest(testEmail), "127.0.0.1");

        VerifyEmailOtpRequest verifyReq = VerifyEmailOtpRequest.builder()
                .email(testEmail)
                .otp("000000")
                .build();

        mockMvc.perform(post("/api/auth/email-verification/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("4 attempt(s) remaining")));
    }

    @Test
    @DisplayName("6. 5 failed OTP attempts invalidates the verification code and returns 429 Too Many Requests")
    void testFiveFailedAttemptsInvalidateOtp() throws Exception {
        emailVerificationService.sendOtp(new SendEmailVerificationOtpRequest(testEmail), "127.0.0.1");

        VerifyEmailOtpRequest wrongReq = VerifyEmailOtpRequest.builder()
                .email(testEmail)
                .otp("111111")
                .build();

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/email-verification/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongReq)))
                    .andExpect(status().isBadRequest());
        }

        // 5th attempt triggers max attempts exceeded -> 429
        mockMvc.perform(post("/api/auth/email-verification/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongReq)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(containsString("Maximum verification attempts exceeded")));
    }

    @Test
    @DisplayName("7. Expired OTP is rejected during verification")
    void testExpiredOtpFails() throws Exception {
        emailVerificationService.sendOtp(new SendEmailVerificationOtpRequest(testEmail), "127.0.0.1");
        String otp = emailVerificationService.getLatestDevOtp(testEmail);

        // Expire in database
        EmailVerification entity = verificationRepository.findTopByEmailAndOtpUsedFalseOrderByCreatedAtDesc(testEmail).orElseThrow();
        entity.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        verificationRepository.save(entity);

        VerifyEmailOtpRequest verifyReq = VerifyEmailOtpRequest.builder()
                .email(testEmail)
                .otp(otp)
                .build();

        mockMvc.perform(post("/api/auth/email-verification/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("expired")));
    }

    @Test
    @DisplayName("8. Verified OTP cannot be verified a second time to generate another token")
    void testOtpCannotBeReused() throws Exception {
        emailVerificationService.sendOtp(new SendEmailVerificationOtpRequest(testEmail), "127.0.0.1");
        String otp = emailVerificationService.getLatestDevOtp(testEmail);

        VerifyEmailOtpRequest verifyReq = VerifyEmailOtpRequest.builder()
                .email(testEmail)
                .otp(otp)
                .build();

        // 1st verification succeeds
        mockMvc.perform(post("/api/auth/email-verification/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk());

        // 2nd verification fails
        mockMvc.perform(post("/api/auth/email-verification/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("No active verification code")));
    }

    @Test
    @DisplayName("9. Resend cooldown (60 seconds) is enforced")
    void testResendCooldownEnforced() throws Exception {
        SendEmailVerificationOtpRequest req = SendEmailVerificationOtpRequest.builder()
                .email(testEmail)
                .build();

        // First send
        mockMvc.perform(post("/api/auth/email-verification/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Immediate second send fails with 429
        mockMvc.perform(post("/api/auth/email-verification/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(containsString("Please wait")));
    }

    @Test
    @DisplayName("10. Full Registration Flow: Send OTP -> Verify -> Register -> Login")
    void testCompleteRegistrationFlow() throws Exception {
        // Step 1: Send OTP
        emailVerificationService.sendOtp(new SendEmailVerificationOtpRequest(testEmail), "127.0.0.1");
        String otp = emailVerificationService.getLatestDevOtp(testEmail);

        // Step 2: Verify OTP
        VerifyEmailOtpResponse verifyRes = emailVerificationService.verifyOtp(new VerifyEmailOtpRequest(testEmail, otp));
        String registrationToken = verifyRes.getRegistrationToken();
        assertNotNull(registrationToken);

        // Step 3: Register Account
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setFullName("Rahul Sharma");
        registerReq.setUsername(testUsername);
        registerReq.setEmail(testEmail);
        registerReq.setMobileNumber(testMobile);
        registerReq.setPassword("SecurePassword@123");
        registerReq.setConfirmPassword("SecurePassword@123");
        registerReq.setGender("Male");
        registerReq.setAddress("Hostel Block A, Room 101");
        registerReq.setRegistrationToken(registrationToken);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.studentId").value(startsWith("STU-")))
                .andExpect(jsonPath("$.emailVerified").value(true));

        // Verify DB User
        User created = userRepository.findByUsername(testUsername).orElseThrow();
        assertEquals(Role.USER, created.getRole());
        assertTrue(created.isEmailVerified());
        assertTrue(passwordEncoder.matches("SecurePassword@123", created.getPassword()));

        // Step 4: Login with created account succeeds
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername(testUsername);
        loginReq.setPassword("SecurePassword@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    @DisplayName("11. Registration without registrationToken is rejected with 400 Bad Request")
    void testRegistrationWithoutTokenRejected() throws Exception {
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setFullName("Rahul Sharma");
        registerReq.setUsername(testUsername);
        registerReq.setEmail(testEmail);
        registerReq.setMobileNumber(testMobile);
        registerReq.setPassword("SecurePassword@123");
        registerReq.setConfirmPassword("SecurePassword@123");
        registerReq.setRegistrationToken(null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Email verification token is required")));
    }

    @Test
    @DisplayName("12. Registration with invalid or fake registrationToken is rejected with 400 Bad Request")
    void testRegistrationWithFakeTokenRejected() throws Exception {
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setFullName("Rahul Sharma");
        registerReq.setUsername(testUsername);
        registerReq.setEmail(testEmail);
        registerReq.setMobileNumber(testMobile);
        registerReq.setPassword("SecurePassword@123");
        registerReq.setConfirmPassword("SecurePassword@123");
        registerReq.setRegistrationToken("fake-unverified-token-9999");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid or expired email verification token")));
    }

    @Test
    @DisplayName("13. Registration with token belonging to a different email address is rejected with 400 Bad Request")
    void testRegistrationWithMismatchedEmailTokenRejected() throws Exception {
        // Obtain token for email A
        String otherEmail = "other_student_" + System.currentTimeMillis() + "@hostel.com";
        emailVerificationService.sendOtp(new SendEmailVerificationOtpRequest(otherEmail), "127.0.0.1");
        String otp = emailVerificationService.getLatestDevOtp(otherEmail);
        VerifyEmailOtpResponse verifyRes = emailVerificationService.verifyOtp(new VerifyEmailOtpRequest(otherEmail, otp));

        // Attempt to register email B with email A's token
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setFullName("Rahul Sharma");
        registerReq.setUsername(testUsername);
        registerReq.setEmail(testEmail);
        registerReq.setMobileNumber(testMobile);
        registerReq.setPassword("SecurePassword@123");
        registerReq.setConfirmPassword("SecurePassword@123");
        registerReq.setRegistrationToken(verifyRes.getRegistrationToken());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("does not match the registration email")));
    }

    @Test
    @DisplayName("14. Registration token is single-use and cannot be used to register multiple accounts")
    void testTokenCannotBeReusedForSecondRegistration() throws Exception {
        emailVerificationService.sendOtp(new SendEmailVerificationOtpRequest(testEmail), "127.0.0.1");
        String otp = emailVerificationService.getLatestDevOtp(testEmail);
        VerifyEmailOtpResponse verifyRes = emailVerificationService.verifyOtp(new VerifyEmailOtpRequest(testEmail, otp));
        String token = verifyRes.getRegistrationToken();

        // 1st Registration succeeds
        RegisterRequest req1 = new RegisterRequest();
        req1.setFullName("Rahul Sharma");
        req1.setUsername(testUsername);
        req1.setEmail(testEmail);
        req1.setMobileNumber(testMobile);
        req1.setPassword("SecurePassword@123");
        req1.setRegistrationToken(token);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        // 2nd Registration with same token fails
        RegisterRequest req2 = new RegisterRequest();
        req2.setFullName("Another Student");
        req2.setUsername("another_" + System.currentTimeMillis());
        req2.setEmail(testEmail);
        req2.setMobileNumber("9888888888");
        req2.setPassword("SecurePassword@123");
        req2.setRegistrationToken(token);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid or expired email verification token")));
    }

    @Test
    @DisplayName("15. Expired registration token is rejected")
    void testExpiredRegistrationTokenRejected() throws Exception {
        emailVerificationService.sendOtp(new SendEmailVerificationOtpRequest(testEmail), "127.0.0.1");
        String otp = emailVerificationService.getLatestDevOtp(testEmail);
        VerifyEmailOtpResponse verifyRes = emailVerificationService.verifyOtp(new VerifyEmailOtpRequest(testEmail, otp));
        String token = verifyRes.getRegistrationToken();

        // Expire the token in DB
        EmailVerification entity = verificationRepository.findAll().stream()
                .filter(v -> v.getEmail().equals(testEmail))
                .findFirst().orElseThrow();
        entity.setTokenExpiresAt(LocalDateTime.now().minusMinutes(1));
        verificationRepository.save(entity);


        RegisterRequest req = new RegisterRequest();
        req.setFullName("Rahul Sharma");
        req.setUsername(testUsername);
        req.setEmail(testEmail);
        req.setMobileNumber(testMobile);
        req.setPassword("SecurePassword@123");
        req.setRegistrationToken(token);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("expired")));
    }

    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            String combined = token + ":DedicatedRegistrationPepperKeyMustBeAtLeast32Bytes!";
            byte[] hash = digest.digest(combined.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
