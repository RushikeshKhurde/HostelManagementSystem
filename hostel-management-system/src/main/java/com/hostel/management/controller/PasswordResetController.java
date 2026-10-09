package com.hostel.management.controller;

import com.hostel.management.dto.*;
import com.hostel.management.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/forgot-password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/request")
    public ResponseEntity<ForgotPasswordResponse> requestOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.requestOtp(request));
    }

    @PostMapping("/resend")
    public ResponseEntity<ForgotPasswordResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return ResponseEntity.ok(passwordResetService.resendOtp(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(passwordResetService.verifyOtp(request));
    }

    @PostMapping("/reset")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.resetPassword(request));
    }
}
