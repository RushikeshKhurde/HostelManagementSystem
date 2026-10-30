package com.hostel.management.controller;

import com.hostel.management.dto.*;
import com.hostel.management.security.UserPrincipal;
import com.hostel.management.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Creates a secure Razorpay order for an approved booking fee.
     * Amount is taken directly from the database to prevent client manipulation.
     */
    @PostMapping("/create-order")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RazorpayOrderResponse> createOrder(@AuthenticationPrincipal UserPrincipal principal,
                                                             @Valid @RequestBody CreateRazorpayOrderRequest request) {
        return ResponseEntity.ok(paymentService.createRazorpayOrder(principal.getUser(), request));
    }

    /**
     * Verifies the cryptographic HMAC-SHA256 signature from Razorpay Checkout.
     * Completes payment only upon authentic verification.
     */
    @PostMapping("/verify-order")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PaymentResponse> verifyOrder(@AuthenticationPrincipal UserPrincipal principal,
                                                       @Valid @RequestBody VerifyRazorpayPaymentRequest request) {
        return ResponseEntity.ok(paymentService.verifyRazorpayPayment(principal.getUser(), request));
    }

    /**
     * Retrieves all pending/unpaid room fees for the currently logged in student.
     */
    @GetMapping("/pending-fees")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<PendingFeeResponse>> getPendingFees(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(paymentService.getPendingFeesForStudent(principal.getUser().getId()));
    }

    /**
     * Preserved legacy/cash payment endpoint for manual cash submission.
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PaymentResponse> pay(@AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.makePayment(principal.getUser(), request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<PaymentResponse>> myPayments(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(paymentService.getPaymentsForStudent(principal.getUser().getId()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<List<PaymentResponse>> allPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<PaymentResponse> confirmPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.confirmCashPayment(id));
    }
}
