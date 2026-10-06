package com.hostel.management.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
    private String fullName;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "Username can only contain letters, digits, dots, and underscores")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = ValidationConstants.MOBILE_PATTERN, message = ValidationConstants.MOBILE_MESSAGE)
    private String mobileNumber;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = ValidationConstants.PASSWORD_PATTERN,
            message = ValidationConstants.PASSWORD_MESSAGE
    )
    private String password;

    private String confirmPassword;

    private String gender;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private String address;
}
