package com.hostel.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailVerificationOtpRequest {

    @NotBlank(message = "Email is required")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    @Pattern(regexp = "^[^A-Z]*$", message = "Email must be in lowercase.")
    @Pattern(
            regexp = "^[a-zA-Z0-9._%+-]+@([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$",
            message = "Please enter a valid email address (e.g., username@domain.com)."
    )
    private String email;
}
