package com.hostel.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.management.dto.LoginRequest;
import com.hostel.management.dto.RegisterRequest;
import com.hostel.management.model.Role;
import com.hostel.management.model.User;
import com.hostel.management.repository.UserRepository;
import com.hostel.management.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthAndSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("Public registration always creates USER / STUDENT role")
    void testPublicRegistrationCreatesStudent() throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uniqueUser = "testuser_" + timestamp;
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Test Student");
        req.setUsername(uniqueUser);
        req.setEmail(uniqueUser + "@example.com");
        req.setMobileNumber("9" + timestamp.substring(timestamp.length() - 9));
        req.setPassword("Password@123");
        req.setConfirmPassword("Password@123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.token").isString());

        User created = userRepository.findByUsername(uniqueUser).orElseThrow();
        assertEquals(Role.USER, created.getRole());
    }

    @Test
    @DisplayName("Login succeeds with valid credentials")
    void testLoginSuccess() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("Admin");
        req.setPassword("Admin123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Login fails with invalid credentials and returns 401")
    void testLoginInvalidCredentials() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("Admin");
        req.setPassword("WrongPassword@999");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint returns 401 via JwtAuthenticationEntryPoint")
    void testUnauthenticatedProtectedEndpointReturns401() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(containsString("Authentication required")));
    }

    @Test
    @DisplayName("Student token attempting to access Admin endpoint returns 403 Forbidden")
    void testStudentCannotAccessAdminEndpoint() throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String username = "student_auth_" + timestamp;
        userRepository.save(User.builder()
                .fullName("Auth Student")
                .username(username)
                .email(username + "@test.com")
                .mobileNumber("9" + timestamp.substring(timestamp.length() - 9))
                .password("Password@123")
                .role(Role.USER)
                .build());

        String studentToken = jwtUtil.generateToken(username, "USER");

        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(containsString("Access denied")));
    }

    @Test
    @DisplayName("Warden token can access /api/payments and student token is forbidden (403)")
    void testWardenPaymentAuthorizationAndStudentForbidden() throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String wardenUser = "warden_" + timestamp;
        userRepository.save(User.builder()
                .fullName("Warden Staff")
                .username(wardenUser)
                .email(wardenUser + "@test.com")
                .mobileNumber("9" + timestamp.substring(timestamp.length() - 9))
                .password("Password@123")
                .role(Role.WARDEN)
                .build());

        String studentUser = "student_pauth_" + timestamp;
        userRepository.save(User.builder()
                .fullName("Pay Student")
                .username(studentUser)
                .email(studentUser + "@test.com")
                .mobileNumber("8" + timestamp.substring(timestamp.length() - 9))
                .password("Password@123")
                .role(Role.USER)
                .build());

        String wardenToken = jwtUtil.generateToken(wardenUser, "WARDEN");
        String studentToken = jwtUtil.generateToken(studentUser, "USER");

        // Warden can access /api/payments
        mockMvc.perform(get("/api/payments")
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Student cannot access /api/payments (all payments)
        mockMvc.perform(get("/api/payments")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Student cannot confirm cash payment (403 Forbidden)")
    void testStudentCannotConfirmCashPayment() throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String studentUser = "student_cnf_" + timestamp;
        userRepository.save(User.builder()
                .fullName("Confirm Student")
                .username(studentUser)
                .email(studentUser + "@test.com")
                .mobileNumber("9" + timestamp.substring(timestamp.length() - 9))
                .password("Password@123")
                .role(Role.USER)
                .build());

        String studentToken = jwtUtil.generateToken(studentUser, "USER");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/payments/999/confirm")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Dashboard stats endpoints work seamlessly with open-in-view=false")
    void testDashboardEndpointsWithOpenInViewDisabled() throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String studentUser = "student_dash_" + timestamp;
        userRepository.save(User.builder()
                .fullName("Dashboard Student")
                .username(studentUser)
                .email(studentUser + "@test.com")
                .mobileNumber("9" + timestamp.substring(timestamp.length() - 9))
                .password("Password@123")
                .role(Role.USER)
                .build());

        String adminUser = "admin_dash_" + timestamp;
        userRepository.save(User.builder()
                .fullName("Dashboard Admin")
                .username(adminUser)
                .email(adminUser + "@test.com")
                .mobileNumber("8" + timestamp.substring(timestamp.length() - 9))
                .password("Password@123")
                .role(Role.ADMIN)
                .build());

        String studentToken = jwtUtil.generateToken(studentUser, "USER");
        String adminToken = jwtUtil.generateToken(adminUser, "ADMIN");

        // Admin stats
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").isNumber())
                .andExpect(jsonPath("$.totalRevenue").isNumber());

        // Student stats
        mockMvc.perform(get("/api/dashboard/student-stats")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBookings").isNumber());
    }

    @Test
    @DisplayName("Case-insensitive duplicate registration is rejected (ONE NORMALIZED USERNAME = ONE ACCOUNT)")
    void testCaseInsensitiveDuplicateRegistrationRejected() throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String mixedCase = "Rushikesh_" + timestamp;

        RegisterRequest req1 = new RegisterRequest();
        req1.setFullName("Rushikesh One");
        req1.setUsername(mixedCase);
        req1.setEmail("rushi1_" + timestamp + "@example.com");
        req1.setMobileNumber("9" + timestamp.substring(timestamp.length() - 9));
        req1.setPassword("Password@123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        // Attempting to register with lower-case variation of same username
        RegisterRequest req2 = new RegisterRequest();
        req2.setFullName("Rushikesh Two");
        req2.setUsername(mixedCase.toLowerCase());
        req2.setEmail("rushi2_" + timestamp + "@example.com");
        req2.setMobileNumber("8" + timestamp.substring(timestamp.length() - 9));
        req2.setPassword("Password@123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("already taken")));
    }

    @Test
    @DisplayName("Login works regardless of username case variations")
    void testLoginCaseInsensitivity() throws Exception {
        LoginRequest reqUpper = new LoginRequest();
        reqUpper.setUsername("ADMIN");
        reqUpper.setPassword("Admin123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqUpper)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());

        LoginRequest reqLower = new LoginRequest();
        reqLower.setUsername("admin");
        reqLower.setPassword("Admin123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqLower)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    @DisplayName("Reserved admin and warden usernames are rejected in any case variation during public registration")
    void testReservedUsernamesRejectedInAnyCase() throws Exception {
        RegisterRequest reqAdmin = new RegisterRequest();
        reqAdmin.setFullName("Fake Admin");
        reqAdmin.setUsername("ADMIN");
        reqAdmin.setEmail("fakeadmin@example.com");
        reqAdmin.setMobileNumber("9111111111");
        reqAdmin.setPassword("Password@123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqAdmin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("reserved")));

        RegisterRequest reqWarden = new RegisterRequest();
        reqWarden.setFullName("Fake Warden");
        reqWarden.setUsername("WaRdEn");
        reqWarden.setEmail("fakewarden@example.com");
        reqWarden.setMobileNumber("9222222222");
        reqWarden.setPassword("Password@123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqWarden)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("reserved")));
    }

    @Test
    @DisplayName("User API returns UserResponse and never exposes password hash")
    void testUserApiNeverExposesPassword() throws Exception {
        String adminToken = jwtUtil.generateToken("admin", "ADMIN");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].username").exists())
                .andExpect(jsonPath("$[0].email").exists());
    }

    @Test
    @DisplayName("CORS preflight OPTIONS for login from localhost:5173 returns 200 OK with correct CORS headers")
    void testCorsPreflightForLoginEndpoint() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().exists("Access-Control-Allow-Headers"));
    }

    @Test
    @DisplayName("CORS actual POST login from localhost:5173 succeeds and includes CORS headers")
    void testCorsPostLoginSucceeds() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("Admin");
        req.setPassword("Admin123");

        mockMvc.perform(post("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    @DisplayName("CORS preflight from 127.0.0.1:5173 is allowed")
    void testCorsPreflightFromLoopbackIp() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://127.0.0.1:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("CORS preflight from alternate localhost port (e.g., 5174) is allowed via pattern")
    void testCorsPreflightFromAlternateLocalhostPort() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5174")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5174"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("CORS preflight from unauthorized external origin is rejected with 403 Forbidden")
    void testCorsPreflightFromUnauthorizedOriginRejected() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://unauthorized-evil-domain.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
