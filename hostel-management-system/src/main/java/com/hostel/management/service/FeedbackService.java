package com.hostel.management.service;

import com.hostel.management.dto.*;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.*;
import com.hostel.management.repository.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackQuestionRepository feedbackQuestionRepository;
    private final FeedbackQuestionOptionRepository feedbackQuestionOptionRepository;
    private final FeedbackSubmissionRepository feedbackSubmissionRepository;
    private final FileStorageService fileStorageService;

    @Getter
    @AllArgsConstructor
    public static class FeedbackImageResource {
        private final Resource resource;
        private final String contentType;
        private final String filename;
    }

    @Transactional(readOnly = true)
    public List<FeedbackQuestionDto> getActiveQuestions() {
        List<FeedbackQuestion> questions = feedbackQuestionRepository.findActiveQuestionsWithOptions();
        return questions.stream()
                .map(q -> FeedbackQuestionDto.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .category(q.getCategory())
                        .displayOrder(q.getDisplayOrder())
                        .options(q.getOptions().stream()
                                .map(o -> FeedbackOptionDto.builder()
                                        .id(o.getId())
                                        .optionValue(o.getOptionValue())
                                        .displayText(o.getDisplayText())
                                        .displayOrder(o.getDisplayOrder())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public FeedbackSubmissionDto submitFeedback(User student, FeedbackSubmitRequest request, MultipartFile image) {
        if (student == null) {
            throw new ApiException("Authentication required to submit feedback.", HttpStatus.UNAUTHORIZED);
        }

        // Enforce: one active questionnaire submission per student
        if (feedbackSubmissionRepository.existsByStudentId(student.getId())) {
            throw new ApiException("You have already submitted feedback. Delete your existing submission if you wish to resubmit.", HttpStatus.CONFLICT);
        }

        List<FeedbackQuestion> activeQuestions = feedbackQuestionRepository.findActiveQuestionsWithOptions();
        if (activeQuestions.isEmpty()) {
            throw new ApiException("Feedback questionnaire is currently unavailable.", HttpStatus.BAD_REQUEST);
        }

        if (request == null || request.getAnswers() == null || request.getAnswers().isEmpty()) {
            throw new ApiException("Answers list cannot be empty.", HttpStatus.BAD_REQUEST);
        }

        // Validate no duplicate question answers in submission
        Set<Long> submittedQuestionIds = new HashSet<>();
        for (FeedbackAnswerRequest ans : request.getAnswers()) {
            if (ans.getQuestionId() == null || ans.getOptionId() == null) {
                throw new ApiException("Question ID and Option ID are both required for each answer.", HttpStatus.BAD_REQUEST);
            }
            if (!submittedQuestionIds.add(ans.getQuestionId())) {
                throw new ApiException("Duplicate answer for question ID " + ans.getQuestionId() + " is not allowed.", HttpStatus.BAD_REQUEST);
            }
        }

        // Validate that all active questions are answered (no missing, no extra)
        if (request.getAnswers().size() != activeQuestions.size()) {
            throw new ApiException("All " + activeQuestions.size() + " active questions must be answered.", HttpStatus.BAD_REQUEST);
        }

        Map<Long, FeedbackQuestion> activeQuestionMap = activeQuestions.stream()
                .collect(Collectors.toMap(FeedbackQuestion::getId, Function.identity()));

        // Process optional description
        String description = null;
        if (request.getDescription() != null) {
            String trimmed = request.getDescription().trim();
            if (trimmed.length() > 5000) {
                throw new ApiException("Additional description cannot exceed 5000 characters.", HttpStatus.BAD_REQUEST);
            }
            if (!trimmed.isBlank()) {
                description = trimmed;
            }
        }

        // Process optional image
        String imagePath = null;
        String imageName = null;
        String imageContentType = null;
        if (image != null && !image.isEmpty()) {
            FileStorageService.StoredFile stored = fileStorageService.storeFeedbackImage(image);
            if (stored != null) {
                imagePath = stored.getFilename();
                imageName = stored.getOriginalFilename();
                imageContentType = stored.getContentType();
            }
        }

        FeedbackSubmission submission = FeedbackSubmission.builder()
                .student(student)
                .submittedAt(LocalDateTime.now())
                .description(description)
                .imagePath(imagePath)
                .imageName(imageName)
                .imageContentType(imageContentType)
                .answers(new ArrayList<>())
                .build();

        for (FeedbackAnswerRequest ans : request.getAnswers()) {
            FeedbackQuestion question = activeQuestionMap.get(ans.getQuestionId());
            if (question == null) {
                throw new ApiException("Question ID " + ans.getQuestionId() + " is invalid or inactive.", HttpStatus.BAD_REQUEST);
            }

            // Critical: Verify option exists AND belongs to the question
            FeedbackQuestionOption option = feedbackQuestionOptionRepository.findByIdAndQuestionId(ans.getOptionId(), ans.getQuestionId())
                    .orElseThrow(() -> new ApiException("Option ID " + ans.getOptionId() + " does not belong to question ID " + ans.getQuestionId(), HttpStatus.BAD_REQUEST));

            FeedbackAnswer answer = FeedbackAnswer.builder()
                    .submission(submission)
                    .question(question)
                    .selectedOption(option)
                    .build();

            submission.getAnswers().add(answer);
        }

        FeedbackSubmission saved = feedbackSubmissionRepository.save(submission);
        return mapToDto(saved);
    }

    @Transactional
    public FeedbackSubmissionDto submitFeedback(User student, FeedbackSubmitRequest request) {
        return submitFeedback(student, request, null);
    }

    @Transactional(readOnly = true)
    public FeedbackSubmissionDto getMyFeedback(User student) {
        if (student == null) {
            throw new ApiException("Authentication required.", HttpStatus.UNAUTHORIZED);
        }
        return feedbackSubmissionRepository.findByStudentIdWithDetails(student.getId())
                .map(this::mapToDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public FeedbackImageResource getFeedbackImage(Long submissionId, User currentUser) {
        if (currentUser == null) {
            throw new ApiException("Authentication required.", HttpStatus.UNAUTHORIZED);
        }

        FeedbackSubmission submission = feedbackSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ApiException("Feedback submission not found with id: " + submissionId, HttpStatus.NOT_FOUND));

        boolean isStaff = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.WARDEN;
        boolean isOwner = submission.getStudent() != null && submission.getStudent().getId().equals(currentUser.getId());

        if (!isStaff && !isOwner) {
            throw new ApiException("You are not authorized to view this feedback image.", HttpStatus.FORBIDDEN);
        }

        if (submission.getImagePath() == null || submission.getImagePath().isBlank()) {
            throw new ApiException("Feedback submission does not have an attached image.", HttpStatus.NOT_FOUND);
        }

        Resource resource = fileStorageService.loadFeedbackImageAsResource(submission.getImagePath());
        String contentType = submission.getImageContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "image/jpeg";
        }

        return new FeedbackImageResource(resource, contentType, submission.getImageName());
    }

    @Transactional
    public void deleteMyFeedback(User student, Long submissionId) {
        if (student == null) {
            throw new ApiException("Authentication required.", HttpStatus.UNAUTHORIZED);
        }

        FeedbackSubmission submission = feedbackSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ApiException("Feedback submission not found with id: " + submissionId, HttpStatus.NOT_FOUND));

        // IDOR ownership verification: student can only delete their own feedback
        if (!submission.getStudent().getId().equals(student.getId())) {
            throw new ApiException("You are only authorized to delete your own feedback submission.", HttpStatus.FORBIDDEN);
        }

        feedbackSubmissionRepository.delete(submission);
    }

    @Transactional(readOnly = true)
    public List<FeedbackSubmissionDto> getAllFeedback() {
        return feedbackSubmissionRepository.findAllWithDetails().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void adminDeleteFeedback(Long submissionId) {
        FeedbackSubmission submission = feedbackSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ApiException("Feedback submission not found with id: " + submissionId, HttpStatus.NOT_FOUND));

        feedbackSubmissionRepository.delete(submission);
    }

    private FeedbackSubmissionDto mapToDto(FeedbackSubmission sub) {
        List<FeedbackAnswerDto> answerDtos = sub.getAnswers().stream()
                .sorted(Comparator.comparing(a -> a.getQuestion().getDisplayOrder()))
                .map(a -> FeedbackAnswerDto.builder()
                        .questionId(a.getQuestion().getId())
                        .question(a.getQuestion().getQuestionText())
                        .category(a.getQuestion().getCategory())
                        .displayOrder(a.getQuestion().getDisplayOrder())
                        .selectedOptionId(a.getSelectedOption().getId())
                        .selectedOption(a.getSelectedOption().getDisplayText())
                        .selectedOptionValue(a.getSelectedOption().getOptionValue())
                        .build())
                .collect(Collectors.toList());

        boolean hasImage = sub.getImagePath() != null && !sub.getImagePath().isBlank();
        String imageUrl = hasImage ? "/api/feedback/" + sub.getId() + "/image" : null;

        return FeedbackSubmissionDto.builder()
                .id(sub.getId())
                .studentId(sub.getStudent().getId())
                .studentName(sub.getStudent().getFullName())
                .studentUsername(sub.getStudent().getUsername())
                .studentEmail(sub.getStudent().getEmail())
                .submittedAt(sub.getSubmittedAt())
                .answers(answerDtos)
                .description(sub.getDescription())
                .hasImage(hasImage)
                .imageUrl(imageUrl)
                .imageName(sub.getImageName())
                .build();
    }
}
