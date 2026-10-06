package com.hostel.management.service;

import com.hostel.management.dto.PaymentRequest;
import com.hostel.management.dto.PaymentResponse;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.Booking;
import com.hostel.management.model.Payment;
import com.hostel.management.model.User;
import com.hostel.management.repository.BookingRepository;
import com.hostel.management.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public PaymentResponse makePayment(User student, PaymentRequest request) {
        // Acquire pessimistic write lock on the booking to prevent concurrent payment race conditions
        Booking booking = bookingRepository.findByIdWithLock(request.getBookingId())
                .orElseThrow(() -> new ApiException("Booking not found", HttpStatus.NOT_FOUND));

        // Enforce payment ownership: student can only pay for their own booking
        if (!booking.getStudent().getId().equals(student.getId())) {
            throw new ApiException("You are not allowed to pay for this booking", HttpStatus.FORBIDDEN);
        }

        if (booking.getStatus() != Booking.BookingStatus.APPROVED) {
            throw new ApiException("Payment can only be made for an approved booking", HttpStatus.BAD_REQUEST);
        }

        // ONE BOOKING = ONE ACTIVE PAYMENT (PENDING or SUCCESS)
        boolean hasActivePayment = paymentRepository.existsByBookingIdAndStatusIn(
                booking.getId(),
                List.of(Payment.PaymentStatus.PENDING, Payment.PaymentStatus.SUCCESS)
        );
        if (hasActivePayment) {
            throw new ApiException("An active payment already exists for this booking.", HttpStatus.CONFLICT);
        }

        Payment.PaymentMethod method;
        try {
            method = Payment.PaymentMethod.valueOf(request.getMethod().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ApiException("Invalid payment method", HttpStatus.BAD_REQUEST);
        }

        BigDecimal amount = booking.getRoom().getPricePerMonth();

        // CASH payments start as PENDING with paidAt = null awaiting staff confirmation.
        // Online payment methods (UPI, CARD, NETBANKING) are instant SUCCESS with paidAt = now.
        Payment.PaymentStatus initialStatus;
        LocalDateTime paidAt;

        if (method == Payment.PaymentMethod.CASH) {
            initialStatus = Payment.PaymentStatus.PENDING;
            paidAt = null;
        } else {
            initialStatus = Payment.PaymentStatus.SUCCESS;
            paidAt = LocalDateTime.now();
        }

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(amount)
                .transactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .method(method)
                .status(initialStatus)
                .paidAt(paidAt)
                .build();

        Payment saved = paymentRepository.save(payment);
        return PaymentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForStudent(Long studentId) {
        return paymentRepository.findByBookingStudentId(studentId).stream()
                .map(PaymentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(PaymentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentResponse confirmCashPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException("Payment not found", HttpStatus.NOT_FOUND));

        if (payment.getMethod() != Payment.PaymentMethod.CASH) {
            throw new ApiException("Only CASH payments can be manually confirmed.", HttpStatus.BAD_REQUEST);
        }

        if (payment.getBooking() == null || payment.getBooking().getStatus() != Booking.BookingStatus.APPROVED) {
            throw new ApiException("Payment can only be confirmed for an APPROVED booking", HttpStatus.BAD_REQUEST);
        }

        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new ApiException("Only pending cash payments can be confirmed.", HttpStatus.BAD_REQUEST);
        }

        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);
        return PaymentResponse.fromEntity(saved);
    }
}
