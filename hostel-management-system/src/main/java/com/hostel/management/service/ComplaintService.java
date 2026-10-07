package com.hostel.management.service;

import com.hostel.management.dto.ComplaintRequest;
import com.hostel.management.dto.ComplaintResponse;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.Complaint;
import com.hostel.management.model.User;
import com.hostel.management.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;

    @Transactional
    public ComplaintResponse createComplaint(User student, ComplaintRequest req) {
        Complaint.Priority priority = Complaint.Priority.MEDIUM;
        if (req.getPriority() != null && !req.getPriority().isBlank()) {
            try {
                priority = Complaint.Priority.valueOf(req.getPriority().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException("Invalid complaint priority: " + req.getPriority(), HttpStatus.BAD_REQUEST);
            }
        }

        Complaint complaint = Complaint.builder()
                .student(student)
                .title(req.getTitle().trim())
                .category(req.getCategory().trim())
                .description(req.getDescription().trim())
                .roomNumber(req.getRoomNumber() != null ? req.getRoomNumber().trim() : null)
                .priority(priority)
                .status(Complaint.ComplaintStatus.PENDING)
                .build();

        Complaint saved = complaintRepository.save(complaint);
        return ComplaintResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getComplaintsForStudent(Long studentId) {
        return complaintRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(ComplaintResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getAllComplaints() {
        return complaintRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ComplaintResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ComplaintResponse updateStatus(Long complaintId, String statusStr, String adminComment) {
        if (statusStr == null || statusStr.isBlank()) {
            throw new ApiException("Status is required", HttpStatus.BAD_REQUEST);
        }

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ApiException("Complaint not found", HttpStatus.NOT_FOUND));

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
        return ComplaintResponse.fromEntity(saved);
    }
}
