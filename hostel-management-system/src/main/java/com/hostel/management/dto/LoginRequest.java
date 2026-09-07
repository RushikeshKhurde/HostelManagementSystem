package com.hostel.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    private String username;
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;

    public String getResolvedIdentifier() {
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }
        if (identifier != null && !identifier.trim().isEmpty()) {
            return identifier.trim();
        }
        return "";
    }
}
