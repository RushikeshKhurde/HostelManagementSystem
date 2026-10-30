package com.hostel.management.service;

import com.hostel.management.dto.ComplaintCreateDto;
import com.hostel.management.dto.ComplaintRequest;
import com.hostel.management.dto.ComplaintResponse;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.Complaint;
import com.hostel.management.model.ComplaintVisibility;
import com.hostel.management.model.NotificationType;
import com.hostel.management.model.Role;
import com.hostel.management.model.User;
import com.hostel.management.repository.ComplaintRepository;
import com.hostel.management.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;

    @Getter
    @AllArgsConstructor
    public static class ComplaintImageResource {
        private final Resource resource;
        private final String contentType;
        private final String filename;
    }

    @Transactional
    public ComplaintResponse createComplaint(User student, ComplaintCreateDto req) {
        if (student == null) {
            throw new ApiException("Authentication required to submit complaint", HttpStatus.UNAUTHORIZED);
        }

        String rawFeedback = req.getFeedback();
        String rawDescription = req.getDescription();

        String feedback = rawFeedback != null ? rawFeedback.trim() : null;
        String description = rawDescription != null ? rawDescription.trim() : null;

        // Backward compatibility: if feedback is missing but description was provided, treat description as feedback
        if ((feedback == null || feedback.isBlank()) && (description != null && !description.isBlank())) {
            feedback = description;
            description = null;
        }

        // Mandatory feedback check
        if (feedback == null || feedback.isBlank()) {
            throw new ApiException("Feedback / complaint message is required", HttpStatus.BAD_REQUEST);
        }

        if (feedback.length() > 2000) {
            throw new ApiException("Feedback / complaint message cannot exceed 2000 characters", HttpStatus.BAD_REQUEST);
        }

        if (description != null && description.length() > 5000) {
            throw new ApiException("Additional description cannot exceed 5000 characters", HttpStatus.BAD_REQUEST);
        }

        // Mandatory visibility check
        String rawVisibility = req.getVisibility();
        if (rawVisibility == null || rawVisibility.isBlank()) {
            throw new ApiException("Complaint visibility is required. Please select who can view this complaint.", HttpStatus.BAD_REQUEST);
        }

        ComplaintVisibility visibility;
        try {
            visibility = ComplaintVisibility.valueOf(rawVisibility.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid complaint visibility: " + rawVisibility + ". Must be WARDEN_ONLY, ADMIN_ONLY, or WARDEN_AND_ADMIN.", HttpStatus.BAD_REQUEST);
        }

        // Priority
        Complaint.Priority priority = Complaint.Priority.MEDIUM;
        if (req.getPriority() != null && !req.getPriority().isBlank()) {
            try {
                priority = Complaint.Priority.valueOf(req.getPriority().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException("Invalid complaint priority: " + req.getPriority(), HttpStatus.BAD_REQUEST);
            }
        }

        // Title
        String title = req.getTitle() != null ? req.getTitle().trim() : null;
        if (title == null || title.isBlank()) {
            if (feedback.length() > 60) {
                title = feedback.substring(0, 57) + "...";
            } else {
                title = feedback;
            }
        }

        // Category
        String category = req.getCategory() != null ? req.getCategory().trim() : null;
        if (category == null || category.isBlank()) {
            category = "OTHER";
        }

        // Handle optional image
        String imagePath = null;
        String imageName = null;
        String imageContentType = null;
        if (req.getImage() != null && !req.getImage().isEmpty()) {
            FileStorageService.StoredFile stored = fileStorageService.storeComplaintImage(req.getImage());
            if (stored != null) {
                imagePath = stored.getFilename();
                imageName = stored.getOriginalFilename();
                imageContentType = stored.getContentType();
            }
        }

        Complaint complaint = Complaint.builder()
                .student(student)
                .title(title)
                .category(category)
                .feedback(feedback)
                .description(description != null && !description.isBlank() ? description : null)
                .roomNumber(req.getRoomNumber() != null && !req.getRoomNumber().isBlank() ? req.getRoomNumber().trim() : null)
                .priority(priority)
                .status(Complaint.ComplaintStatus.PENDING)
                .visibility(visibility)
                .imagePath(imagePath)
                .imageName(imageName)
                .imageContentType(imageContentType)
                .build();

        Complaint saved = complaintRepository.save(complaint);

        // Notifications
        try {
            // 1. Notify Student
            notificationService.createNotification(
                    student,
                    "Complaint Submitted: #" + saved.getId(),
                    "Your grievance regarding '" + saved.getTitle() + "' has been submitted successfully and is pending review.",
                    NotificationType.COMPLAINT
            );

            // 2. Notify Staff according to selected visibility
            List<User> staffToNotify = new ArrayList<>();
            if (visibility == ComplaintVisibility.WARDEN_ONLY) {
                staffToNotify.addAll(userRepository.findByRole(Role.WARDEN));
            } else if (visibility == ComplaintVisibility.ADMIN_ONLY) {
                staffToNotify.addAll(userRepository.findByRole(Role.ADMIN));
            } else {
                staffToNotify.addAll(userRepository.findByRole(Role.ADMIN));
                staffToNotify.addAll(userRepository.findByRole(Role.WARDEN));
            }

            for (User s : staffToNotify) {
                notificationService.createNotification(
                        s,
                        "New Grievance Submitted",
                        "New complaint #" + saved.getId() + " submitted by " + student.getFullName() + ": " + saved.getTitle(),
                        NotificationType.COMPLAINT
                );
            }
        } catch (Exception e) {
            log.warn("Failed to dispatch complaint notification: {}", e.getMessage());
        }

        return ComplaintResponse.fromEntity(saved);
    }

    @Transactional
    public ComplaintResponse createComplaint(User student, ComplaintRequest req) {
        ComplaintCreateDto dto = ComplaintCreateDto.builder()
                .feedback(req.getFeedback())
                .description(req.getDescription())
                .title(req.getTitle())
                .category(req.getCategory())
                .roomNumber(req.getRoomNumber())
                .priority(req.getPriority())
                .visibility(req.getVisibility())
                .build();
        return createComplaint(student, dto);
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getComplaintsForStudent(Long studentId) {
        return complaintRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(ComplaintResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getComplaintsForStaff(User staffUser) {
        if (staffUser == null) {
            throw new ApiException("Authentication required", HttpStatus.UNAUTHORIZED);
        }

        List<ComplaintVisibility> allowedVisibilities = new ArrayList<>();
        if (staffUser.getRole() == Role.ADMIN) {
            allowedVisibilities.add(ComplaintVisibility.ADMIN_ONLY);
            allowedVisibilities.add(ComplaintVisibility.WARDEN_AND_ADMIN);
        } else if (staffUser.getRole() == Role.WARDEN) {
            allowedVisibilities.add(ComplaintVisibility.WARDEN_ONLY);
            allowedVisibilities.add(ComplaintVisibility.WARDEN_AND_ADMIN);
        } else {
            throw new ApiException("Only administrative staff can view staff complaints", HttpStatus.FORBIDDEN);
        }

        return complaintRepository.findByVisibilityInOrderByCreatedAtDesc(allowedVisibilities).stream()
                .map(ComplaintResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getAllComplaints() {
        return complaintRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ComplaintResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintById(Long complaintId, User currentUser) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ApiException("Complaint not found", HttpStatus.NOT_FOUND));

        validateComplaintAccess(complaint, currentUser);
        return ComplaintResponse.fromEntity(complaint);
    }

    @Transactional(readOnly = true)
    public ComplaintImageResource getComplaintImage(Long complaintId, User currentUser) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ApiException("Complaint not found", HttpStatus.NOT_FOUND));

        validateComplaintAccess(complaint, currentUser);

        if (complaint.getImagePath() == null || complaint.getImagePath().isBlank()) {
            throw new ApiException("Complaint does not have an attached image", HttpStatus.NOT_FOUND);
        }

        Resource resource = fileStorageService.loadComplaintImageAsResource(complaint.getImagePath());
        String contentType = complaint.getImageContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "image/jpeg";
        }

        return new ComplaintImageResource(resource, contentType, complaint.getImageName());
    }

    @Transactional
    public ComplaintResponse updateStatus(Long complaintId, String statusStr, String adminComment, User staffUser) {
        if (statusStr == null || statusStr.isBlank()) {
            throw new ApiException("Status is required", HttpStatus.BAD_REQUEST);
        }

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ApiException("Complaint not found", HttpStatus.NOT_FOUND));

        if (staffUser != null) {
            if (staffUser.getRole() != Role.ADMIN && staffUser.getRole() != Role.WARDEN) {
                throw new ApiException("Only administrative staff can update complaint status", HttpStatus.FORBIDDEN);
            }
            validateComplaintAccess(complaint, staffUser);
        }

        Complaint.ComplaintStatus newStatus;
        try {
            newStatus = Complaint.ComplaintStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid complaint status: " + statusStr, HttpStatus.BAD_REQUEST);
        }

        Complaint.ComplaintStatus oldStatus = complaint.getStatus();

        // Validate state machine transitions
        // PENDING -> IN_PROGRESS, RESOLVED, REJECTED
        // IN_PROGRESS -> RESOLVED, REJECTED
        // RESOLVED, REJECTED are terminal
        if (oldStatus == Complaint.ComplaintStatus.RESOLVED || oldStatus == Complaint.ComplaintStatus.REJECTED) {
            if (oldStatus != newStatus) {
                throw new ApiException("Cannot update complaint that is already " + oldStatus, HttpStatus.BAD_REQUEST);
            }
        } else if (oldStatus == Complaint.ComplaintStatus.IN_PROGRESS) {
            if (newStatus == Complaint.ComplaintStatus.PENDING) {
                throw new ApiException("Invalid complaint status transition from IN_PROGRESS to PENDING", HttpStatus.BAD_REQUEST);
            }
        }

        complaint.setStatus(newStatus);
        if (adminComment != null) {
            complaint.setAdminComment(adminComment.trim());
        }

        Complaint saved = complaintRepository.save(complaint);

        // Notify Student of status change
        try {
            String noteMsg = (adminComment != null && !adminComment.isBlank()) ? ". Note: " + adminComment.trim() : ".";
            notificationService.createNotification(
                    saved.getStudent(),
                    "Complaint Status Updated: #" + saved.getId(),
                    "Your grievance #" + saved.getId() + " status has been updated to " + newStatus + noteMsg,
                    NotificationType.COMPLAINT
            );
        } catch (Exception e) {
            log.warn("Failed to dispatch status update notification: {}", e.getMessage());
        }

        return ComplaintResponse.fromEntity(saved);
    }

    @Transactional
    public ComplaintResponse updateStatus(Long complaintId, String statusStr, String adminComment) {
        return updateStatus(complaintId, statusStr, adminComment, null);
    }

    public boolean canAccessComplaint(Complaint complaint, User user) {
        if (complaint == null || user == null) {
            return false;
        }

        // Owning student can view their own complaint
        if (complaint.getStudent() != null && complaint.getStudent().getId().equals(user.getId())) {
            return true;
        }

        ComplaintVisibility visibility = complaint.getVisibility() != null
                ? complaint.getVisibility()
                : ComplaintVisibility.WARDEN_AND_ADMIN;

        if (user.getRole() == Role.ADMIN) {
            return visibility == ComplaintVisibility.ADMIN_ONLY || visibility == ComplaintVisibility.WARDEN_AND_ADMIN;
        }

        if (user.getRole() == Role.WARDEN) {
            return visibility == ComplaintVisibility.WARDEN_ONLY || visibility == ComplaintVisibility.WARDEN_AND_ADMIN;
        }

        return false;
    }

    public void validateComplaintAccess(Complaint complaint, User user) {
        if (user == null) {
            throw new ApiException("Authentication required", HttpStatus.UNAUTHORIZED);
        }

        if (!canAccessComplaint(complaint, user)) {
            throw new ApiException("You are not authorized to view or manage this complaint", HttpStatus.FORBIDDEN);
        }
    }
}
