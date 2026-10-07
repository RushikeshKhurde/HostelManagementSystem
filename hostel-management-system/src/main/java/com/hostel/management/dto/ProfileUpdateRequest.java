package com.hostel.management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Pattern(regexp = "^[^A-Z]*$", message = "Email must be in lowercase.")
    @Pattern(
            regexp = "^[a-zA-Z0-9._%+-]+@([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$",
            message = "Please enter a valid email address (e.g., username@domain.com)."
    )
    private String email;

    // 6-digit OTP code, required only when changing email address
    private String emailOtp;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be exactly 10 digits")
    private String mobileNumber;

    private String gender;
    private LocalDate dateOfBirth;
    private String address;

    // Optional password update
    private String currentPassword;
    private String newPassword;
}