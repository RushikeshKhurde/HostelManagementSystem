package com.hostel.management;

import com.hostel.management.dto.BookingRequest;
import com.hostel.management.dto.RoomRequest;
import com.hostel.management.dto.RoomResponse;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.Role;
import com.hostel.management.model.Room;
import com.hostel.management.model.User;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class RoomManagementTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    private User student;

    @BeforeEach
    void setUp() {
        String ts = String.valueOf(System.currentTimeMillis());
        student = userRepository.save(User.builder()
                .fullName("Room Test Student")
                .username("rstudent_" + ts)
                .email("rstudent_" + ts + "@test.com")
                .mobileNumber("9" + ts.substring(ts.length() - 9))
                .password("Password@123")
                .role(Role.USER)
                .build());
    }

    @Test
    @DisplayName("Room cannot be deleted if active or past bookings exist (409 Conflict)")
    void testRoomWithBookingsCannotBeDeleted() {
        String ts = String.valueOf(System.currentTimeMillis());
        RoomResponse room = roomService.addRoom(RoomRequest.builder()
                .roomNumber("DEL-" + ts.substring(ts.length() - 5))
                .roomType(Room.RoomType.SINGLE)
                .capacity(1)
                .pricePerMonth(BigDecimal.valueOf(4500.00))
                .status(Room.RoomStatus.AVAILABLE)
                .build());

        BookingRequest bookingReq = new BookingRequest();
        bookingReq.setRoomId(room.getId());
        bookingReq.setCheckInDate(LocalDate.now().plusDays(1));
        bookingService.createBooking(student, bookingReq);

        ApiException ex = assertThrows(ApiException.class, () -> roomService.deleteRoom(room.getId()));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("booking record(s) reference this room"));
    }

    @Test
    @DisplayName("Room capacity cannot be reduced below current occupancy (400 Bad Request)")
    void testCannotReduceCapacityBelowOccupancy() {
        String ts = String.valueOf(System.currentTimeMillis());
        RoomResponse room = roomService.addRoom(RoomRequest.builder()
                .roomNumber("CAP-" + ts.substring(ts.length() - 5))
                .roomType(Room.RoomType.DOUBLE)
                .capacity(2)
                .pricePerMonth(BigDecimal.valueOf(5000.00))
                .status(Room.RoomStatus.AVAILABLE)
                .build());

        BookingRequest bookingReq = new BookingRequest();
        bookingReq.setRoomId(room.getId());
        bookingReq.setCheckInDate(LocalDate.now().plusDays(1));
        var booking = bookingService.createBooking(student, bookingReq);
        bookingService.updateStatus(booking.getId(), "APPROVED");

        // Attempting to update capacity to 0 (below occupied=1)
        RoomRequest updateReq = RoomRequest.builder()
                .roomNumber(room.getRoomNumber())
                .roomType(Room.RoomType.SINGLE)
                .capacity(0)
                .pricePerMonth(BigDecimal.valueOf(5000.00))
                .status(Room.RoomStatus.AVAILABLE)
                .build();

        ApiException ex = assertThrows(ApiException.class, () -> roomService.updateRoom(room.getId(), updateReq));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("cannot be less than current occupied beds"));
    }

    @Test
    @DisplayName("Creating or updating room with FULL status when occupied < capacity is rejected (400 Bad Request)")
    void testInvalidFullStatusRejected() {
        String ts = String.valueOf(System.currentTimeMillis());
        // Creation with FULL when occupied is 0
        RoomRequest addReq = RoomRequest.builder()
                .roomNumber("FUL-" + ts.substring(ts.length() - 5))
                .roomType(Room.RoomType.SINGLE)
                .capacity(1)
                .pricePerMonth(BigDecimal.valueOf(5000.00))
                .status(Room.RoomStatus.FULL)
                .build();

        ApiException ex1 = assertThrows(ApiException.class, () -> roomService.addRoom(addReq));
        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatus());
        assertTrue(ex1.getMessage().contains("Cannot set room status to FULL"));

        // Update with FULL when empty
        RoomResponse room = roomService.addRoom(RoomRequest.builder()
                .roomNumber("FULU-" + ts.substring(ts.length() - 5))
                .roomType(Room.RoomType.SINGLE)
                .capacity(1)
                .pricePerMonth(BigDecimal.valueOf(5000.00))
                .status(Room.RoomStatus.AVAILABLE)
                .build());

        RoomRequest updateReq = RoomRequest.builder()
                .roomNumber(room.getRoomNumber())
                .roomType(Room.RoomType.SINGLE)
                .capacity(1)
                .pricePerMonth(BigDecimal.valueOf(5000.00))
                .status(Room.RoomStatus.FULL)
                .build();

        ApiException ex2 = assertThrows(ApiException.class, () -> roomService.updateRoom(room.getId(), updateReq));
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatus());
        assertTrue(ex2.getMessage().contains("Cannot set room status to FULL"));
    }

    @Test
    @DisplayName("Updating room to AVAILABLE when occupied >= capacity is rejected (400 Bad Request)")
    void testInvalidAvailableStatusRejected() {
        String ts = String.valueOf(System.currentTimeMillis());
        RoomResponse room = roomService.addRoom(RoomRequest.builder()
                .roomNumber("AV-" + ts.substring(ts.length() - 5))
                .roomType(Room.RoomType.SINGLE)
                .capacity(1)
                .pricePerMonth(BigDecimal.valueOf(5000.00))
                .status(Room.RoomStatus.AVAILABLE)
                .build());

        BookingRequest bookingReq = new BookingRequest();
        bookingReq.setRoomId(room.getId());
        bookingReq.setCheckInDate(LocalDate.now().plusDays(1));
        var booking = bookingService.createBooking(student, bookingReq);
        bookingService.updateStatus(booking.getId(), "APPROVED");

        // Room now has occupied=1, capacity=1 (FULL). Explicitly updating to AVAILABLE must be rejected.
        RoomRequest updateReq = RoomRequest.builder()
                .roomNumber(room.getRoomNumber())
                .roomType(Room.RoomType.SINGLE)
                .capacity(1)
                .pricePerMonth(BigDecimal.valueOf(5000.00))
                .status(Room.RoomStatus.AVAILABLE)
                .build();

        ApiException ex = assertThrows(ApiException.class, () -> roomService.updateRoom(room.getId(), updateReq));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Cannot set room status to AVAILABLE when room is at full capacity"));
    }

    @Test
    @DisplayName("MAINTENANCE status is explicitly allowed and preserved")
    void testMaintenanceStatusAllowed() {
        String ts = String.valueOf(System.currentTimeMillis());
        RoomResponse room = roomService.addRoom(RoomRequest.builder()
                .roomNumber("MNT-" + ts.substring(ts.length() - 5))
                .roomType(Room.RoomType.SINGLE)
                .capacity(1)
                .pricePerMonth(BigDecimal.valueOf(5000.00))
                .status(Room.RoomStatus.MAINTENANCE)
                .build());

        assertEquals(Room.RoomStatus.MAINTENANCE, room.getStatus());

        // Update without changing status maintains MAINTENANCE
        RoomRequest updateReq = RoomRequest.builder()
                .roomNumber(room.getRoomNumber())
                .roomType(Room.RoomType.SINGLE)
                .capacity(2)
                .pricePerMonth(BigDecimal.valueOf(6000.00))
                .status(null)
                .build();

        RoomResponse updated = roomService.updateRoom(room.getId(), updateReq);
        assertEquals(Room.RoomStatus.MAINTENANCE, updated.getStatus());
    }
}
