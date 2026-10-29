package com.hostel.management.service;

import com.hostel.management.dto.LeaveRequestDto;
import com.hostel.management.dto.LeaveResponse;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.LeaveRequest;
import com.hostel.management.model.User;
import com.hostel.management.repository.LeaveRequestRepository;
import com.hostel.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional
    public LeaveResponse applyLeave(User student, LeaveRequestDto request) {
        if (student == null) {
            throw new ApiException("Authentication required to submit leave request", HttpStatus.UNAUTHORIZED);
        }
        if (request == null) {
            throw new ApiException("Leave request payload is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new ApiException("Start date and end date are required", HttpStatus.BAD_REQUEST);
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new ApiException("End date cannot be before start date", HttpStatus.BAD_REQUEST);
        }

        // Serialize operations per student to avoid concurrent creation race conditions
        userRepository.findByIdWithLock(student.getId());

        // Check if student already has an overlapping approved leave
        List<LeaveRequest> overlapping = leaveRequestRepository.findOverlappingLeaves(
                student.getId(),
                LeaveRequest.LeaveStatus.APPROVED,
                request.getStartDate(),
                request.getEndDate()
        );

        if (!overlapping.isEmpty()) {
            LeaveRequest conflict = overlapping.get(0);
            String message = String.format(
                    "Your approved leave from %s to %s prevents you from applying for leave on overlapping dates. You can apply for leave starting %s.",
                    conflict.getStartDate().format(DATE_FORMATTER),
                    conflict.getEndDate().format(DATE_FORMATTER),
                    conflict.getEndDate().plusDays(1).format(DATE_FORMATTER)
            );
            throw new ApiException(message, HttpStatus.BAD_REQUEST);
        }

        String leaveType = request.getLeaveType();
        if (leaveType == null || leaveType.isBlank()) {
            leaveType = "OUTPASS";
        } else {
            leaveType = leaveType.trim().toUpperCase();
        }

        LeaveRequest leave = LeaveRequest.builder()
                .student(student)
                .leaveType(leaveType)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason() != null ? request.getReason().trim() : "")
                .emergencyContact(request.getEmergencyContact() != null ? request.getEmergencyContact().trim() : null)
                .status(LeaveRequest.LeaveStatus.PENDING)
                .build();

        LeaveRequest saved = leaveRequestRepository.save(leave);
        return LeaveResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> getLeavesForStudent(Long studentId) {
        return leaveRequestRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(LeaveResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> getAllLeaves() {
        return leaveRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(LeaveResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public LeaveResponse updateStatus(Long leaveId, String statusStr, String remarks) {
        if (statusStr == null || statusStr.isBlank()) {
            throw new ApiException("Status is required", HttpStatus.BAD_REQUEST);
        }

        LeaveRequest leave = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new ApiException("Leave request not found", HttpStatus.NOT_FOUND));

        LeaveRequest.LeaveStatus newStatus;
        try {
            newStatus = LeaveRequest.LeaveStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid leave status: " + statusStr, HttpStatus.BAD_REQUEST);
        }

        LeaveRequest.LeaveStatus oldStatus = leave.getStatus();

        // Validate state machine transitions: only PENDING -> APPROVED or REJECTED
        if (oldStatus == LeaveRequest.LeaveStatus.APPROVED || oldStatus == LeaveRequest.LeaveStatus.REJECTED) {
            if (oldStatus != newStatus) {
                throw new ApiException("Cannot update leave application that is already " + oldStatus, HttpStatus.BAD_REQUEST);
            }
        }

        // When approving, recheck for overlapping approved leave for the same student, excluding this leave
        if (newStatus == LeaveRequest.LeaveStatus.APPROVED) {
            userRepository.findByIdWithLock(leave.getStudent().getId());

            List<LeaveRequest> conflicts = leaveRequestRepository.findOverlappingLeavesExcluding(
                    leave.getStudent().getId(),
                    LeaveRequest.LeaveStatus.APPROVED,
                    leave.getStartDate(),
                    leave.getEndDate(),
                    leave.getId()
            );

            if (!conflicts.isEmpty()) {
                LeaveRequest conflict = conflicts.get(0);
                String msg = String.format(
                        "Cannot approve leave: student already has an approved leave from %s to %s that overlaps with this request (%s to %s).",
                        conflict.getStartDate().format(DATE_FORMATTER),
                        conflict.getEndDate().format(DATE_FORMATTER),
                        leave.getStartDate().format(DATE_FORMATTER),
                        leave.getEndDate().format(DATE_FORMATTER)
                );
                throw new ApiException(msg, HttpStatus.BAD_REQUEST);
            }
        }

        leave.setStatus(newStatus);
        if (remarks != null) {
            leave.setAdminRemarks(remarks.trim());
        }

        LeaveRequest saved = leaveRequestRepository.save(leave);
        return LeaveResponse.fromEntity(saved);
    }
}
