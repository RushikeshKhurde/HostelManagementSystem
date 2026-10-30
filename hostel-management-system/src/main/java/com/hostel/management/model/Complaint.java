package com.hostel.management.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
    private User student;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "feedback", length = 2000)
    private String feedback;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String roomNumber;

    @Column(name = "image_path", length = 255)
    private String imagePath;

    @Column(name = "image_name", length = 255)
    private String imageName;

    @Column(name = "image_content_type", length = 100)
    private String imageContentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ComplaintStatus status = ComplaintStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 30)
    @Builder.Default
    private ComplaintVisibility visibility = ComplaintVisibility.WARDEN_AND_ADMIN;

    @Column(length = 500)
    private String adminComment;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.visibility == null) {
            this.visibility = ComplaintVisibility.WARDEN_AND_ADMIN;
        }
        if ((this.feedback == null || this.feedback.isBlank()) && (this.description != null && !this.description.isBlank())) {
            this.feedback = this.description;
        }
        if (this.title == null || this.title.isBlank()) {
            if (this.feedback != null && this.feedback.length() > 60) {
                this.title = this.feedback.substring(0, 57) + "...";
            } else if (this.feedback != null && !this.feedback.isBlank()) {
                this.title = this.feedback;
            } else {
                this.title = "Complaint";
            }
        }
        if (this.category == null || this.category.isBlank()) {
            this.category = "OTHER";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PostLoad
    protected void onLoad() {
        if (this.visibility == null) {
            this.visibility = ComplaintVisibility.WARDEN_AND_ADMIN;
        }
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, URGENT
    }

    public enum ComplaintStatus {
        PENDING, IN_PROGRESS, RESOLVED, REJECTED
    }
}
