package com.hostel.management.controller;

import com.hostel.management.dto.AuthResponse;
import com.hostel.management.dto.LoginRequest;
import com.hostel.management.dto.RegisterRequest;
import com.hostel.management.dto.ResetPasswordRequest;
import com.hostel.management.dto.SendOtpRequest;
import com.hostel.management.dto.VerifyOtpRequest;
import com.hostel.management.security.UserPrincipal;
import com.hostel.management.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal));
    }

    @PostMapping("/forgot-password/request-otp")
    public ResponseEntity<Map<String, Object>> forgotPasswordRequestOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.forgotPasswordRequestOtp(request.getIdentifier(), request.getType());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password reset OTP sent successfully."
        ));
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<Map<String, Object>> forgotPasswordVerifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String resetToken = authService.forgotPasswordVerifyOtp(request.getIdentifier(), request.getType(), request.getOtp());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "resetToken", resetToken,
                "message", "OTP verified successfully. You can now reset your password."
        ));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<Map<String, Object>> forgotPasswordReset(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password has been reset successfully. You can now log in."
        ));
    }
}
