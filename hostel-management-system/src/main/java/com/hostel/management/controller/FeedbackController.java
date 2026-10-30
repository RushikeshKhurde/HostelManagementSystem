package com.hostel.management.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.management.dto.FeedbackAnswerRequest;
import com.hostel.management.dto.FeedbackQuestionDto;
import com.hostel.management.dto.FeedbackSubmissionDto;
import com.hostel.management.dto.FeedbackSubmitRequest;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.Role;
import com.hostel.management.security.UserPrincipal;
import com.hostel.management.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@Slf4j
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final ObjectMapper objectMapper;

    /**
     * Get active questionnaire questions and options.
     * Accessible to all authenticated users (Students, Warden, Admin).
     */
    @GetMapping("/questions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FeedbackQuestionDto>> getActiveQuestions() {
        return ResponseEntity.ok(feedbackService.getActiveQuestions());
    }

    /**
     * Submit feedback questionnaire with optional image attachment and description (multipart).
     * Accessible ONLY to Students (ROLE_USER). Student identity is taken strictly from JWT.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FeedbackSubmissionDto> submitFeedbackMultipart(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("answers") String answersJson,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        List<FeedbackAnswerRequest> answers;
        try {
            answers = objectMapper.readValue(answersJson, new TypeReference<List<FeedbackAnswerRequest>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse feedback answers JSON: {}", e.getMessage());
            throw new ApiException("Invalid answers payload format. Expected list of question/option answers.", HttpStatus.BAD_REQUEST);
        }

        FeedbackSubmitRequest request = FeedbackSubmitRequest.builder()
                .answers(answers)
                .description(description)
                .build();

        FeedbackSubmissionDto dto = feedbackService.submitFeedback(principal.getUser(), request, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Submit feedback questionnaire via JSON (backward compatibility).
     * Accessible ONLY to Students (ROLE_USER). Student identity is taken strictly from JWT.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FeedbackSubmissionDto> submitFeedbackJson(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FeedbackSubmitRequest request) {
        FeedbackSubmissionDto dto = feedbackService.submitFeedback(principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * View authenticated student's submitted feedback.
     * Accessible ONLY to Students (ROLE_USER).
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FeedbackSubmissionDto> getMyFeedback(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(feedbackService.getMyFeedback(principal.getUser()));
    }

    /**
     * Retrieve feedback image attachment.
     * Protected by IDOR access control: Only the owning student or authorized staff (ADMIN, WARDEN) can access.
     */
    @GetMapping("/{id}/image")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'WARDEN')")
    public ResponseEntity<Resource> getFeedbackImage(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        FeedbackService.FeedbackImageResource imageInfo = feedbackService.getFeedbackImage(id, principal.getUser());
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(imageInfo.getContentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (imageInfo.getFilename() != null ? imageInfo.getFilename() : "feedback-image") + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(imageInfo.getResource());
    }

    /**
     * Delete feedback by ID.
     * - Students (ROLE_USER) can delete ONLY their own feedback (enforced via IDOR check).
     * - Admin (ROLE_ADMIN) can delete any feedback.
     * - Warden (ROLE_WARDEN) receives 403 Forbidden.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, String>> deleteFeedback(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getUser().getRole() == Role.ADMIN) {
            feedbackService.adminDeleteFeedback(id);
        } else {
            feedbackService.deleteMyFeedback(principal.getUser(), id);
        }
        return ResponseEntity.ok(Map.of("message", "Feedback deleted successfully."));
    }

    /**
     * Dedicated Admin endpoint to delete any student feedback.
     * Accessible ONLY to ADMIN.
     */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminDeleteFeedback(@PathVariable Long id) {
        feedbackService.adminDeleteFeedback(id);
        return ResponseEntity.ok(Map.of("message", "Feedback deleted successfully by administrator."));
    }

    /**
     * View all student feedback submissions.
     * Accessible to ADMIN and WARDEN (Rector).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<List<FeedbackSubmissionDto>> getAllFeedback() {
        return ResponseEntity.ok(feedbackService.getAllFeedback());
    }
}
