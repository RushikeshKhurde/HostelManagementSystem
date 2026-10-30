package com.hostel.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyEmailOtpResponse {
    private String message;
    private boolean verified;
    private String registrationToken;
    private long expiresInSeconds;
}
