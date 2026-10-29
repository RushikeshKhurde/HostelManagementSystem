package com.hostel.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.management.model.*;
import com.hostel.management.repository.ComplaintRepository;
import com.hostel.management.repository.NotificationRepository;
import com.hostel.management.repository.UserRepository;
import com.hostel.management.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ComplaintVisibilityAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User ownerStudent;
    private User otherStudent;
    private User admin;
    private User warden;

    private String tokenOwnerStudent;
    private String tokenOtherStudent;
    private String tokenAdmin;
    private String tokenWarden;

    private static final byte[] VALID_PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
            (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
            0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
            0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00,
            0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
            0x42, 0x60, (byte) 0x82
    };

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        complaintRepository.deleteAll();

        ownerStudent = userRepository.findByUsername("vis_owner_stu").orElseGet(() -> {
            User u = User.builder()
                    .fullName("Owner Student")
                    .username("vis_owner_stu")
                    .email("owner_stu@hostel.com")
                    .mobileNumber("9876543201")
                    .password(passwordEncoder.encode("Password@123"))
                    .role(Role.USER)
                    .gender("Male")
                    .address("Room A-101")
                    .status("ACTIVE")
                    .enabled(true)
                    .build();
            return userRepository.save(u);
        });
        tokenOwnerStudent = jwtUtil.generateToken(ownerStudent.getUsername(), ownerStudent.getRole().name());

        otherStudent = userRepository.findByUsername("vis_other_stu").orElseGet(() -> {
            User u = User.builder()
                    .fullName("Other Student")
                    .username("vis_other_stu")
                    .email("other_stu@hostel.com")
                    .mobileNumber("9876543202")
                    .password(passwordEncoder.encode("Password@123"))
                    .role(Role.USER)
                    .gender("Female")
                    .address("Room B-202")
                    .status("ACTIVE")
                    .enabled(true)
                    .build();
            return userRepository.save(u);
        });
        tokenOtherStudent = jwtUtil.generateToken(otherStudent.getUsername(), otherStudent.getRole().name());

        admin = userRepository.findByUsername("admin").orElseGet(() -> {
            User a = User.builder()
                    .fullName("Main Admin")
                    .username("admin")
                    .email("admin@hostel.com")
                    .mobileNumber("9999999901")
                    .password(passwordEncoder.encode("Admin@123456"))
                    .role(Role.ADMIN)
                    .gender("Other")
                    .address("Admin Office")
                    .status("ACTIVE")
                    .enabled(true)
                    .build();
            return userRepository.save(a);
        });
        tokenAdmin = jwtUtil.generateToken(admin.getUsername(), admin.getRole().name());

        warden = userRepository.findByUsername("warden").orElseGet(() -> {
            User w = User.builder()
                    .fullName("Main Warden")
                    .username("warden")
                    .email("warden@hostel.com")
                    .mobileNumber("9888888801")
                    .password(passwordEncoder.encode("Warden@123456"))
                    .role(Role.WARDEN)
                    .gender("Other")
                    .address("Warden Office")
                    .status("ACTIVE")
                    .enabled(true)
                    .build();
            return userRepository.save(w);
        });
        tokenWarden = jwtUtil.generateToken(warden.getUsername(), warden.getRole().name());
    }

    private Long createComplaintWithImage(String feedback, String visibility, String filename) throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                filename,
                "image/png",
                VALID_PNG_BYTES
        );

        String res = mockMvc.perform(multipart("/api/complaints")
                        .file(imageFile)
                        .param("feedback", feedback)
                        .param("visibility", visibility)
                        .param("category", "CLEANLINESS")
                        .header("Authorization", "Bearer " + tokenOwnerStudent))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(res).get("id").asLong();
    }

    @Test
    @DisplayName("1. WARDEN_ONLY complaint authorization matrix: Owner & Warden YES, Admin & Other Student NO")
    void testWardenOnlyComplaintAuthorization() throws Exception {
        Long complaintId = createComplaintWithImage("Warden only issue in corridor", "WARDEN_ONLY", "warden_issue.png");

        // A. Owner Student: can view details, image, and in /my
        mockMvc.perform(get("/api/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + tokenOwnerStudent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("WARDEN_ONLY"));

        mockMvc.perform(get("/api/complaints/" + complaintId + "/image")
                        .header("Authorization", "Bearer " + tokenOwnerStudent))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/complaints/my")
                        .header("Authorization", "Bearer " + tokenOwnerStudent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(complaintId.intValue())));

        // B. Warden: can see in staff list, can view details, download image, update status
        mockMvc.perform(get("/api/complaints")
                        .header("Authorization", "Bearer " + tokenWarden))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(complaintId.intValue())));

        mockMvc.perform(get("/api/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + tokenWarden))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(complaintId));

        mockMvc.perform(get("/api/complaints/" + complaintId + "/image")
                        .header("Authorization", "Bearer " + tokenWarden))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/complaints/" + complaintId + "/status")
                        .header("Authorization", "Bearer " + tokenWarden)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS", "adminComment", "Warden acknowledged."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // C. Admin: MUST NOT see in staff list, MUST NOT view details (403), MUST NOT view image (403), MUST NOT update status (403)
        mockMvc.perform(get("/api/complaints")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(complaintId.intValue()))));

        mockMvc.perform(get("/api/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/complaints/" + complaintId + "/image")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/complaints/" + complaintId + "/status")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "RESOLVED"))))
                .andExpect(status().isForbidden());

        // D. Other Student: MUST NOT see in /my, MUST NOT view details (403), MUST NOT view image (403)
        mockMvc.perform(get("/api/complaints/my")
                        .header("Authorization", "Bearer " + tokenOtherStudent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(complaintId.intValue()))));

        mockMvc.perform(get("/api/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + tokenOtherStudent))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/complaints/" + complaintId + "/image")
                        .header("Authorization", "Bearer " + tokenOtherStudent))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("2. ADMIN_ONLY complaint authorization matrix: Owner & Admin YES, Warden & Other Student NO")
    void testAdminOnlyComplaintAuthorization() throws Exception {
        Long complaintId = createComplaintWithImage("Confidential administrative matter", "ADMIN_ONLY", "admin_issue.png");

        // A. Owner Student: can view details, image, and in /my
        mockMvc.perform(get("/api/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + tokenOwnerStudent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("ADMIN_ONLY"));

        mockMvc.perform(get("/api/complaints/" + complaintId + "/image")
                        .header("Authorization", "Bearer " + tokenOwnerStudent))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/complaints/my")
                        .header("Authorization", "Bearer " + tokenOwnerStudent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(complaintId.intValue())));

        // B. Admin: can see in staff list, view details, download image, update status
        mockMvc.perform(get("/api/complaints")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(complaintId.intValue())));

        mockMvc.perform(get("/api/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(complaintId));

        mockMvc.perform(get("/api/complaints/" + complaintId + "/image")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/complaints/" + complaintId + "/status")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS", "adminComment", "Admin investigating."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // C. Warden: MUST NOT see in staff list, MUST NOT view details (403), MUST NOT view image (403), MUST NOT update status (403)
        mockMvc.perform(get("/api/complaints")
                        .header("Authorization", "Bearer " + tokenWarden))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(complaintId.intValue()))));

        mockMvc.perform(get("/api/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + tokenWarden))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/complaints/" + complaintId + "/image")
                        .header("Authorization", "Bearer " + tokenWarden))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/complaints/" + complaintId + "/status")
                        .header("Authorization", "Bearer " + tokenWarden)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "RESOLVED"))))
                .andExpect(status().isForbidden());

        // D. Other Student: MUST NOT see in /my, MUST NOT view details (403), MUST NOT view image (403)
        mockMvc.perform(get("/api/complaints/my")
                        .header("Authorization", "Bearer " + tokenOtherStudent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(complaintId.intValue()))));

        mockMvc.perform(get("/api/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + tokenOtherStudent))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/complaints/" + complaintId + "/image")
                        .header("Authorization", "Bearer " + tokenOtherStudent))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. WARDEN_AND_ADMIN complaint authorization matrix: Owner, Admin & Warden YES, Other Student NO")
    void testBothWardenAndAdminComplaintAuthorization() throws Exception {
        Long complaintId = createComplaintWithImage("General maintenance needed in mess", "WARDEN_AND_ADMIN", "mess_issue.png");

        // A. Owner Student: can view details, image, and in /my
        mockMvc.perform(get("/api/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + tokenOwnerStudent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("WARDEN_AND_ADMIN"));

        mockMvc.perform(get("/api/complaints/" + complaintId + "/image")
                        .header("Authorization", "Bearer " + tokenOwnerStudent))
                .andExpect(status().isOk());

        // B. Admin: can see in staff list, view details, download image
        mockMvc.perform(get("/api/complaints")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(complaintId.intValue())));

        mockMvc.perform(get("/api/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/complaints/" + complaintId + "/image")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        // C. Warden: can see in staff list, view details, download image
        mockMvc.perform(get("/api/complaints")
                        .header("Authorization", "Bearer " + tokenWarden))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(complaintId.intValue())));

        mockMvc.perform(get("/api/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + tokenWarden))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/complaints/" + complaintId + "/image")
                        .header("Authorization", "Bearer " + tokenWarden))
                .andExpect(status().isOk());

        // D. Other Student: MUST NOT view details (403), MUST NOT view image (403)
        mockMvc.perform(get("/api/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + tokenOtherStudent))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/complaints/" + complaintId + "/image")
                        .header("Authorization", "Bearer " + tokenOtherStudent))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Notification delivery accurately respects complaint visibility")
    void testNotificationRecipientFiltering() throws Exception {
        notificationRepository.deleteAll();

        // 1. Submit WARDEN_ONLY
        createComplaintWithImage("Corridor light flickering", "WARDEN_ONLY", "light.png");

        // Verify warden received notification, admin did NOT
        List<Notification> wardenNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(warden.getId());
        List<Notification> adminNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(admin.getId());
        assertEquals(1, wardenNotifs.size());
        assertEquals(0, adminNotifs.size());

        notificationRepository.deleteAll();

        // 2. Submit ADMIN_ONLY
        createComplaintWithImage("Fee receipt discrepancy", "ADMIN_ONLY", "receipt.png");

        // Verify admin received notification, warden did NOT
        wardenNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(warden.getId());
        adminNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(admin.getId());
        assertEquals(0, wardenNotifs.size());
        assertEquals(1, adminNotifs.size());

        notificationRepository.deleteAll();

        // 3. Submit WARDEN_AND_ADMIN
        createComplaintWithImage("Water cooler malfunctioning", "WARDEN_AND_ADMIN", "cooler.png");

        // Verify both admin and warden received notification
        wardenNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(warden.getId());
        adminNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(admin.getId());
        assertEquals(1, wardenNotifs.size());
        assertEquals(1, adminNotifs.size());
    }

    @Test
    @DisplayName("5. Dashboard stats pending complaints count strictly respects visibility")
    void testDashboardStatsFilterVisibility() throws Exception {
        complaintRepository.deleteAll();

        // Save 1 WARDEN_ONLY (Pending)
        complaintRepository.save(Complaint.builder()
                .student(ownerStudent)
                .title("Warden issue")
                .feedback("Warden only feedback")
                .category("HYGIENE")
                .status(Complaint.ComplaintStatus.PENDING)
                .visibility(ComplaintVisibility.WARDEN_ONLY)
                .build());

        // Save 2 ADMIN_ONLY (Pending)
        complaintRepository.save(Complaint.builder()
                .student(ownerStudent)
                .title("Admin issue 1")
                .feedback("Admin only feedback 1")
                .category("MAINTENANCE")
                .status(Complaint.ComplaintStatus.PENDING)
                .visibility(ComplaintVisibility.ADMIN_ONLY)
                .build());
        complaintRepository.save(Complaint.builder()
                .student(ownerStudent)
                .title("Admin issue 2")
                .feedback("Admin only feedback 2")
                .category("MAINTENANCE")
                .status(Complaint.ComplaintStatus.PENDING)
                .visibility(ComplaintVisibility.ADMIN_ONLY)
                .build());

        // Save 3 WARDEN_AND_ADMIN (Pending)
        for (int i = 1; i <= 3; i++) {
            complaintRepository.save(Complaint.builder()
                    .student(ownerStudent)
                    .title("Both issue " + i)
                    .feedback("Both feedback " + i)
                    .category("MAINTENANCE")
                    .status(Complaint.ComplaintStatus.PENDING)
                    .visibility(ComplaintVisibility.WARDEN_AND_ADMIN)
                    .build());
        }

        // Admin dashboard stats: should see 2 ADMIN_ONLY + 3 WARDEN_AND_ADMIN = 5
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingComplaints").value(5));

        // Warden dashboard stats: should see 1 WARDEN_ONLY + 3 WARDEN_AND_ADMIN = 4
        mockMvc.perform(get("/api/dashboard/warden-stats")
                        .header("Authorization", "Bearer " + tokenWarden))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingComplaints").value(4));
    }

    @Test
    @DisplayName("6. Legacy complaint with null visibility safely defaults to WARDEN_AND_ADMIN")
    void testLegacyRecordCompatibility() throws Exception {
        Complaint legacy = Complaint.builder()
                .student(ownerStudent)
                .title("Historical Record")
                .feedback("Legacy complaint created before visibility field existed")
                .category("OTHER")
                .status(Complaint.ComplaintStatus.PENDING)
                .visibility(null) // explicitly null
                .build();
        legacy = complaintRepository.save(legacy);

        // Fetch as Admin
        mockMvc.perform(get("/api/complaints/" + legacy.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("WARDEN_AND_ADMIN"));

        // Fetch as Warden
        mockMvc.perform(get("/api/complaints/" + legacy.getId())
                        .header("Authorization", "Bearer " + tokenWarden))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("WARDEN_AND_ADMIN"));
    }
}
