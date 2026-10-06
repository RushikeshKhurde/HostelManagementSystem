package com.hostel.management;

import com.hostel.management.dto.BookingRequest;
import com.hostel.management.dto.BookingResponse;
import com.hostel.management.dto.RoomRequest;
import com.hostel.management.dto.RoomResponse;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.Role;
import com.hostel.management.model.Room;
import com.hostel.management.model.User;
import com.hostel.management.repository.BookingRepository;
import com.hostel.management.repository.RoomRepository;
import com.hostel.management.repository.UserRepository;
import com.hostel.management.service.BookingService;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class BookingWorkflowTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private User testStudent;
    private RoomResponse testRoom;

    @BeforeEach
    void setUp() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        testStudent = userRepository.save(User.builder()
                .fullName("Booking Test Student")
                .username("bstudent_" + timestamp)
                .email("bstudent_" + timestamp + "@test.com")
                .mobileNumber("9" + timestamp.substring(timestamp.length() - 9))
                .password("Password@123")
                .role(Role.USER)
                .build());

        testRoom = roomService.addRoom(RoomRequest.builder()
                .roomNumber("TEST-" + timestamp.substring(timestamp.length() - 5))
                .roomType(Room.RoomType.SINGLE)
                .capacity(1)
                .pricePerMonth(BigDecimal.valueOf(5000.00))
                .status(Room.RoomStatus.AVAILABLE)
                .build());
    }

    @Test
    @DisplayName("Past check-in date is rejected with 400 Bad Request")
    void testPastCheckInDateRejected() {
        BookingRequest req = new BookingRequest();
        req.setRoomId(testRoom.getId());
        req.setCheckInDate(LocalDate.now().minusDays(2));

        ApiException ex = assertThrows(ApiException.class, () -> bookingService.createBooking(testStudent, req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("past"));
    }

    @Test
    @DisplayName("Duplicate active booking is rejected with 409 Conflict")
    void testDuplicateActiveBookingRejected() {
        BookingRequest req = new BookingRequest();
        req.setRoomId(testRoom.getId());
        req.setCheckInDate(LocalDate.now().plusDays(2));

        BookingResponse firstBooking = bookingService.createBooking(testStudent, req);
        assertNotNull(firstBooking);
        assertEquals("PENDING", firstBooking.getStatus());

        ApiException ex = assertThrows(ApiException.class, () -> bookingService.createBooking(testStudent, req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    @DisplayName("Approval increments room occupancy and updates status to FULL")
    void testApprovalIncrementsOccupancy() {
        BookingRequest req = new BookingRequest();
        req.setRoomId(testRoom.getId());
        req.setCheckInDate(LocalDate.now().plusDays(1));

        BookingResponse booking = bookingService.createBooking(testStudent, req);
        BookingResponse approved = bookingService.updateStatus(booking.getId(), "APPROVED");

        assertEquals("APPROVED", approved.getStatus());

        Room reloaded = roomRepository.findById(testRoom.getId()).orElseThrow();
        assertEquals(1, reloaded.getOccupied());
        assertEquals(Room.RoomStatus.FULL, reloaded.getStatus());
    }

    @Test
    @DisplayName("Invalid status transition (REJECTED -> APPROVED) is rejected with 400 Bad Request")
    void testInvalidStatusTransitionRejected() {
        BookingRequest req = new BookingRequest();
        req.setRoomId(testRoom.getId());
        req.setCheckInDate(LocalDate.now().plusDays(1));

        BookingResponse booking = bookingService.createBooking(testStudent, req);
        bookingService.updateStatus(booking.getId(), "REJECTED");

        ApiException ex = assertThrows(ApiException.class, () -> bookingService.updateStatus(booking.getId(), "APPROVED"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("Student can cancel their own pending booking")
    void testStudentCanCancelOwnBooking() {
        BookingRequest req = new BookingRequest();
        req.setRoomId(testRoom.getId());
        req.setCheckInDate(LocalDate.now().plusDays(1));

        BookingResponse booking = bookingService.createBooking(testStudent, req);
        BookingResponse cancelled = bookingService.cancelStudentBooking(booking.getId(), testStudent);

        assertEquals("CANCELLED", cancelled.getStatus());
        assertNotNull(cancelled.getCheckOutDate());
    }

    @Test
    @DisplayName("Concurrent booking attempts by same student allow at most one active booking")
    void testConcurrentBookingAttemptsPreventDuplicates() throws Exception {
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());
        List<BookingResponse> successes = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    BookingRequest req = new BookingRequest();
                    req.setRoomId(testRoom.getId());
                    req.setCheckInDate(LocalDate.now().plusDays(3));
                    BookingResponse resp = bookingService.createBooking(testStudent, req);
                    successes.add(resp);
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // Exactly 1 booking must succeed and the others must fail with 409 Conflict
        assertEquals(1, successes.size(), "Only one concurrent booking should succeed");
        assertEquals(threadCount - 1, errors.size(), "Other concurrent requests should be rejected");
        for (Exception err : errors) {
            assertTrue(err instanceof ApiException);
            assertEquals(HttpStatus.CONFLICT, ((ApiException) err).getStatus());
        }
    }
}
