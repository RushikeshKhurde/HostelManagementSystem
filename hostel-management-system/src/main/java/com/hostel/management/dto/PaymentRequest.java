package com.hostel.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotNull(message = "Booking id is required")
    private Long bookingId;

    @NotNull(message = "Payment method is required")
    private String method; // CARD, UPI, NETBANKING, CASH

    // Optional - card/upi details are NOT stored, only used to simulate gateway call
    private String payerDetail;
}
