package com.hostel.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderResponse {

    private String orderId;
    private Long paymentId;
    private Long bookingId;
    private BigDecimal amount; // In rupees
    private Long amountInPaise; // In paise (amount * 100)
    private String currency; // "INR"
    private String keyId; // Razorpay public key
    private String companyName;
    private String description;
    private String studentName;
    private String studentEmail;
    private String studentMobile;
    private String roomNumber;
    private String roomType;
}
