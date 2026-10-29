package com.hostel.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.management.model.Complaint;
import com.hostel.management.model.Notification;
import com.hostel.management.model.NotificationType;
import com.hostel.management.model.Role;
import com.hostel.management.model.User;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ComplaintWorkflowTest {

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

    private User student1;
    private User student2;
    private User admin;
    private User warden;

    private String tokenStudent1;
    private String tokenStudent2;
    private String tokenAdmin;
    private String tokenWarden;

    // Standard valid 1x1 PNG image with magic bytes 89 50 4E 47 0D 0A 1A 0A
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

    // Valid WebP header: RIFF....WEBP
    private static final byte[] VALID_WEBP_BYTES = new byte[]{
            'R', 'I', 'F', 'F', 0x20, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P',
            'V', 'P', '8', ' ', 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        complaintRepository.deleteAll();

        student1 = userRepository.findByUsername("complaint_stu1").orElseGet(() -> {
            User u = User.builder()
                    .fullName("Alice Student")
                    .username("complaint_stu1")
                    .email("alice_stu1@hostel.com")
                    .mobileNumber("9876543211")
                    .password(passwordEncoder.encode("Password@123"))
                    .role(Role.USER)
                    .gender("Female")
                    .address("Room A-101")
                    .status("ACTIVE")
                    .enabled(true)
                    .build();
            return userRepository.save(u);
        });
        tokenStudent1 = jwtUtil.generateToken(student1.getUsername(), student1.getRole().name());

        student2 = userRepository.findByUsername("complaint_stu2").orElseGet(() -> {
            User u = User.builder()
                    .fullName("Bob Student")
                    .username("complaint_stu2")
                    .email("bob_stu2@hostel.com")
                    .mobileNumber("9876543212")
                    .password(passwordEncoder.encode("Password@123"))
                    .role(Role.USER)
                    .gender("Male")
                    .address("Room B-202")
                    .status("ACTIVE")
                    .enabled(true)
                    .build();
            return userRepository.save(u);
        });
        tokenStudent2 = jwtUtil.generateToken(student2.getUsername(), student2.getRole().name());

        admin = userRepository.findByUsername("admin").orElseGet(() -> {
            User a = User.builder()
                    .fullName("System Admin")
                    .username("admin")
                    .email("admin@hostel.com")
                    .mobileNumber("9999999991")
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
                    .fullName("Head Warden")
                    .username("warden")
                    .email("warden@hostel.com")
                    .mobileNumber("9888888881")
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

    @Test
    @DisplayName("1. Authenticated student submits feedback alone with visibility -> success")
    void testSubmitFeedbackAlone() throws Exception {
        mockMvc.perform(multipart("/api/complaints")
                        .param("feedback", "The ceiling fan is vibrating violently and making rattling noise.")
                        .param("category", "ELECTRICITY")
                        .param("roomNumber", "A-101")
                        .param("priority", "HIGH")
                        .param("visibility", "WARDEN_AND_ADMIN")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.feedback").value("The ceiling fan is vibrating violently and making rattling noise."))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.hasImage").value(false))
                .andExpect(jsonPath("$.imageUrl").doesNotExist())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.visibility").value("WARDEN_AND_ADMIN"))
                .andExpect(jsonPath("$.student.fullName").value("Alice Student"));

        // Verify student notification created
        List<Notification> stuNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(student1.getId());
        assertFalse(stuNotifs.isEmpty());
        assertTrue(stuNotifs.get(0).getTitle().contains("Complaint Submitted"));
    }

    @Test
    @DisplayName("2. Authenticated student submits feedback with description but no image -> success")
    void testSubmitFeedbackWithDescription() throws Exception {
        mockMvc.perform(multipart("/api/complaints")
                        .param("feedback", "Bathroom tap leaking continuously.")
                        .param("description", "It started yesterday night around 9 PM. Water is pooling near the door.")
                        .param("category", "PLUMBING")
                        .param("roomNumber", "A-101")
                        .param("visibility", "WARDEN_AND_ADMIN")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedback").value("Bathroom tap leaking continuously."))
                .andExpect(jsonPath("$.description").value("It started yesterday night around 9 PM. Water is pooling near the door."))
                .andExpect(jsonPath("$.hasImage").value(false))
                .andExpect(jsonPath("$.visibility").value("WARDEN_AND_ADMIN"));
    }

    @Test
    @DisplayName("3. Authenticated student submits feedback with image but no description -> success")
    void testSubmitFeedbackWithImage() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "damaged_switch.png",
                "image/png",
                VALID_PNG_BYTES
        );

        String res = mockMvc.perform(multipart("/api/complaints")
                        .file(imageFile)
                        .param("feedback", "Wall socket broken and sparks when plugged in.")
                        .param("category", "ELECTRICITY")
                        .param("visibility", "WARDEN_AND_ADMIN")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedback").value("Wall socket broken and sparks when plugged in."))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.hasImage").value(true))
                .andExpect(jsonPath("$.imageUrl").isString())
                .andExpect(jsonPath("$.imageName").value("damaged_switch.png"))
                .andExpect(jsonPath("$.visibility").value("WARDEN_AND_ADMIN"))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(res).get("id").asLong();

        // Retrieve the image securely as student1
        mockMvc.perform(get("/api/complaints/" + id + "/image")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE))
                .andExpect(header().string("Content-Disposition", containsString("damaged_switch.png")));
    }

    @Test
    @DisplayName("4. Authenticated student submits feedback with description AND image -> success")
    void testSubmitFeedbackWithDescriptionAndWebpImage() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "leakage.webp",
                "image/webp",
                VALID_WEBP_BYTES
        );

        mockMvc.perform(multipart("/api/complaints")
                        .file(imageFile)
                        .param("feedback", "Severe water leakage from the ceiling.")
                        .param("description", "It is dripping directly onto the study desk. Please send maintenance urgently.")
                        .param("category", "PLUMBING")
                        .param("priority", "URGENT")
                        .param("visibility", "WARDEN_AND_ADMIN")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedback").value("Severe water leakage from the ceiling."))
                .andExpect(jsonPath("$.description").value("It is dripping directly onto the study desk. Please send maintenance urgently."))
                .andExpect(jsonPath("$.hasImage").value(true))
                .andExpect(jsonPath("$.priority").value("URGENT"))
                .andExpect(jsonPath("$.visibility").value("WARDEN_AND_ADMIN"));
    }

    @Test
    @DisplayName("5. Missing feedback -> rejected 400")
    void testMissingFeedbackRejected() throws Exception {
        mockMvc.perform(multipart("/api/complaints")
                        .param("feedback", "")
                        .param("description", "")
                        .param("visibility", "WARDEN_AND_ADMIN")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Feedback / complaint message is required")));
    }

    @Test
    @DisplayName("6. Whitespace-only feedback -> rejected 400")
    void testWhitespaceOnlyFeedbackRejected() throws Exception {
        mockMvc.perform(multipart("/api/complaints")
                        .param("feedback", "    \t  \n  ")
                        .param("visibility", "WARDEN_AND_ADMIN")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Feedback / complaint message is required")));
    }

    @Test
    @DisplayName("7. Oversized feedback (> 2000 chars) -> rejected 400")
    void testOversizedFeedbackRejected() throws Exception {
        String longFeedback = "A".repeat(2005);
        mockMvc.perform(multipart("/api/complaints")
                        .param("feedback", longFeedback)
                        .param("visibility", "WARDEN_AND_ADMIN")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Feedback / complaint message cannot exceed 2000 characters")));
    }

    @Test
    @DisplayName("8. Unsupported file type (.exe) -> rejected 400")
    void testUnsupportedFileTypeRejected() throws Exception {
        MockMultipartFile badFile = new MockMultipartFile(
                "image",
                "virus.exe",
                "application/x-msdownload",
                new byte[]{0x4D, 0x5A, 0x00, 0x00}
        );

        mockMvc.perform(multipart("/api/complaints")
                        .file(badFile)
                        .param("feedback", "Test complaint with malicious file.")
                        .param("visibility", "WARDEN_AND_ADMIN")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("strictly prohibited")));
    }

    @Test
    @DisplayName("9. Image exceeding size limit (> 5 MB) -> rejected 400")
    void testOversizedImageRejected() throws Exception {
        byte[] oversizedBytes = new byte[6 * 1024 * 1024]; // 6 MB
        oversizedBytes[0] = (byte) 0x89;
        oversizedBytes[1] = 0x50;
        oversizedBytes[2] = 0x4E;
        oversizedBytes[3] = 0x47;

        MockMultipartFile bigFile = new MockMultipartFile(
                "image",
                "huge_photo.png",
                "image/png",
                oversizedBytes
        );

        mockMvc.perform(multipart("/api/complaints")
                        .file(bigFile)
                        .param("feedback", "Oversized image test.")
                        .param("visibility", "WARDEN_AND_ADMIN")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("5MB limit")));
    }

    @Test
    @DisplayName("10. Unauthenticated complaint submission -> rejected 401")
    void testUnauthenticatedSubmissionRejected() throws Exception {
        mockMvc.perform(multipart("/api/complaints")
                        .param("feedback", "Attempting anonymous complaint.")
                        .param("visibility", "WARDEN_AND_ADMIN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("11. Student attempts IDOR access to another student's complaint image -> denied 403")
    void testIdorImageAccessDenied() throws Exception {
        // Student 1 submits complaint with image
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "private_photo.png",
                "image/png",
                VALID_PNG_BYTES
        );

        String res = mockMvc.perform(multipart("/api/complaints")
                        .file(imageFile)
                        .param("feedback", "Confidential issue in room.")
                        .param("visibility", "WARDEN_AND_ADMIN")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long complaintId = objectMapper.readTree(res).get("id").asLong();

        // Student 2 tries to download Student 1's image
        mockMvc.perform(get("/api/complaints/" + complaintId + "/image")
                        .header("Authorization", "Bearer " + tokenStudent2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not authorized")));

        // Student 2 tries to view Student 1's complaint details
        mockMvc.perform(get("/api/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + tokenStudent2))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("12. Student attempts staff-only status update -> denied 403")
    void testStudentCannotUpdateStatus() throws Exception {
        Complaint c = complaintRepository.save(Complaint.builder()
                .student(student1)
                .title("Sample")
                .feedback("Sample feedback")
                .category("OTHER")
                .status(Complaint.ComplaintStatus.PENDING)
                .build());

        mockMvc.perform(put("/api/complaints/" + c.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenStudent1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "RESOLVED"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("13. Authorized staff (Admin/Warden) can view complaint details, review image, and update status")
    void testStaffReviewAndStatusUpdate() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "issue.png",
                "image/png",
                VALID_PNG_BYTES
        );

        String res = mockMvc.perform(multipart("/api/complaints")
                        .file(imageFile)
                        .param("feedback", "Window latch broken.")
                        .param("visibility", "WARDEN_AND_ADMIN")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(res).get("id").asLong();

        // Admin views details
        mockMvc.perform(get("/api/complaints/" + id)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedback").value("Window latch broken."))
                .andExpect(jsonPath("$.visibility").value("WARDEN_AND_ADMIN"));

        // Warden downloads image
        mockMvc.perform(get("/api/complaints/" + id + "/image")
                        .header("Authorization", "Bearer " + tokenWarden))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE));

        // Warden updates status to IN_PROGRESS
        mockMvc.perform(put("/api/complaints/" + id + "/status")
                        .header("Authorization", "Bearer " + tokenWarden)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS", "adminComment", "Carpenter dispatched."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.adminComment").value("Carpenter dispatched."));

        // Admin updates status to RESOLVED
        mockMvc.perform(put("/api/complaints/" + id + "/status")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "RESOLVED", "adminComment", "Window repaired."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // Check student received status update notifications
        List<Notification> notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(student1.getId());
        boolean hasUpdateNotif = notifs.stream().anyMatch(n -> n.getMessage().contains("RESOLVED"));
        assertTrue(hasUpdateNotif);
    }

    @Test
    @DisplayName("14. Existing complaint records without image or feedback column remain readable")
    void testExistingComplaintWithoutImageReadable() throws Exception {
        Complaint legacy = Complaint.builder()
                .student(student1)
                .title("Legacy Complaint")
                .category("HYGIENE")
                .description("Legacy description without separate feedback or image")
                .status(Complaint.ComplaintStatus.RESOLVED)
                .adminComment("Resolved long ago")
                .build();
        legacy = complaintRepository.save(legacy);

        mockMvc.perform(get("/api/complaints/" + legacy.getId())
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(legacy.getId()))
                .andExpect(jsonPath("$.feedback").value("Legacy description without separate feedback or image"))
                .andExpect(jsonPath("$.hasImage").value(false))
                .andExpect(jsonPath("$.imageUrl").doesNotExist())
                .andExpect(jsonPath("$.visibility").value("WARDEN_AND_ADMIN"));
    }

    @Test
    @DisplayName("15. Complaint submission without visibility is rejected with 400 Bad Request")
    void testMissingVisibilityRejected() throws Exception {
        mockMvc.perform(multipart("/api/complaints")
                        .param("feedback", "Complaint without visibility selection.")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Complaint visibility is required")));
    }

    @Test
    @DisplayName("16. Complaint submission with invalid visibility value is rejected with 400 Bad Request")
    void testInvalidVisibilityRejected() throws Exception {
        mockMvc.perform(multipart("/api/complaints")
                        .param("feedback", "Complaint with invalid visibility.")
                        .param("visibility", "SUPER_ADMIN_ONLY")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid complaint visibility")));
    }
}
