package com.hostel.management.controller;

import com.hostel.management.dto.FeedbackQuestionDto;
import com.hostel.management.dto.FeedbackSubmissionDto;
import com.hostel.management.dto.FeedbackSubmitRequest;
import com.hostel.management.model.Role;
import com.hostel.management.security.UserPrincipal;
import com.hostel.management.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

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
     * Submit feedback questionnaire.
     * Accessible ONLY to Students (ROLE_USER). Student identity is taken strictly from JWT.
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FeedbackSubmissionDto> submitFeedback(
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
