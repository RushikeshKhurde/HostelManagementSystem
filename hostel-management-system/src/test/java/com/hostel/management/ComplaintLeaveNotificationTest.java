package com.hostel.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.management.dto.ComplaintRequest;
import com.hostel.management.dto.LeaveRequestDto;
import com.hostel.management.dto.NoticeRequest;
import com.hostel.management.model.*;
import com.hostel.management.repository.*;
import com.hostel.management.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ComplaintLeaveNotificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User studentUser;
    private User adminUser;
    private User wardenUser;
    private String studentToken;
    private String adminToken;
    private String wardenToken;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        complaintRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        noticeRepository.deleteAll();

        studentUser = userRepository.findByUsername("student_test").orElseGet(() -> {
            User s = User.builder()
                    .fullName("Test Student")
                    .username("student_test")
                    .email("student_test@example.com")
                    .mobileNumber("9876543210")
                    .password(passwordEncoder.encode("Password@123"))
                    .role(Role.USER)
                    .gender("Male")
                    .address("Hostel Block A")
                    .status("ACTIVE")
                    .enabled(true)
                    .build();
            return userRepository.save(s);
        });
        studentUser.setStatus("ACTIVE");
        studentUser.setEnabled(true);
        userRepository.save(studentUser);
        studentToken = jwtUtil.generateToken(studentUser.getUsername(), studentUser.getRole().name());

        adminUser = userRepository.findByUsername("admin").orElseGet(() -> {
            User a = User.builder()
                    .fullName("System Administrator")
                    .username("admin")
                    .email("admin@hostel.com")
                    .mobileNumber("9999999999")
                    .password(passwordEncoder.encode("Admin@123456"))
                    .role(Role.ADMIN)
                    .gender("Other")
                    .address("Hostel Admin Office")
                    .status("ACTIVE")
                    .enabled(true)
                    .build();
            return userRepository.save(a);
        });
        adminToken = jwtUtil.generateToken(adminUser.getUsername(), adminUser.getRole().name());

        wardenUser = userRepository.findByUsername("warden").orElseGet(() -> {
            User w = User.builder()
                    .fullName("Chief Warden")
                    .username("warden")
                    .email("warden@hostel.com")
                    .mobileNumber("9888888888")
                    .password(passwordEncoder.encode("Warden@123456"))
                    .role(Role.WARDEN)
                    .gender("Other")
                    .address("Hostel Warden Office")
                    .status("ACTIVE")
                    .enabled(true)
                    .build();
            return userRepository.save(w);
        });
        wardenToken = jwtUtil.generateToken(wardenUser.getUsername(), wardenUser.getRole().name());
    }

    @Test
    @DisplayName("Student creates complaint and fetches /my with DTO response")
    void testStudentComplaintWorkflow() throws Exception {
        ComplaintRequest req = new ComplaintRequest();
        req.setTitle("Water heater broken");
        req.setCategory("MAINTENANCE");
        req.setDescription("Hot water is not working in room bathroom since yesterday.");

        String response = mockMvc.perform(post("/api/complaints")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Water heater broken"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.student.fullName").value("Test Student"))
                .andReturn().getResponse().getContentAsString();

        Long complaintId = objectMapper.readTree(response).get("id").asLong();

        // Fetch /my
        mockMvc.perform(get("/api/complaints/my")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(complaintId))
                .andExpect(jsonPath("$[0].student.email").value("student_test@example.com"));

        // Admin updates status to IN_PROGRESS then RESOLVED
        mockMvc.perform(put("/api/complaints/" + complaintId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(put("/api/complaints/" + complaintId + "/status")
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "RESOLVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // Invalid transition from RESOLVED to PENDING
        mockMvc.perform(put("/api/complaints/" + complaintId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PENDING"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Student applies for leave and admin/warden approves")
    void testLeaveRequestWorkflow() throws Exception {
        LeaveRequestDto req = new LeaveRequestDto();
        req.setReason("Attending family wedding ceremony in hometown");
        req.setStartDate(LocalDate.now().plusDays(2));
        req.setEndDate(LocalDate.now().plusDays(5));

        String response = mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.reason").value("Attending family wedding ceremony in hometown"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.student.fullName").value("Test Student"))
                .andReturn().getResponse().getContentAsString();

        Long leaveId = objectMapper.readTree(response).get("id").asLong();

        // Student lists their leaves
        mockMvc.perform(get("/api/leaves/my")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(leaveId));

        // Warden approves leave
        mockMvc.perform(put("/api/leaves/" + leaveId + "/status")
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "APPROVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Cannot transition from APPROVED to REJECTED
        mockMvc.perform(put("/api/leaves/" + leaveId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "REJECTED"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Admin posts notice with authenticated UserPrincipal as postedBy")
    void testNoticePostingAndRetrieval() throws Exception {
        NoticeRequest noticeReq = new NoticeRequest();
        noticeReq.setTitle("Hostel Annual Maintenance Notice");
        noticeReq.setContent("Water supply will be temporarily stopped on Sunday from 10 AM to 2 PM.");

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noticeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Hostel Annual Maintenance Notice"))
                .andExpect(jsonPath("$.postedBy").value("System Administrator (ADMIN)"));

        mockMvc.perform(get("/api/notices")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].postedBy").value("System Administrator (ADMIN)"));
    }

    @Test
    @DisplayName("Student notification list and mark-as-read returns NotificationResponse")
    void testNotificationEndpoints() throws Exception {
        Notification notification = Notification.builder()
                .recipient(studentUser)
                .title("Welcome to SmartHostel")
                .message("Your student account has been activated successfully.")
                .type(NotificationType.GENERAL)
                .isRead(false)
                .build();
        notification = notificationRepository.save(notification);

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Welcome to SmartHostel"))
                .andExpect(jsonPath("$[0].isRead").value(false))
                .andExpect(jsonPath("$[0].type").value("GENERAL"));

        mockMvc.perform(put("/api/notifications/" + notification.getId() + "/read")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    @DisplayName("Deactivated user token is rejected by JwtFilter")
    void testDeactivatedUserTokenRejected() throws Exception {
        studentUser.setStatus("INACTIVE");
        studentUser.setEnabled(false);
        userRepository.save(studentUser);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin self-deactivation and deactivation of another admin are blocked")
    void testAdminDeactivationProtection() throws Exception {
        // Admin status change attempt
        mockMvc.perform(put("/api/users/" + adminUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "INACTIVE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cannot change the status of an ADMIN account")));

        // Invalid status string
        mockMvc.perform(put("/api/users/" + studentUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "DELETED"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid status. Only ACTIVE or INACTIVE are allowed.")));
    }
}
