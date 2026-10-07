package com.hostel.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendOtpRequest {

    @NotBlank(message = "Identifier (email or mobile number) is required")
    private String identifier;

    // Optional type: EMAIL or MOBILE (defaults to EMAIL if identifier contains '@')
    private String type;
}
