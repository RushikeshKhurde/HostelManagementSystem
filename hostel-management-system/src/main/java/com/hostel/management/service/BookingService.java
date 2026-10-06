package com.hostel.management.service;

import com.hostel.management.dto.BookingRequest;
import com.hostel.management.dto.BookingResponse;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.Booking;
import com.hostel.management.model.Payment;
import com.hostel.management.model.Room;
import com.hostel.management.model.User;
import com.hostel.management.repository.BookingRepository;
import com.hostel.management.repository.PaymentRepository;
import com.hostel.management.repository.RoomRepository;
import com.hostel.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @Transactional
    public BookingResponse createBooking(User student, BookingRequest request) {
        if (request.getCheckInDate() == null || request.getCheckInDate().isBefore(LocalDate.now())) {
            throw new ApiException("Check-in date cannot be in the past", HttpStatus.BAD_REQUEST);
        }

        // Acquire pessimistic write lock on the student record to serialize concurrent booking attempts by the same student
        User lockedStudent = userRepository.findByIdWithLock(student.getId())
                .orElseThrow(() -> new ApiException("Student not found", HttpStatus.NOT_FOUND));

        List<Booking.BookingStatus> activeStatuses = List.of(
                Booking.BookingStatus.PENDING,
                Booking.BookingStatus.APPROVED
        );
        boolean hasActiveBooking = bookingRepository.existsByStudentIdAndStatusIn(lockedStudent.getId(), activeStatuses);
        if (hasActiveBooking) {
            throw new ApiException("You already have an active room booking. You cannot book another room.", HttpStatus.CONFLICT);
        }

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ApiException("Room not found", HttpStatus.NOT_FOUND));

        if (room.getStatus() == Room.RoomStatus.MAINTENANCE) {
            throw new ApiException("Room is under maintenance", HttpStatus.BAD_REQUEST);
        }
        if (room.getOccupied() >= room.getCapacity()) {
            throw new ApiException("Room is fully occupied", HttpStatus.BAD_REQUEST);
        }

        Booking booking = Booking.builder()
                .student(lockedStudent)
                .room(room)
                .checkInDate(request.getCheckInDate())
                .status(Booking.BookingStatus.PENDING)
                .build();

        Booking saved = bookingRepository.save(booking);
        return BookingResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsForStudent(Long studentId) {
        return bookingRepository.findByStudentId(studentId).stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponse updateStatus(Long bookingId, String statusStr) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException("Booking not found", HttpStatus.NOT_FOUND));

        Booking.BookingStatus newStatus;
        try {
            newStatus = Booking.BookingStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ApiException("Invalid status value", HttpStatus.BAD_REQUEST);
        }

        Booking.BookingStatus oldStatus = booking.getStatus();

        if (oldStatus == newStatus) {
            return BookingResponse.fromEntity(booking);
        }

        // Validate allowed state transitions
        validateStatusTransition(oldStatus, newStatus);

        Long roomId = booking.getRoom().getId();

        // Concurrency-safe handling: lock room record during approval or release
        if (newStatus == Booking.BookingStatus.APPROVED && oldStatus == Booking.BookingStatus.PENDING) {
            Room room = roomRepository.findByIdWithLock(roomId)
                    .orElseThrow(() -> new ApiException("Room not found", HttpStatus.NOT_FOUND));

            if (room.getStatus() == Room.RoomStatus.MAINTENANCE) {
                throw new ApiException("Cannot approve booking: Room is under maintenance", HttpStatus.BAD_REQUEST);
            }

            int currentOccupied = room.getOccupied() != null ? room.getOccupied() : 0;
            if (currentOccupied >= room.getCapacity()) {
                throw new ApiException("Cannot approve booking: Room is fully occupied", HttpStatus.BAD_REQUEST);
            }

            int newOccupied = currentOccupied + 1;
            room.setOccupied(newOccupied);
            if (newOccupied >= room.getCapacity()) {
                room.setStatus(Room.RoomStatus.FULL);
            }
            roomRepository.save(room);
            booking.setRoom(room);
        } else if (oldStatus == Booking.BookingStatus.APPROVED &&
                (newStatus == Booking.BookingStatus.CANCELLED || newStatus == Booking.BookingStatus.COMPLETED)) {

            Room room = roomRepository.findByIdWithLock(roomId)
                    .orElseThrow(() -> new ApiException("Room not found", HttpStatus.NOT_FOUND));

            int currentOccupied = room.getOccupied() != null ? room.getOccupied() : 0;
            int newOccupied = Math.max(0, currentOccupied - 1);
            room.setOccupied(newOccupied);

            // Preserve MAINTENANCE status; only reset to AVAILABLE if not in maintenance
            if (room.getStatus() != Room.RoomStatus.MAINTENANCE && newOccupied < room.getCapacity()) {
                room.setStatus(Room.RoomStatus.AVAILABLE);
            }
            roomRepository.save(room);
            booking.setRoom(room);
            booking.setCheckOutDate(LocalDate.now());
        }

        booking.setStatus(newStatus);
        Booking saved = bookingRepository.save(booking);

        if (newStatus == Booking.BookingStatus.CANCELLED || newStatus == Booking.BookingStatus.REJECTED || newStatus == Booking.BookingStatus.COMPLETED) {
            List<Payment> pendingPayments = paymentRepository.findByBookingIdAndStatus(booking.getId(), Payment.PaymentStatus.PENDING);
            for (Payment p : pendingPayments) {
                p.setStatus(Payment.PaymentStatus.FAILED);
                paymentRepository.save(p);
            }
        }

        return BookingResponse.fromEntity(saved);
    }

    @Transactional
    public BookingResponse cancelStudentBooking(Long bookingId, User student) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException("Booking not found", HttpStatus.NOT_FOUND));

        if (!booking.getStudent().getId().equals(student.getId())) {
            throw new ApiException("You are not authorized to cancel this booking", HttpStatus.FORBIDDEN);
        }

        Booking.BookingStatus currentStatus = booking.getStatus();
        if (currentStatus == Booking.BookingStatus.CANCELLED) {
            throw new ApiException("Booking is already cancelled", HttpStatus.BAD_REQUEST);
        }
        if (currentStatus == Booking.BookingStatus.REJECTED || currentStatus == Booking.BookingStatus.COMPLETED) {
            throw new ApiException("Cannot cancel a booking that is already " + currentStatus, HttpStatus.BAD_REQUEST);
        }

        // If it was APPROVED, decrement room occupancy safely
        if (currentStatus == Booking.BookingStatus.APPROVED) {
            Long roomId = booking.getRoom().getId();
            Room room = roomRepository.findByIdWithLock(roomId)
                    .orElseThrow(() -> new ApiException("Room not found", HttpStatus.NOT_FOUND));

            int currentOccupied = room.getOccupied() != null ? room.getOccupied() : 0;
            int newOccupied = Math.max(0, currentOccupied - 1);
            room.setOccupied(newOccupied);
            if (room.getStatus() != Room.RoomStatus.MAINTENANCE && newOccupied < room.getCapacity()) {
                room.setStatus(Room.RoomStatus.AVAILABLE);
            }
            roomRepository.save(room);
            booking.setRoom(room);
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setCheckOutDate(LocalDate.now());
        Booking saved = bookingRepository.save(booking);

        List<Payment> pendingPayments = paymentRepository.findByBookingIdAndStatus(booking.getId(), Payment.PaymentStatus.PENDING);
        for (Payment p : pendingPayments) {
            p.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(p);
        }

        return BookingResponse.fromEntity(saved);
    }

    private void validateStatusTransition(Booking.BookingStatus from, Booking.BookingStatus to) {
        // Defined valid transitions:
        // PENDING -> APPROVED, REJECTED, CANCELLED
        // APPROVED -> CANCELLED, COMPLETED
        boolean valid = switch (from) {
            case PENDING -> to == Booking.BookingStatus.APPROVED || to == Booking.BookingStatus.REJECTED || to == Booking.BookingStatus.CANCELLED;
            case APPROVED -> to == Booking.BookingStatus.CANCELLED || to == Booking.BookingStatus.COMPLETED;
            case REJECTED, CANCELLED, COMPLETED -> false; // Terminal states
        };

        if (!valid) {
            throw new ApiException("Invalid status transition from " + from + " to " + to, HttpStatus.BAD_REQUEST);
        }
    }
}
