package com.hostel.management.service;

import com.hostel.management.dto.PaymentRequest;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.Booking;
import com.hostel.management.model.Payment;
import com.hostel.management.repository.BookingRepository;
import com.hostel.management.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    public Payment makePayment(PaymentRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ApiException("Booking not found", HttpStatus.NOT_FOUND));

        if (booking.getStatus() != Booking.BookingStatus.APPROVED) {
            throw new ApiException("Payment can only be made for an approved booking", HttpStatus.BAD_REQUEST);
        }

        Payment.PaymentMethod method;
        try {
            method = Payment.PaymentMethod.valueOf(request.getMethod().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException("Invalid payment method", HttpStatus.BAD_REQUEST);
        }

        double amount = booking.getRoom().getPricePerMonth();

        // NOTE: This simulates a successful gateway transaction for demo purposes.
        // In production, integrate a real gateway (Razorpay/Stripe/PayPal) here and
        // NEVER store raw card numbers/CVV - only the gateway's transaction reference.
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(amount)
                .transactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .method(method)
                .status(Payment.PaymentStatus.SUCCESS)
                .build();

        return paymentRepository.save(payment);
    }

    public List<Payment> getPaymentsForStudent(Long studentId) {
        return paymentRepository.findByBookingStudentId(studentId);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
}
