package com.hostel.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailVerificationOtpResponse {
    private String message;
    private String email;
    private String destinationMasked;
    private long expiresInSeconds;
    private int resendCooldownSeconds;
}
