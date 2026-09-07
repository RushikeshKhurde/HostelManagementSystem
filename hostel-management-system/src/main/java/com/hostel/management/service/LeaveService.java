package com.hostel.management.service;

import com.hostel.management.exception.ApiException;
import com.hostel.management.model.LeaveRequest;
import com.hostel.management.model.User;
import com.hostel.management.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;

    public LeaveRequest applyLeave(User student, LeaveRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new ApiException("Start date and end date are required", HttpStatus.BAD_REQUEST);
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ApiException("End date cannot be before start date", HttpStatus.BAD_REQUEST);
        }
        if (request.getLeaveType() == null || request.getLeaveType().isBlank()) {
            request.setLeaveType("OUTPASS");
        }

        request.setStudent(student);
        request.setStatus(LeaveRequest.LeaveStatus.PENDING);
        return leaveRequestRepository.save(request);
    }

    public List<LeaveRequest> getLeavesForStudent(Long studentId) {
        return leaveRequestRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    public List<LeaveRequest> getAllLeaves() {
        return leaveRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    public LeaveRequest updateStatus(Long leaveId, String statusStr, String remarks) {
        LeaveRequest leave = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new ApiException("Leave request not found", HttpStatus.NOT_FOUND));

        try {
            leave.setStatus(LeaveRequest.LeaveStatus.valueOf(statusStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid leave status", HttpStatus.BAD_REQUEST);
        }

        if (remarks != null) {
            leave.setAdminRemarks(remarks);
        }

        return leaveRequestRepository.save(leave);
    }
}
