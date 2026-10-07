package com.hostel.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank(message = "Identifier (email or mobile number) is required")
    private String identifier;

    @NotBlank(message = "OTP is required")
    private String otp;

    private String type;
}
