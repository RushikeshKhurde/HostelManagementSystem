package com.hostel.management.service;

import com.hostel.management.dto.*;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.*;
import com.hostel.management.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackQuestionRepository feedbackQuestionRepository;

    @Mock
    private FeedbackQuestionOptionRepository feedbackQuestionOptionRepository;

    @Mock
    private FeedbackSubmissionRepository feedbackSubmissionRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    private User studentA;
    private User studentB;
    private List<FeedbackQuestion> activeQuestions;

    @BeforeEach
    void setUp() {
        studentA = User.builder().id(101L).fullName("Rahul Patil").username("rahul101").email("rahul@college.edu").role(Role.USER).build();
        studentB = User.builder().id(102L).fullName("Pooja Sharma").username("pooja102").email("pooja@college.edu").role(Role.USER).build();

        activeQuestions = new ArrayList<>();
        for (long i = 1; i <= 15; i++) {
            FeedbackQuestion q = FeedbackQuestion.builder()
                    .id(i)
                    .questionText("Question " + i)
                    .category("CATEGORY_" + i)
                    .displayOrder((int) i)
                    .active(true)
                    .options(new ArrayList<>())
                    .build();

            for (long j = 1; j <= 5; j++) {
                long optId = (i - 1) * 5 + j;
                FeedbackQuestionOption opt = FeedbackQuestionOption.builder()
                        .id(optId)
                        .question(q)
                        .optionValue("OPT_" + j)
                        .displayText("Option " + j)
                        .displayOrder((int) j)
                        .build();
                q.getOptions().add(opt);
            }
            activeQuestions.add(q);
        }
    }

    private List<FeedbackAnswerRequest> createValidAnswers() {
        List<FeedbackAnswerRequest> list = new ArrayList<>();
        for (long i = 1; i <= 15; i++) {
            long optId = (i - 1) * 5 + 1; // pick first option of each question
            list.add(FeedbackAnswerRequest.builder().questionId(i).optionId(optId).build());
        }
        return list;
    }

    @Test
    @DisplayName("1. Student can load active questions and their options")
    void testGetActiveQuestions() {
        when(feedbackQuestionRepository.findActiveQuestionsWithOptions()).thenReturn(activeQuestions);

        List<FeedbackQuestionDto> result = feedbackService.getActiveQuestions();
        assertNotNull(result);
        assertEquals(15, result.size());
        assertEquals("Question 1", result.get(0).getQuestionText());
        assertEquals(5, result.get(0).getOptions().size());
    }

    @Test
    @DisplayName("2. Student can submit complete feedback questionnaire")
    void testSubmitCompleteFeedbackSuccess() {
        when(feedbackSubmissionRepository.existsByStudentId(studentA.getId())).thenReturn(false);
        when(feedbackQuestionRepository.findActiveQuestionsWithOptions()).thenReturn(activeQuestions);

        for (long i = 1; i <= 15; i++) {
            long optId = (i - 1) * 5 + 1;
            FeedbackQuestionOption opt = activeQuestions.get((int) i - 1).getOptions().get(0);
            when(feedbackQuestionOptionRepository.findByIdAndQuestionId(optId, i)).thenReturn(Optional.of(opt));
        }

        when(feedbackSubmissionRepository.save(any(FeedbackSubmission.class))).thenAnswer(invocation -> {
            FeedbackSubmission sub = invocation.getArgument(0);
            sub.setId(500L);
            return sub;
        });

        FeedbackSubmitRequest request = FeedbackSubmitRequest.builder().answers(createValidAnswers()).build();
        FeedbackSubmissionDto dto = feedbackService.submitFeedback(studentA, request);

        assertNotNull(dto);
        assertEquals(500L, dto.getId());
        assertEquals(studentA.getId(), dto.getStudentId());
        assertEquals(15, dto.getAnswers().size());
        verify(feedbackSubmissionRepository, times(1)).save(any(FeedbackSubmission.class));
    }

    @Test
    @DisplayName("3. Student cannot submit incomplete feedback")
    void testSubmitIncompleteFeedbackThrows() {
        when(feedbackSubmissionRepository.existsByStudentId(studentA.getId())).thenReturn(false);
        when(feedbackQuestionRepository.findActiveQuestionsWithOptions()).thenReturn(activeQuestions);

        List<FeedbackAnswerRequest> answers = createValidAnswers();
        answers.remove(answers.size() - 1); // Only 14 out of 15

        FeedbackSubmitRequest request = FeedbackSubmitRequest.builder().answers(answers).build();
        ApiException ex = assertThrows(ApiException.class, () -> feedbackService.submitFeedback(studentA, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("All 15 active questions must be answered"));
    }

    @Test
    @DisplayName("4. Student cannot submit invalid question ID")
    void testSubmitInvalidQuestionIdThrows() {
        when(feedbackSubmissionRepository.existsByStudentId(studentA.getId())).thenReturn(false);
        when(feedbackQuestionRepository.findActiveQuestionsWithOptions()).thenReturn(activeQuestions);

        List<FeedbackAnswerRequest> answers = createValidAnswers();
        answers.set(0, FeedbackAnswerRequest.builder().questionId(999L).optionId(1L).build());

        FeedbackSubmitRequest request = FeedbackSubmitRequest.builder().answers(answers).build();
        ApiException ex = assertThrows(ApiException.class, () -> feedbackService.submitFeedback(studentA, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("invalid or inactive"));
    }

    @Test
    @DisplayName("5. Student cannot submit invalid option ID")
    void testSubmitInvalidOptionIdThrows() {
        when(feedbackSubmissionRepository.existsByStudentId(studentA.getId())).thenReturn(false);
        when(feedbackQuestionRepository.findActiveQuestionsWithOptions()).thenReturn(activeQuestions);
        when(feedbackQuestionOptionRepository.findByIdAndQuestionId(9999L, 1L)).thenReturn(Optional.empty());

        List<FeedbackAnswerRequest> answers = createValidAnswers();
        answers.set(0, FeedbackAnswerRequest.builder().questionId(1L).optionId(9999L).build());

        FeedbackSubmitRequest request = FeedbackSubmitRequest.builder().answers(answers).build();
        ApiException ex = assertThrows(ApiException.class, () -> feedbackService.submitFeedback(studentA, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("does not belong to question ID"));
    }

    @Test
    @DisplayName("6. Critical: Student cannot submit option belonging to another question")
    void testSubmitOptionBelongingToAnotherQuestionThrows() {
        when(feedbackSubmissionRepository.existsByStudentId(studentA.getId())).thenReturn(false);
        when(feedbackQuestionRepository.findActiveQuestionsWithOptions()).thenReturn(activeQuestions);
        // Option 70 belongs to Question 14, not Question 1!
        when(feedbackQuestionOptionRepository.findByIdAndQuestionId(70L, 1L)).thenReturn(Optional.empty());

        List<FeedbackAnswerRequest> answers = createValidAnswers();
        answers.set(0, FeedbackAnswerRequest.builder().questionId(1L).optionId(70L).build());

        FeedbackSubmitRequest request = FeedbackSubmitRequest.builder().answers(answers).build();
        ApiException ex = assertThrows(ApiException.class, () -> feedbackService.submitFeedback(studentA, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("does not belong to question ID 1"));
    }

    @Test
    @DisplayName("7. Student cannot submit duplicate question answers")
    void testSubmitDuplicateQuestionAnswersThrows() {
        when(feedbackSubmissionRepository.existsByStudentId(studentA.getId())).thenReturn(false);
        when(feedbackQuestionRepository.findActiveQuestionsWithOptions()).thenReturn(activeQuestions);

        List<FeedbackAnswerRequest> answers = createValidAnswers();
        // Duplicate question 1
        answers.set(1, FeedbackAnswerRequest.builder().questionId(1L).optionId(2L).build());

        FeedbackSubmitRequest request = FeedbackSubmitRequest.builder().answers(answers).build();
        ApiException ex = assertThrows(ApiException.class, () -> feedbackService.submitFeedback(studentA, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Duplicate answer for question ID 1"));
    }

    @Test
    @DisplayName("8. Student cannot create duplicate active submission (one submission rule)")
    void testSubmitDuplicateActiveSubmissionThrowsConflict() {
        when(feedbackSubmissionRepository.existsByStudentId(studentA.getId())).thenReturn(true);

        FeedbackSubmitRequest request = FeedbackSubmitRequest.builder().answers(createValidAnswers()).build();
        ApiException ex = assertThrows(ApiException.class, () -> feedbackService.submitFeedback(studentA, request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("You have already submitted feedback"));
    }

    @Test
    @DisplayName("9. Student can view own feedback")
    void testGetMyFeedbackSuccess() {
        FeedbackSubmission sub = FeedbackSubmission.builder()
                .id(10L)
                .student(studentA)
                .submittedAt(LocalDateTime.now())
                .answers(List.of(
                        FeedbackAnswer.builder()
                                .id(1L)
                                .question(activeQuestions.get(0))
                                .selectedOption(activeQuestions.get(0).getOptions().get(0))
                                .build()
                ))
                .build();

        when(feedbackSubmissionRepository.findByStudentIdWithDetails(studentA.getId())).thenReturn(Optional.of(sub));

        FeedbackSubmissionDto dto = feedbackService.getMyFeedback(studentA);
        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("Rahul Patil", dto.getStudentName());
        assertEquals(1, dto.getAnswers().size());
    }

    @Test
    @DisplayName("10. Student receives null when they have not submitted yet")
    void testGetMyFeedbackWhenEmpty() {
        when(feedbackSubmissionRepository.findByStudentIdWithDetails(studentA.getId())).thenReturn(Optional.empty());

        FeedbackSubmissionDto dto = feedbackService.getMyFeedback(studentA);
        assertNull(dto);
    }

    @Test
    @DisplayName("11. Student can delete own feedback submission")
    void testDeleteOwnFeedbackSuccess() {
        FeedbackSubmission sub = FeedbackSubmission.builder()
                .id(10L)
                .student(studentA)
                .submittedAt(LocalDateTime.now())
                .build();

        when(feedbackSubmissionRepository.findById(10L)).thenReturn(Optional.of(sub));

        feedbackService.deleteMyFeedback(studentA, 10L);
        verify(feedbackSubmissionRepository, times(1)).delete(sub);
    }

    @Test
    @DisplayName("12. MANDATORY IDOR TEST: Student B cannot delete Student A's feedback")
    void testIdorDeleteAnotherStudentsFeedbackForbidden() {
        FeedbackSubmission sub = FeedbackSubmission.builder()
                .id(10L)
                .student(studentA) // Belongs to Student A
                .submittedAt(LocalDateTime.now())
                .build();

        when(feedbackSubmissionRepository.findById(10L)).thenReturn(Optional.of(sub));

        // Student B tries to delete Student A's feedback
        ApiException ex = assertThrows(ApiException.class, () -> feedbackService.deleteMyFeedback(studentB, 10L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("only authorized to delete your own"));
        verify(feedbackSubmissionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("13. Admin can view all feedback submissions")
    void testAdminGetAllFeedback() {
        FeedbackSubmission subA = FeedbackSubmission.builder().id(1L).student(studentA).submittedAt(LocalDateTime.now()).answers(new ArrayList<>()).build();
        FeedbackSubmission subB = FeedbackSubmission.builder().id(2L).student(studentB).submittedAt(LocalDateTime.now()).answers(new ArrayList<>()).build();

        when(feedbackSubmissionRepository.findAllWithDetails()).thenReturn(List.of(subA, subB));

        List<FeedbackSubmissionDto> list = feedbackService.getAllFeedback();
        assertNotNull(list);
        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("14. Admin can delete any student's feedback")
    void testAdminDeleteFeedbackSuccess() {
        FeedbackSubmission sub = FeedbackSubmission.builder().id(20L).student(studentA).submittedAt(LocalDateTime.now()).build();
        when(feedbackSubmissionRepository.findById(20L)).thenReturn(Optional.of(sub));

        feedbackService.adminDeleteFeedback(20L);
        verify(feedbackSubmissionRepository, times(1)).delete(sub);
    }

    @Test
    @DisplayName("15. Unauthenticated user cannot submit feedback")
    void testUnauthenticatedSubmitThrows() {
        FeedbackSubmitRequest req = FeedbackSubmitRequest.builder().answers(createValidAnswers()).build();
        ApiException ex = assertThrows(ApiException.class, () -> feedbackService.submitFeedback(null, req));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("16. Unauthenticated user cannot view feedback")
    void testUnauthenticatedGetMyFeedbackThrows() {
        ApiException ex = assertThrows(ApiException.class, () -> feedbackService.getMyFeedback(null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("17. Non-existent feedback deletion throws 404 Not Found")
    void testDeleteNonExistentFeedbackThrowsNotFound() {
        when(feedbackSubmissionRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> feedbackService.deleteMyFeedback(studentA, 999L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
