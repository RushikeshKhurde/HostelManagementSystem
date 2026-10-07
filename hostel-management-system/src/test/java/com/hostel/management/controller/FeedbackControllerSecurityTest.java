package com.hostel.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.management.dto.*;
import com.hostel.management.model.Role;
import com.hostel.management.model.User;
import com.hostel.management.security.UserPrincipal;
import com.hostel.management.service.FeedbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FeedbackControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FeedbackService feedbackService;

    private User student;
    private User warden;
    private User admin;

    private UsernamePasswordAuthenticationToken studentAuth;
    private UsernamePasswordAuthenticationToken wardenAuth;
    private UsernamePasswordAuthenticationToken adminAuth;

    @BeforeEach
    void setUp() {
        student = User.builder().id(101L).fullName("Rahul Student").username("rahul101").role(Role.USER).email("rahul@college.edu").build();
        warden = User.builder().id(102L).fullName("Chief Warden").username("warden102").role(Role.WARDEN).email("warden@college.edu").build();
        admin = User.builder().id(103L).fullName("System Admin").username("admin103").role(Role.ADMIN).email("admin@college.edu").build();

        UserPrincipal studentPrincipal = new UserPrincipal(student);
        UserPrincipal wardenPrincipal = new UserPrincipal(warden);
        UserPrincipal adminPrincipal = new UserPrincipal(admin);

        studentAuth = new UsernamePasswordAuthenticationToken(studentPrincipal, null, studentPrincipal.getAuthorities());
        wardenAuth = new UsernamePasswordAuthenticationToken(wardenPrincipal, null, wardenPrincipal.getAuthorities());
        adminAuth = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());
    }

    @Test
    @DisplayName("Security: Unauthenticated user accessing questions is rejected (403 Forbidden)")
    void testGetQuestionsUnauthenticated() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/feedback/questions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Student can access active questions (200)")
    void testGetQuestionsAsStudent() throws Exception {
        when(feedbackService.getActiveQuestions()).thenReturn(List.of());
        mockMvc.perform(get("/api/feedback/questions").with(authentication(studentAuth)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: Student can submit feedback (201)")
    void testSubmitFeedbackAsStudent() throws Exception {
        FeedbackSubmitRequest req = FeedbackSubmitRequest.builder()
                .answers(List.of(FeedbackAnswerRequest.builder().questionId(1L).optionId(1L).build()))
                .build();

        FeedbackSubmissionDto dto = FeedbackSubmissionDto.builder().id(1L).studentId(student.getId()).build();
        when(feedbackService.submitFeedback(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/feedback")
                        .with(authentication(studentAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Security: Warden CANNOT submit feedback (403 Forbidden)")
    void testSubmitFeedbackAsWardenForbidden() throws Exception {
        FeedbackSubmitRequest req = FeedbackSubmitRequest.builder()
                .answers(List.of(FeedbackAnswerRequest.builder().questionId(1L).optionId(1L).build()))
                .build();

        mockMvc.perform(post("/api/feedback")
                        .with(authentication(wardenAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Admin CANNOT submit feedback (403 Forbidden)")
    void testSubmitFeedbackAsAdminForbidden() throws Exception {
        FeedbackSubmitRequest req = FeedbackSubmitRequest.builder()
                .answers(List.of(FeedbackAnswerRequest.builder().questionId(1L).optionId(1L).build()))
                .build();

        mockMvc.perform(post("/api/feedback")
                        .with(authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Student can view own feedback (200)")
    void testGetMyFeedbackAsStudent() throws Exception {
        when(feedbackService.getMyFeedback(any())).thenReturn(FeedbackSubmissionDto.builder().id(1L).build());
        mockMvc.perform(get("/api/feedback/my").with(authentication(studentAuth)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: Warden CANNOT call /api/feedback/my (403 Forbidden)")
    void testGetMyFeedbackAsWardenForbidden() throws Exception {
        mockMvc.perform(get("/api/feedback/my").with(authentication(wardenAuth)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Student CANNOT view all feedback (403 Forbidden)")
    void testGetAllFeedbackAsStudentForbidden() throws Exception {
        mockMvc.perform(get("/api/feedback").with(authentication(studentAuth)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Warden CAN view all feedback (200)")
    void testGetAllFeedbackAsWarden() throws Exception {
        when(feedbackService.getAllFeedback()).thenReturn(List.of());
        mockMvc.perform(get("/api/feedback").with(authentication(wardenAuth)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: Admin CAN view all feedback (200)")
    void testGetAllFeedbackAsAdmin() throws Exception {
        when(feedbackService.getAllFeedback()).thenReturn(List.of());
        mockMvc.perform(get("/api/feedback").with(authentication(adminAuth)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: Student can delete feedback (ownership verified by service)")
    void testDeleteFeedbackAsStudent() throws Exception {
        doNothing().when(feedbackService).deleteMyFeedback(any(), eq(1L));
        mockMvc.perform(delete("/api/feedback/1").with(authentication(studentAuth)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: Warden CANNOT delete feedback via /api/feedback/{id} (403 Forbidden)")
    void testDeleteFeedbackAsWardenForbidden() throws Exception {
        mockMvc.perform(delete("/api/feedback/1").with(authentication(wardenAuth)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Warden CANNOT delete feedback via /api/feedback/admin/{id} (403 Forbidden)")
    void testAdminDeleteAsWardenForbidden() throws Exception {
        mockMvc.perform(delete("/api/feedback/admin/1").with(authentication(wardenAuth)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Student CANNOT delete feedback via /api/feedback/admin/{id} (403 Forbidden)")
    void testAdminDeleteAsStudentForbidden() throws Exception {
        mockMvc.perform(delete("/api/feedback/admin/1").with(authentication(studentAuth)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Admin CAN delete feedback via /api/feedback/admin/{id} (200)")
    void testAdminDeleteAsAdminSuccess() throws Exception {
        doNothing().when(feedbackService).adminDeleteFeedback(1L);
        mockMvc.perform(delete("/api/feedback/admin/1").with(authentication(adminAuth)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Immutability: PUT /api/feedback/{id} does not exist (405 Method Not Allowed)")
    void testPutNotAllowed() throws Exception {
        mockMvc.perform(put("/api/feedback/1").with(authentication(studentAuth)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("Immutability: PATCH /api/feedback/{id} does not exist (405 Method Not Allowed)")
    void testPatchNotAllowed() throws Exception {
        mockMvc.perform(patch("/api/feedback/1").with(authentication(studentAuth)))
                .andExpect(status().isMethodNotAllowed());
    }
}
