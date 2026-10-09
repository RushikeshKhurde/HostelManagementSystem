package com.hostel.management.dto;

import com.hostel.management.model.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private Long bookingId;
    private BookingSummary booking;
    private BigDecimal amount;
    private String transactionRef;
    private String method;
    private String status;
    private LocalDateTime paidAt;
    private String razorpayOrderId;
    private String razorpayPaymentId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingSummary {
        private Long id;
        private BookingResponse.StudentSummary student;
        private BookingResponse.RoomSummary room;
        private String status;
    }

    public static PaymentResponse fromEntity(Payment payment) {
        if (payment == null) return null;

        BookingSummary bookingSummary = null;
        Long bId = null;

        if (payment.getBooking() != null) {
            bId = payment.getBooking().getId();
            BookingResponse bookingDto = BookingResponse.fromEntity(payment.getBooking());
            bookingSummary = BookingSummary.builder()
                    .id(bookingDto.getId())
                    .student(bookingDto.getStudent())
                    .room(bookingDto.getRoom())
                    .status(bookingDto.getStatus())
                    .build();
        }

        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(bId)
                .booking(bookingSummary)
                .amount(payment.getAmount())
                .transactionRef(payment.getTransactionRef())
                .method(payment.getMethod() != null ? payment.getMethod().name() : null)
                .status(payment.getStatus() != null ? payment.getStatus().name() : null)
                .paidAt(payment.getPaidAt())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .build();
    }
}
