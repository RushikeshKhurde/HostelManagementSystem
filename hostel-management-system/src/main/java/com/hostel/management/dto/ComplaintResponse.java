package com.hostel.management.dto;

import com.hostel.management.model.Complaint;
import com.hostel.management.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintResponse {

    private Long id;
    private String title;
    private String category;
    private String description;
    private String roomNumber;
    private String priority;
    private String status;
    private String adminComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
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

    public static ComplaintResponse fromEntity(Complaint complaint) {
        if (complaint == null) return null;

        StudentSummary studentSummary = null;
        if (complaint.getStudent() != null) {
            User s = complaint.getStudent();
            studentSummary = StudentSummary.builder()
                    .id(s.getId())
                    .fullName(s.getFullName())
                    .username(s.getUsername())
                    .email(s.getEmail())
                    .mobileNumber(s.getMobileNumber())
                    .build();
        }

        return ComplaintResponse.builder()
                .id(complaint.getId())
                .title(complaint.getTitle())
                .category(complaint.getCategory())
                .description(complaint.getDescription())
                .roomNumber(complaint.getRoomNumber())
                .priority(complaint.getPriority() != null ? complaint.getPriority().name() : null)
                .status(complaint.getStatus() != null ? complaint.getStatus().name() : null)
                .adminComment(complaint.getAdminComment())
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .student(studentSummary)
                .build();
    }
}
