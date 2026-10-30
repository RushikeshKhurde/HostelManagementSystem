package com.hostel.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingFeeResponse {

    private Long bookingId;
    private Long roomId;
    private String roomNumber;
    private String roomType;
    private BigDecimal monthlyRent;
    private BigDecimal amountPaid;
    private BigDecimal remainingAmount;
    private LocalDate checkInDate;
    private String bookingStatus;
    private boolean paid;
    private String lastPaymentStatus;
    private String lastPaymentMethod;
    private String lastTransactionRef;
}
