package com.hostel.management.controller;

import com.hostel.management.dto.SendEmailVerificationOtpRequest;
import com.hostel.management.dto.SendEmailVerificationOtpResponse;
import com.hostel.management.dto.VerifyEmailOtpRequest;
import com.hostel.management.dto.VerifyEmailOtpResponse;
import com.hostel.management.service.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/email-verification")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/send-otp")
    public ResponseEntity<SendEmailVerificationOtpResponse> sendOtp(
            @Valid @RequestBody SendEmailVerificationOtpRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        return ResponseEntity.ok(emailVerificationService.sendOtp(request, clientIp));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyEmailOtpResponse> verifyOtp(
            @Valid @RequestBody VerifyEmailOtpRequest request) {
        return ResponseEntity.ok(emailVerificationService.verifyOtp(request));
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
