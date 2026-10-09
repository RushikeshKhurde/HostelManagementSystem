package com.hostel.management.repository;

import com.hostel.management.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBookingStudentId(Long studentId);
    List<Payment> findByBookingId(Long bookingId);
    List<Payment> findByBookingIdAndStatus(Long bookingId, Payment.PaymentStatus status);
    Optional<Payment> findFirstByBookingIdAndStatus(Long bookingId, Payment.PaymentStatus status);
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);
    boolean existsByBookingIdAndStatus(Long bookingId, Payment.PaymentStatus status);
    boolean existsByBookingIdAndStatusIn(Long bookingId, Collection<Payment.PaymentStatus> statuses);
}
