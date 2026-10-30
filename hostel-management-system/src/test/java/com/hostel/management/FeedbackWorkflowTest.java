package com.hostel.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.management.dto.FeedbackAnswerRequest;
import com.hostel.management.dto.FeedbackSubmitRequest;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FeedbackWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedbackQuestionRepository feedbackQuestionRepository;

    @Autowired
    private FeedbackSubmissionRepository feedbackSubmissionRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

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

    private List<FeedbackQuestion> activeQuestions;

    // Valid 1x1 PNG bytes
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

    // Valid WebP header
    private static final byte[] VALID_WEBP_BYTES = new byte[]{
            'R', 'I', 'F', 'F', 0x20, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P',
            'V', 'P', '8', ' ', 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    @BeforeEach
    void setUp() {
        feedbackSubmissionRepository.deleteAll();

        student1 = userRepository.findByUsername("fb_stu1").orElseGet(() -> {
            User u = User.builder()
                    .fullName("Rohan Joshi")
                    .username("fb_stu1")
                    .email("rohan_stu1@hostel.com")
                    .mobileNumber("9876543231")
                    .password(passwordEncoder.encode("Password@123"))
                    .role(Role.USER)
                    .gender("Male")
                    .address("Room A-201")
                    .status("ACTIVE")
                    .enabled(true)
                    .build();
            return userRepository.save(u);
        });
        tokenStudent1 = jwtUtil.generateToken(student1.getUsername(), student1.getRole().name());

        student2 = userRepository.findByUsername("fb_stu2").orElseGet(() -> {
            User u = User.builder()
                    .fullName("Sneha Kulkarni")
                    .username("fb_stu2")
                    .email("sneha_stu2@hostel.com")
                    .mobileNumber("9876543232")
                    .password(passwordEncoder.encode("Password@123"))
                    .role(Role.USER)
                    .gender("Female")
                    .address("Room B-105")
                    .status("ACTIVE")
                    .enabled(true)
                    .build();
            return userRepository.save(u);
        });
        tokenStudent2 = jwtUtil.generateToken(student2.getUsername(), student2.getRole().name());

        admin = userRepository.findByUsername("admin").orElseGet(() -> {
            User a = User.builder()
                    .fullName("Hostel Administrator")
                    .username("admin")
                    .email("admin@hostel.com")
                    .mobileNumber("9999999999")
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
                    .fullName("Chief Warden")
                    .username("warden")
                    .email("warden@hostel.com")
                    .mobileNumber("9888888888")
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

        activeQuestions = feedbackQuestionRepository.findActiveQuestionsWithOptions();
        assertEquals(15, activeQuestions.size(), "Database must seed exactly 15 feedback questions.");
    }

    private List<FeedbackAnswerRequest> buildValidAnswers() {
        List<FeedbackAnswerRequest> answers = new ArrayList<>();
        for (FeedbackQuestion q : activeQuestions) {
            FeedbackQuestionOption firstOption = q.getOptions().get(0);
            answers.add(FeedbackAnswerRequest.builder()
                    .questionId(q.getId())
                    .optionId(firstOption.getId())
                    .build());
        }
        return answers;
    }

    @Test
    @DisplayName("1. Submit answers to all 15 questions without description or image -> 201 Created")
    void testSubmitFeedbackWithoutDescriptionOrImage() throws Exception {
        List<FeedbackAnswerRequest> answers = buildValidAnswers();

        mockMvc.perform(multipart("/api/feedback")
                        .param("answers", objectMapper.writeValueAsString(answers))
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.studentId").value(student1.getId()))
                .andExpect(jsonPath("$.answers", hasSize(15)))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.hasImage").value(false))
                .andExpect(jsonPath("$.imageUrl").doesNotExist());

        // Verify retrieval via /api/feedback/my
        mockMvc.perform(get("/api/feedback/my")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answers", hasSize(15)))
                .andExpect(jsonPath("$.hasImage").value(false));
    }

    @Test
    @DisplayName("2. Submit answers with description and no image -> 201 Created")
    void testSubmitFeedbackWithDescription() throws Exception {
        List<FeedbackAnswerRequest> answers = buildValidAnswers();
        String desc = "The washroom cleaning schedule could be improved during exam weeks.";

        mockMvc.perform(multipart("/api/feedback")
                        .param("answers", objectMapper.writeValueAsString(answers))
                        .param("description", desc)
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(desc))
                .andExpect(jsonPath("$.hasImage").value(false));
    }

    @Test
    @DisplayName("3. Submit answers with PNG image and no description -> 201 Created")
    void testSubmitFeedbackWithImage() throws Exception {
        List<FeedbackAnswerRequest> answers = buildValidAnswers();

        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "washroom_leak.png",
                "image/png",
                VALID_PNG_BYTES
        );

        String res = mockMvc.perform(multipart("/api/feedback")
                        .file(imageFile)
                        .param("answers", objectMapper.writeValueAsString(answers))
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasImage").value(true))
                .andExpect(jsonPath("$.imageUrl").isString())
                .andExpect(jsonPath("$.imageName").value("washroom_leak.png"))
                .andReturn().getResponse().getContentAsString();

        Long submissionId = objectMapper.readTree(res).get("id").asLong();

        // Download image securely as student1
        mockMvc.perform(get("/api/feedback/" + submissionId + "/image")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE))
                .andExpect(header().string("Content-Disposition", containsString("washroom_leak.png")));
    }

    @Test
    @DisplayName("4. Submit answers with both description AND WebP image -> 201 Created")
    void testSubmitFeedbackWithDescriptionAndWebpImage() throws Exception {
        List<FeedbackAnswerRequest> answers = buildValidAnswers();
        String desc = "Mess water dispenser filter requires replacement.";

        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "filter.webp",
                "image/webp",
                VALID_WEBP_BYTES
        );

        mockMvc.perform(multipart("/api/feedback")
                        .file(imageFile)
                        .param("answers", objectMapper.writeValueAsString(answers))
                        .param("description", desc)
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(desc))
                .andExpect(jsonPath("$.hasImage").value(true))
                .andExpect(jsonPath("$.imageName").value("filter.webp"));
    }

    @Test
    @DisplayName("5. Incomplete questionnaire answers (e.g. 14 instead of 15) -> 400 Bad Request")
    void testIncompleteAnswersRejected() throws Exception {
        List<FeedbackAnswerRequest> answers = buildValidAnswers();
        answers.remove(answers.size() - 1); // remove 1 answer

        mockMvc.perform(multipart("/api/feedback")
                        .param("answers", objectMapper.writeValueAsString(answers))
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("All 15 active questions must be answered")));
    }

    @Test
    @DisplayName("6. Empty / omitted optional description -> successfully stored as null")
    void testOmittedDescriptionStoredAsNull() throws Exception {
        List<FeedbackAnswerRequest> answers = buildValidAnswers();

        mockMvc.perform(multipart("/api/feedback")
                        .param("answers", objectMapper.writeValueAsString(answers))
                        .param("description", "   ")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    @DisplayName("7. Oversized description (> 5,000 chars) -> 400 Bad Request")
    void testOversizedDescriptionRejected() throws Exception {
        List<FeedbackAnswerRequest> answers = buildValidAnswers();
        String longDesc = "X".repeat(5005);

        mockMvc.perform(multipart("/api/feedback")
                        .param("answers", objectMapper.writeValueAsString(answers))
                        .param("description", longDesc)
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("5000 characters")));
    }

    @Test
    @DisplayName("8. Unsupported image format (.exe) -> 400 Bad Request")
    void testUnsupportedFormatRejected() throws Exception {
        List<FeedbackAnswerRequest> answers = buildValidAnswers();

        MockMultipartFile badFile = new MockMultipartFile(
                "image",
                "script.exe",
                "application/x-msdownload",
                new byte[]{0x4D, 0x5A, 0x00, 0x00}
        );

        mockMvc.perform(multipart("/api/feedback")
                        .file(badFile)
                        .param("answers", objectMapper.writeValueAsString(answers))
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("strictly prohibited")));
    }

    @Test
    @DisplayName("9. Image exceeding 5 MB limit -> 400 Bad Request")
    void testOversizedImageRejected() throws Exception {
        List<FeedbackAnswerRequest> answers = buildValidAnswers();

        byte[] bigBytes = new byte[6 * 1024 * 1024];
        bigBytes[0] = (byte) 0x89;
        bigBytes[1] = 0x50;
        bigBytes[2] = 0x4E;
        bigBytes[3] = 0x47;

        MockMultipartFile bigFile = new MockMultipartFile(
                "image",
                "big.png",
                "image/png",
                bigBytes
        );

        mockMvc.perform(multipart("/api/feedback")
                        .file(bigFile)
                        .param("answers", objectMapper.writeValueAsString(answers))
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("5MB limit")));
    }

    @Test
    @DisplayName("10. Malformed or corrupted image file -> 400 Bad Request")
    void testCorruptedImageRejected() throws Exception {
        List<FeedbackAnswerRequest> answers = buildValidAnswers();

        MockMultipartFile corruptFile = new MockMultipartFile(
                "image",
                "fake.jpg",
                "image/jpeg",
                new byte[]{0x00, 0x01, 0x02, 0x03} // Not valid JPEG magic bytes
        );

        mockMvc.perform(multipart("/api/feedback")
                        .file(corruptFile)
                        .param("answers", objectMapper.writeValueAsString(answers))
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not match a valid JPG, PNG, or WebP image")));
    }

    @Test
    @DisplayName("11. Unauthenticated submission -> 401 Unauthorized")
    void testUnauthenticatedSubmission() throws Exception {
        List<FeedbackAnswerRequest> answers = buildValidAnswers();

        mockMvc.perform(multipart("/api/feedback")
                        .param("answers", objectMapper.writeValueAsString(answers)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("12. IDOR: Student 2 cannot access Student 1's feedback image -> 403 Forbidden")
    void testIdorFeedbackImageAccessForbidden() throws Exception {
        List<FeedbackAnswerRequest> answers = buildValidAnswers();

        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "private_dorm.png",
                "image/png",
                VALID_PNG_BYTES
        );

        String res = mockMvc.perform(multipart("/api/feedback")
                        .file(imageFile)
                        .param("answers", objectMapper.writeValueAsString(answers))
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long submissionId = objectMapper.readTree(res).get("id").asLong();

        // Student 2 tries to download Student 1's feedback image
        mockMvc.perform(get("/api/feedback/" + submissionId + "/image")
                        .header("Authorization", "Bearer " + tokenStudent2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not authorized")));
    }

    @Test
    @DisplayName("13. Authorized staff (Admin & Warden) can review feedback and retrieve image")
    void testStaffReviewAndImageAccess() throws Exception {
        List<FeedbackAnswerRequest> answers = buildValidAnswers();

        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "staff_review.png",
                "image/png",
                VALID_PNG_BYTES
        );

        String res = mockMvc.perform(multipart("/api/feedback")
                        .file(imageFile)
                        .param("answers", objectMapper.writeValueAsString(answers))
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long submissionId = objectMapper.readTree(res).get("id").asLong();

        // Admin lists all feedback
        mockMvc.perform(get("/api/feedback")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].studentName").value("Rohan Joshi"));

        // Admin views image
        mockMvc.perform(get("/api/feedback/" + submissionId + "/image")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE));

        // Warden views image
        mockMvc.perform(get("/api/feedback/" + submissionId + "/image")
                        .header("Authorization", "Bearer " + tokenWarden))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE));
    }

    @Test
    @DisplayName("14. Legacy feedback records without image or description remain readable")
    void testLegacyFeedbackRecordReadable() throws Exception {
        FeedbackSubmission legacy = FeedbackSubmission.builder()
                .student(student1)
                .answers(new ArrayList<>())
                .build();
        feedbackSubmissionRepository.save(legacy);

        mockMvc.perform(get("/api/feedback/my")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(legacy.getId()))
                .andExpect(jsonPath("$.hasImage").value(false))
                .andExpect(jsonPath("$.imageUrl").doesNotExist())
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    @DisplayName("15. Active questionnaire questions and options preserved")
    void testQuestionsPreserved() throws Exception {
        mockMvc.perform(get("/api/feedback/questions")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(15)))
                .andExpect(jsonPath("$[0].category").value("CLEANLINESS"))
                .andExpect(jsonPath("$[14].category").value("OVERALL"));
    }

    @Test
    @DisplayName("16. Critical regression check: Complaints module still functions with feedback, description, and image")
    void testComplaintModuleUnchanged() throws Exception {
        MockMultipartFile complaintImage = new MockMultipartFile(
                "image",
                "broken_chair.png",
                "image/png",
                VALID_PNG_BYTES
        );

        String res = mockMvc.perform(multipart("/api/complaints")
                        .file(complaintImage)
                        .param("feedback", "Study chair leg is broken in Room A-201.")
                        .param("description", "Needs carpenter inspection.")
                        .param("category", "FURNITURE")
                        .param("priority", "MEDIUM")
                        .param("visibility", "WARDEN_AND_ADMIN")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedback").value("Study chair leg is broken in Room A-201."))
                .andExpect(jsonPath("$.description").value("Needs carpenter inspection."))
                .andExpect(jsonPath("$.hasImage").value(true))
                .andExpect(jsonPath("$.imageUrl").isString())
                .andReturn().getResponse().getContentAsString();

        Long complaintId = objectMapper.readTree(res).get("id").asLong();

        // Verify complaint image access
        mockMvc.perform(get("/api/complaints/" + complaintId + "/image")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE));

        // Verify staff complaint status update
        mockMvc.perform(put("/api/complaints/" + complaintId + "/status")
                        .header("Authorization", "Bearer " + tokenWarden)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS", "adminComment", "Carpenter dispatched."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.adminComment").value("Carpenter dispatched."));
    }
}
