package com.hostel.management.dto;

import com.hostel.management.model.LeaveRequest;
import com.hostel.management.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveResponse {

    private Long id;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String emergencyContact;
    private String status;
    private String adminRemarks;
    private LocalDateTime createdAt;
    private StudentSummary student;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentSummary {
        private Long id;
        private String fullName;
        private String username;
        private String email;
        private String mobileNumber;
    }

    public static LeaveResponse fromEntity(LeaveRequest leave) {
        if (leave == null) return null;

        StudentSummary studentSummary = null;
        if (leave.getStudent() != null) {
            User s = leave.getStudent();
            studentSummary = StudentSummary.builder()
                    .id(s.getId())
                    .fullName(s.getFullName())
                    .username(s.getUsername())
                    .email(s.getEmail())
                    .mobileNumber(s.getMobileNumber())
                    .build();
        }

        return LeaveResponse.builder()
                .id(leave.getId())
                .leaveType(leave.getLeaveType())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .reason(leave.getReason())
                .emergencyContact(leave.getEmergencyContact())
                .status(leave.getStatus() != null ? leave.getStatus().name() : null)
                .adminRemarks(leave.getAdminRemarks())
                .createdAt(leave.getCreatedAt())
                .student(studentSummary)
                .build();
    }
}
