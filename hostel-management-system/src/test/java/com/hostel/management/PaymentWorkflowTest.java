package com.hostel.management;

import com.hostel.management.dto.BookingRequest;
import com.hostel.management.dto.BookingResponse;
import com.hostel.management.dto.PaymentRequest;
import com.hostel.management.dto.PaymentResponse;
import com.hostel.management.dto.RoomRequest;
import com.hostel.management.dto.RoomResponse;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.Payment;
import com.hostel.management.model.Role;
import com.hostel.management.model.Room;
import com.hostel.management.model.User;
import com.hostel.management.repository.BookingRepository;
import com.hostel.management.repository.PaymentRepository;
import com.hostel.management.repository.UserRepository;
import com.hostel.management.service.BookingService;
import com.hostel.management.service.PaymentService;
import com.hostel.management.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class PaymentWorkflowTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private User student1;
    private User student2;
    private RoomResponse room;
    private BookingResponse approvedBooking;

    @BeforeEach
    void setUp() {
        String ts = String.valueOf(System.currentTimeMillis());
        student1 = userRepository.save(User.builder()
                .fullName("Student One")
                .username("pstudent1_" + ts)
                .email("pstudent1_" + ts + "@test.com")
                .mobileNumber("9" + ts.substring(ts.length() - 9))
                .password("Password@123")
                .role(Role.USER)
                .build());

        student2 = userRepository.save(User.builder()
                .fullName("Student Two")
                .username("pstudent2_" + ts)
                .email("pstudent2_" + ts + "@test.com")
                .mobileNumber("8" + ts.substring(ts.length() - 9))
                .password("Password@123")
                .role(Role.USER)
                .build());

        room = roomService.addRoom(RoomRequest.builder()
                .roomNumber("PAY-" + ts.substring(ts.length() - 5))
                .roomType(Room.RoomType.SINGLE)
                .capacity(1)
                .pricePerMonth(BigDecimal.valueOf(6500.00))
                .status(Room.RoomStatus.AVAILABLE)
                .build());

        BookingRequest bookingReq = new BookingRequest();
        bookingReq.setRoomId(room.getId());
        bookingReq.setCheckInDate(LocalDate.now().plusDays(1));

        BookingResponse pending = bookingService.createBooking(student1, bookingReq);
        approvedBooking = bookingService.updateStatus(pending.getId(), "APPROVED");
    }

    @Test
    @DisplayName("Student cannot pay for another student's booking (403 Forbidden)")
    void testStudentCannotPayOtherStudentBooking() {
        PaymentRequest payReq = new PaymentRequest();
        payReq.setBookingId(approvedBooking.getId());
        payReq.setMethod("UPI");

        ApiException ex = assertThrows(ApiException.class, () -> paymentService.makePayment(student2, payReq));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("not allowed"));
    }

    @Test
    @DisplayName("CASH payment starts PENDING with paidAt == null and confirmation sets SUCCESS and paidAt")
    void testCashPaymentLifecycle() {
        PaymentRequest payReq = new PaymentRequest();
        payReq.setBookingId(approvedBooking.getId());
        payReq.setMethod("CASH");

        PaymentResponse payment = paymentService.makePayment(student1, payReq);
        assertNotNull(payment);
        assertEquals("PENDING", payment.getStatus());
        assertNull(payment.getPaidAt(), "CASH payment paidAt must be null while PENDING");

        PaymentResponse confirmed = paymentService.confirmCashPayment(payment.getId());
        assertEquals("SUCCESS", confirmed.getStatus());
        assertNotNull(confirmed.getPaidAt(), "Confirmed CASH payment must have a valid paidAt timestamp");
    }

    @Test
    @DisplayName("Online payment creates SUCCESS status immediately with paidAt timestamp")
    void testOnlinePaymentLifecycle() {
        PaymentRequest payReq = new PaymentRequest();
        payReq.setBookingId(approvedBooking.getId());
        payReq.setMethod("UPI");

        PaymentResponse payment = paymentService.makePayment(student1, payReq);
        assertNotNull(payment);
        assertEquals("SUCCESS", payment.getStatus());
        assertEquals(0, payment.getAmount().compareTo(BigDecimal.valueOf(6500.00)));
        assertNotNull(payment.getTransactionRef());
        assertNotNull(payment.getPaidAt(), "Online payment must immediately have paidAt timestamp");
    }

    @Test
    @DisplayName("Non-CASH payment cannot be confirmed through cash confirmation endpoint (400 Bad Request)")
    void testNonCashCannotBeConfirmed() {
        PaymentRequest payReq = new PaymentRequest();
        payReq.setBookingId(approvedBooking.getId());
        payReq.setMethod("CARD");

        PaymentResponse payment = paymentService.makePayment(student1, payReq);
        assertEquals("SUCCESS", payment.getStatus());

        ApiException ex = assertThrows(ApiException.class, () -> paymentService.confirmCashPayment(payment.getId()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Only CASH payments"));
    }

    @Test
    @DisplayName("FAILED payment cannot be manually confirmed (400 Bad Request)")
    void testFailedPaymentCannotBeConfirmed() {
        Payment failedPayment = paymentRepository.save(Payment.builder()
                .booking(bookingRepository.findById(approvedBooking.getId()).orElseThrow())
                .amount(BigDecimal.valueOf(6500.00))
                .transactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 8))
                .method(Payment.PaymentMethod.CASH)
                .status(Payment.PaymentStatus.FAILED)
                .paidAt(null)
                .build());

        ApiException ex = assertThrows(ApiException.class, () -> paymentService.confirmCashPayment(failedPayment.getId()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Only pending cash payments can be confirmed"));
    }

    @Test
    @DisplayName("REFUNDED payment cannot be manually confirmed (400 Bad Request)")
    void testRefundedPaymentCannotBeConfirmed() {
        Payment refundedPayment = paymentRepository.save(Payment.builder()
                .booking(bookingRepository.findById(approvedBooking.getId()).orElseThrow())
                .amount(BigDecimal.valueOf(6500.00))
                .transactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 8))
                .method(Payment.PaymentMethod.CASH)
                .status(Payment.PaymentStatus.REFUNDED)
                .paidAt(null)
                .build());

        ApiException ex = assertThrows(ApiException.class, () -> paymentService.confirmCashPayment(refundedPayment.getId()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Only pending cash payments can be confirmed"));
    }

    @Test
    @DisplayName("Second payment while first is PENDING returns 409 Conflict")
    void testSecondPaymentWhilePendingReturns409() {
        PaymentRequest cashReq = new PaymentRequest();
        cashReq.setBookingId(approvedBooking.getId());
        cashReq.setMethod("CASH");

        paymentService.makePayment(student1, cashReq);

        PaymentRequest upiReq = new PaymentRequest();
        upiReq.setBookingId(approvedBooking.getId());
        upiReq.setMethod("UPI");

        ApiException ex = assertThrows(ApiException.class, () -> paymentService.makePayment(student1, upiReq));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("An active payment already exists"));
    }

    @Test
    @DisplayName("Second payment after SUCCESS returns 409 Conflict")
    void testSecondPaymentAfterSuccessReturns409() {
        PaymentRequest upiReq = new PaymentRequest();
        upiReq.setBookingId(approvedBooking.getId());
        upiReq.setMethod("UPI");

        paymentService.makePayment(student1, upiReq);

        PaymentRequest cashReq = new PaymentRequest();
        cashReq.setBookingId(approvedBooking.getId());
        cashReq.setMethod("CASH");

        ApiException ex = assertThrows(ApiException.class, () -> paymentService.makePayment(student1, cashReq));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("An active payment already exists"));
    }

    @Test
    @DisplayName("Payment list retrieval returns structured DTOs with booking summaries")
    void testPaymentListDtos() {
        PaymentRequest payReq = new PaymentRequest();
        payReq.setBookingId(approvedBooking.getId());
        payReq.setMethod("UPI");
        paymentService.makePayment(student1, payReq);

        List<PaymentResponse> studentPayments = paymentService.getPaymentsForStudent(student1.getId());
        assertFalse(studentPayments.isEmpty());
        assertNotNull(studentPayments.get(0).getBooking());
        assertNotNull(studentPayments.get(0).getBooking().getRoom());

        List<PaymentResponse> allPayments = paymentService.getAllPayments();
        assertFalse(allPayments.isEmpty());
    }

    @Test
    @DisplayName("Create Razorpay Order success with DB-driven amount")
    void testCreateRazorpayOrderSuccess() {
        com.hostel.management.dto.CreateRazorpayOrderRequest req = new com.hostel.management.dto.CreateRazorpayOrderRequest();
        req.setBookingId(approvedBooking.getId());

        com.hostel.management.dto.RazorpayOrderResponse order = paymentService.createRazorpayOrder(student1, req);
        assertNotNull(order);
        assertNotNull(order.getOrderId());
        assertEquals(approvedBooking.getId(), order.getBookingId());
        assertEquals(0, order.getAmount().compareTo(BigDecimal.valueOf(6500.00)));
        assertEquals(650000L, order.getAmountInPaise());
        assertEquals("INR", order.getCurrency());
        assertEquals(student1.getFullName(), order.getStudentName());
    }

    @Test
    @DisplayName("Create Razorpay Order for another student's booking returns 403 Forbidden")
    void testCreateRazorpayOrderForbiddenForOtherStudent() {
        com.hostel.management.dto.CreateRazorpayOrderRequest req = new com.hostel.management.dto.CreateRazorpayOrderRequest();
        req.setBookingId(approvedBooking.getId());

        ApiException ex = assertThrows(ApiException.class, () -> paymentService.createRazorpayOrder(student2, req));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("not allowed"));
    }

    @Test
    @DisplayName("Create Razorpay Order when fee already successfully paid returns 409 Conflict")
    void testCreateRazorpayOrderAlreadyPaidConflict() {
        PaymentRequest payReq = new PaymentRequest();
        payReq.setBookingId(approvedBooking.getId());
        payReq.setMethod("UPI");
        paymentService.makePayment(student1, payReq);

        com.hostel.management.dto.CreateRazorpayOrderRequest req = new com.hostel.management.dto.CreateRazorpayOrderRequest();
        req.setBookingId(approvedBooking.getId());

        ApiException ex = assertThrows(ApiException.class, () -> paymentService.createRazorpayOrder(student1, req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("already been paid"));
    }

    @Test
    @DisplayName("Verify Razorpay Payment with valid signature updates status to SUCCESS")
    void testVerifyRazorpayPaymentSuccess() {
        com.hostel.management.dto.CreateRazorpayOrderRequest orderReq = new com.hostel.management.dto.CreateRazorpayOrderRequest();
        orderReq.setBookingId(approvedBooking.getId());
        com.hostel.management.dto.RazorpayOrderResponse order = paymentService.createRazorpayOrder(student1, orderReq);

        String paymentId = "pay_" + UUID.randomUUID().toString().substring(0, 10);
        com.hostel.management.dto.VerifyRazorpayPaymentRequest verifyReq = com.hostel.management.dto.VerifyRazorpayPaymentRequest.builder()
                .razorpayOrderId(order.getOrderId())
                .razorpayPaymentId(paymentId)
                .razorpaySignature("test_signature")
                .bookingId(approvedBooking.getId())
                .paymentMethod("UPI")
                .build();

        PaymentResponse verified = paymentService.verifyRazorpayPayment(student1, verifyReq);
        assertNotNull(verified);
        assertEquals("SUCCESS", verified.getStatus());
        assertEquals(paymentId, verified.getTransactionRef());
        assertNotNull(verified.getPaidAt());
        assertEquals(0, verified.getAmount().compareTo(BigDecimal.valueOf(6500.00)));
    }

    @Test
    @DisplayName("Get pending fees for student correctly indicates unpaid vs paid state")
    void testGetPendingFeesForStudent() {
        List<com.hostel.management.dto.PendingFeeResponse> fees = paymentService.getPendingFeesForStudent(student1.getId());
        assertFalse(fees.isEmpty());
        com.hostel.management.dto.PendingFeeResponse fee = fees.get(0);
        assertFalse(fee.isPaid());
        assertEquals(0, fee.getMonthlyRent().compareTo(BigDecimal.valueOf(6500.00)));
        assertEquals(0, fee.getRemainingAmount().compareTo(BigDecimal.valueOf(6500.00)));

        // Now pay it
        PaymentRequest payReq = new PaymentRequest();
        payReq.setBookingId(approvedBooking.getId());
        payReq.setMethod("UPI");
        paymentService.makePayment(student1, payReq);

        List<com.hostel.management.dto.PendingFeeResponse> updatedFees = paymentService.getPendingFeesForStudent(student1.getId());
        assertTrue(updatedFees.get(0).isPaid());
        assertEquals(0, updatedFees.get(0).getRemainingAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Razorpay order creation throws clear error when gateway is not configured")
    void testRazorpayNotConfiguredThrowsClearError() {
        com.hostel.management.service.RazorpayService unconfiguredService = new com.hostel.management.service.RazorpayService();
        // unconfiguredService has default empty keys and allowTestSimulation = false
        ApiException ex = assertThrows(ApiException.class, () ->
            unconfiguredService.createOrder(BigDecimal.valueOf(1000), "rcpt_1", java.util.Map.of())
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Razorpay payment gateway is not configured"));

        ApiException verifyEx = assertThrows(ApiException.class, () ->
            unconfiguredService.verifySignature("order_123", "pay_123", "sig_123")
        );
        assertEquals(HttpStatus.BAD_REQUEST, verifyEx.getStatus());
        assertTrue(verifyEx.getMessage().contains("Razorpay payment gateway is not configured"));
    }
}

