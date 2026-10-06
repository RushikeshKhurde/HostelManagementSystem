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
}
