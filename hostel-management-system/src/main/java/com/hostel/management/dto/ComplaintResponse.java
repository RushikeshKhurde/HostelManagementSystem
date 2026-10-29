package com.hostel.management.dto;

import com.hostel.management.model.Complaint;
import com.hostel.management.model.ComplaintVisibility;
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
    private String feedback;
    private String description;
    private String roomNumber;
    private String priority;
    private String status;
    private String visibility;
    private String adminComment;
    private boolean hasImage;
    private String imageUrl;
    private String imageName;
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

        boolean hasImage = complaint.getImagePath() != null && !complaint.getImagePath().isBlank();
        String imageUrl = hasImage ? "/api/complaints/" + complaint.getId() + "/image" : null;
        String feedback = complaint.getFeedback() != null && !complaint.getFeedback().isBlank()
                ? complaint.getFeedback()
                : complaint.getDescription();

        String visibility = complaint.getVisibility() != null
                ? complaint.getVisibility().name()
                : ComplaintVisibility.WARDEN_AND_ADMIN.name();

        return ComplaintResponse.builder()
                .id(complaint.getId())
                .title(complaint.getTitle())
                .category(complaint.getCategory())
                .feedback(feedback)
                .description(complaint.getDescription())
                .roomNumber(complaint.getRoomNumber())
                .priority(complaint.getPriority() != null ? complaint.getPriority().name() : null)
                .status(complaint.getStatus() != null ? complaint.getStatus().name() : null)
                .visibility(visibility)
                .adminComment(complaint.getAdminComment())
                .hasImage(hasImage)
                .imageUrl(imageUrl)
                .imageName(complaint.getImageName())
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .student(studentSummary)
                .build();
    }
}
