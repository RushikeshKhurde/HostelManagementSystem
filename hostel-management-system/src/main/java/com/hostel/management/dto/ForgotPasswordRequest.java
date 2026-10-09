package com.hostel.management.dto;

import com.hostel.management.model.VerificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest {

    @Builder.Default
    private VerificationChannel method = VerificationChannel.EMAIL;

    @NotBlank(message = "Registered email address is required")
    private String identifier;
}
