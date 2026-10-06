package com.hostel.management.service;

import com.hostel.management.dto.LeaveRequestDto;
import com.hostel.management.dto.LeaveResponse;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.LeaveRequest;
import com.hostel.management.model.User;
import com.hostel.management.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;

    @Transactional
    public LeaveResponse applyLeave(User student, LeaveRequestDto request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new ApiException("Start date and end date are required", HttpStatus.BAD_REQUEST);
        }
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new ApiException("Start date cannot be in the past", HttpStatus.BAD_REQUEST);
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ApiException("End date cannot be before start date", HttpStatus.BAD_REQUEST);
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
                .reason(request.getReason().trim())
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

        leave.setStatus(newStatus);
        if (remarks != null) {
            leave.setAdminRemarks(remarks.trim());
        }

        LeaveRequest saved = leaveRequestRepository.save(leave);
        return LeaveResponse.fromEntity(saved);
    }
}
