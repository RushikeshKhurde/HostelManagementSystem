package com.hostel.management.service;

import com.hostel.management.dto.*;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.Booking;
import com.hostel.management.model.Payment;
import com.hostel.management.model.User;
import com.hostel.management.repository.BookingRepository;
import com.hostel.management.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final RazorpayService razorpayService;

    @Value("${app.payment.allow-test-simulation:false}")
    private boolean allowTestSimulation;

    /**
     * Creates a secure Razorpay order for an unpaid approved room booking.
     * Enforces authentication, booking ownership, approval status, and extracts amount strictly from DB.
     * Throws "Razorpay payment gateway is not configured." if credentials are missing or placeholder.
     */
    @Transactional
    public RazorpayOrderResponse createRazorpayOrder(User student, CreateRazorpayOrderRequest request) {
        // Enforce gateway configuration
        if (!razorpayService.isConfigured() && !allowTestSimulation) {
            throw new ApiException("Razorpay payment gateway is not configured.", HttpStatus.BAD_REQUEST);
        }

        // 1. Acquire pessimistic write lock on the booking
        Booking booking = bookingRepository.findByIdWithLock(request.getBookingId())
                .orElseThrow(() -> new ApiException("Booking not found", HttpStatus.NOT_FOUND));

        // 2. Enforce payment ownership: student can only pay for their own booking
        if (!booking.getStudent().getId().equals(student.getId())) {
            throw new ApiException("You are not allowed to pay for another student's booking", HttpStatus.FORBIDDEN);
        }

        // 3. Enforce booking status: must be APPROVED
        if (booking.getStatus() != Booking.BookingStatus.APPROVED) {
            throw new ApiException("Payment can only be made for an approved room booking", HttpStatus.BAD_REQUEST);
        }

        // 4. Verify fee is unpaid: student cannot pay an already successful payment
        boolean alreadyPaid = paymentRepository.existsByBookingIdAndStatus(
                booking.getId(), Payment.PaymentStatus.SUCCESS
        );
        if (alreadyPaid) {
            throw new ApiException("Fee for this booking has already been paid successfully.", HttpStatus.CONFLICT);
        }

        // 5. Amount is obtained strictly from the DATABASE (student cannot tamper or send amount)
        BigDecimal amount = booking.getRoom().getPricePerMonth();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Invalid room rent amount in database", HttpStatus.BAD_REQUEST);
        }

        // 6. Create order with Razorpay
        String receipt = "RCPT-" + booking.getId() + "-" + System.currentTimeMillis();
        Map<String, String> notes = Map.of(
                "bookingId", String.valueOf(booking.getId()),
                "studentId", String.valueOf(student.getId()),
                "studentName", student.getFullName(),
                "roomNumber", booking.getRoom().getRoomNumber()
        );
        String razorpayOrderId = razorpayService.createOrder(amount, receipt, notes);

        // 7. Save or update pending payment record with razorpay order ID
        Payment payment = paymentRepository.findFirstByBookingIdAndStatus(booking.getId(), Payment.PaymentStatus.PENDING)
                .orElse(null);

        if (payment == null) {
            payment = Payment.builder()
                    .booking(booking)
                    .amount(amount)
                    .transactionRef("ORDER-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase())
                    .method(Payment.PaymentMethod.RAZORPAY)
                    .status(Payment.PaymentStatus.PENDING)
                    .razorpayOrderId(razorpayOrderId)
                    .build();
        } else {
            payment.setAmount(amount);
            payment.setMethod(Payment.PaymentMethod.RAZORPAY);
            payment.setRazorpayOrderId(razorpayOrderId);
        }

        Payment saved = paymentRepository.save(payment);

        return RazorpayOrderResponse.builder()
                .orderId(razorpayOrderId)
                .paymentId(saved.getId())
                .bookingId(booking.getId())
                .amount(amount)
                .amountInPaise(amount.multiply(BigDecimal.valueOf(100)).longValue())
                .currency(razorpayService.getCurrency())
                .keyId(razorpayService.getKeyId())
                .companyName(razorpayService.getCompanyName())
                .description("Hostel Rent - Room " + booking.getRoom().getRoomNumber())
                .studentName(student.getFullName())
                .studentEmail(student.getEmail())
                .studentMobile(student.getMobileNumber())
                .roomNumber(booking.getRoom().getRoomNumber())
                .roomType(booking.getRoom().getRoomType() != null ? booking.getRoom().getRoomType().name() : "")
                .build();
    }

    /**
     * Verifies the cryptographic HMAC-SHA256 signature from Razorpay Checkout.
     * Updates payment status to SUCCESS upon authentic verification.
     * Throws "Razorpay payment gateway is not configured." if credentials are missing or placeholder.
     */
    @Transactional
    public PaymentResponse verifyRazorpayPayment(User student, VerifyRazorpayPaymentRequest request) {
        if (!razorpayService.isConfigured() && !allowTestSimulation) {
            throw new ApiException("Razorpay payment gateway is not configured.", HttpStatus.BAD_REQUEST);
        }

        // 1. Cryptographically verify signature using Razorpay Secret
        boolean isValid = razorpayService.verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        // 2. Find existing payment by razorpayOrderId or bookingId
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElse(null);

        if (payment == null && request.getBookingId() != null) {
            payment = paymentRepository.findFirstByBookingIdAndStatus(request.getBookingId(), Payment.PaymentStatus.PENDING)
                    .orElse(null);
        }

        if (payment == null) {
            throw new ApiException("Payment record not found for Razorpay order: " + request.getRazorpayOrderId(), HttpStatus.NOT_FOUND);
        }

        // 3. Enforce student ownership
        if (!payment.getBooking().getStudent().getId().equals(student.getId())) {
            throw new ApiException("You are not authorized to verify this payment", HttpStatus.FORBIDDEN);
        }

        // 4. Idempotency check: if already SUCCESS, return response directly
        if (payment.getStatus() == Payment.PaymentStatus.SUCCESS) {
            return PaymentResponse.fromEntity(payment);
        }

        // 5. Signature validation check
        if (!isValid) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new ApiException("Payment verification failed: Invalid Razorpay cryptographic signature", HttpStatus.BAD_REQUEST);
        }

        // 6. Save as SUCCESS with authentic transaction reference and timestamp
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setTransactionRef(request.getRazorpayPaymentId()); // Authentic Razorpay reference e.g. pay_XXXXX

        if (request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank()) {
            try {
                payment.setMethod(Payment.PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()));
            } catch (Exception e) {
                payment.setMethod(Payment.PaymentMethod.RAZORPAY);
            }
        } else {
            payment.setMethod(Payment.PaymentMethod.RAZORPAY);
        }

        Payment saved = paymentRepository.save(payment);
        log.info("Payment #{} successfully verified via Razorpay! Ref: {}", saved.getId(), saved.getTransactionRef());
        return PaymentResponse.fromEntity(saved);
    }

    /**
     * Retrieves all pending/unpaid fees for a given student.
     */
    @Transactional(readOnly = true)
    public List<PendingFeeResponse> getPendingFeesForStudent(Long studentId) {
        List<Booking> approvedBookings = bookingRepository.findByStudentIdAndStatusIn(
                studentId, List.of(Booking.BookingStatus.APPROVED)
        );

        return approvedBookings.stream().map(b -> {
            List<Payment> payments = paymentRepository.findByBookingId(b.getId());
            boolean isPaid = payments.stream().anyMatch(p -> p.getStatus() == Payment.PaymentStatus.SUCCESS);

            BigDecimal totalRent = b.getRoom().getPricePerMonth();
            BigDecimal amountPaid = payments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.SUCCESS)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remaining = totalRent.subtract(amountPaid).max(BigDecimal.ZERO);

            Payment lastPayment = payments.stream()
                    .filter(p -> p.getStatus() != null)
                    .reduce((first, second) -> second)
                    .orElse(null);

            return PendingFeeResponse.builder()
                    .bookingId(b.getId())
                    .roomId(b.getRoom().getId())
                    .roomNumber(b.getRoom().getRoomNumber())
                    .roomType(b.getRoom().getRoomType() != null ? b.getRoom().getRoomType().name() : "")
                    .monthlyRent(totalRent)
                    .amountPaid(amountPaid)
                    .remainingAmount(remaining)
                    .checkInDate(b.getCheckInDate())
                    .bookingStatus(b.getStatus().name())
                    .paid(isPaid)
                    .lastPaymentStatus(lastPayment != null ? lastPayment.getStatus().name() : null)
                    .lastPaymentMethod(lastPayment != null ? lastPayment.getMethod().name() : null)
                    .lastTransactionRef(lastPayment != null ? lastPayment.getTransactionRef() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Preserved for CASH payments and manual confirmation.
     * Rejects online payments in normal application runtime if not completed via Razorpay.
     */
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
        Payment.PaymentStatus initialStatus;
        LocalDateTime paidAt;

        if (method == Payment.PaymentMethod.CASH) {
            initialStatus = Payment.PaymentStatus.PENDING;
            paidAt = null;
        } else {
            // Online payment methods (UPI, CARD, NETBANKING)
            if (!allowTestSimulation) {
                if (!razorpayService.isConfigured()) {
                    throw new ApiException("Razorpay payment gateway is not configured.", HttpStatus.BAD_REQUEST);
                }
                throw new ApiException("Online payments must be processed securely through the Razorpay payment gateway.", HttpStatus.BAD_REQUEST);
            }
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
