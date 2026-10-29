package com.hostel.management.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verifications", indexes = {
        @Index(name = "idx_email_verif_email", columnList = "email"),
        @Index(name = "idx_email_verif_token_hash", columnList = "registration_token_hash")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String email;

    @JsonIgnore
    @Column(name = "otp_hash", nullable = false, length = 64)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @Column(name = "resend_count", nullable = false)
    @Builder.Default
    private int resendCount = 0;

    @Column(name = "last_resend_at")
    private LocalDateTime lastResendAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;

    @Column(name = "otp_used", nullable = false)
    @Builder.Default
    private boolean otpUsed = false;

    @JsonIgnore
    @Column(name = "registration_token_hash", length = 64)
    private String registrationTokenHash;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "token_used", nullable = false)
    @Builder.Default
    private boolean tokenUsed = false;

    @Column(name = "token_used_at")
    private LocalDateTime tokenUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
