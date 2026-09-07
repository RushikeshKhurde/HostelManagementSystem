package com.hostel.management.service;

import com.hostel.management.dto.BookingRequest;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.Booking;
import com.hostel.management.model.Room;
import com.hostel.management.model.User;
import com.hostel.management.repository.BookingRepository;
import com.hostel.management.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    public Booking createBooking(User student, BookingRequest request) {
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ApiException("Room not found", HttpStatus.NOT_FOUND));

        if (room.getStatus() == Room.RoomStatus.MAINTENANCE) {
            throw new ApiException("Room is under maintenance", HttpStatus.BAD_REQUEST);
        }
        if (room.getOccupied() >= room.getCapacity()) {
            throw new ApiException("Room is fully occupied", HttpStatus.BAD_REQUEST);
        }

        Booking booking = Booking.builder()
                .student(student)
                .room(room)
                .checkInDate(request.getCheckInDate())
                .status(Booking.BookingStatus.PENDING)
                .build();

        return bookingRepository.save(booking);
    }

    public List<Booking> getBookingsForStudent(Long studentId) {
        return bookingRepository.findByStudentId(studentId);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Booking updateStatus(Long bookingId, String statusStr) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException("Booking not found", HttpStatus.NOT_FOUND));

        Booking.BookingStatus newStatus;
        try {
            newStatus = Booking.BookingStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException("Invalid status value", HttpStatus.BAD_REQUEST);
        }

        Room room = booking.getRoom();

        // Only increment occupancy the first time a booking is approved
        if (newStatus == Booking.BookingStatus.APPROVED && booking.getStatus() != Booking.BookingStatus.APPROVED) {
            if (room.getOccupied() >= room.getCapacity()) {
                throw new ApiException("Room is fully occupied", HttpStatus.BAD_REQUEST);
            }
            room.setOccupied(room.getOccupied() + 1);
            if (room.getOccupied() >= room.getCapacity()) {
                room.setStatus(Room.RoomStatus.FULL);
            }
            roomRepository.save(room);
        }

        // Free up the room slot if a previously approved booking is cancelled/completed
        boolean wasApproved = booking.getStatus() == Booking.BookingStatus.APPROVED;
        boolean freeingUp = (newStatus == Booking.BookingStatus.CANCELLED || newStatus == Booking.BookingStatus.COMPLETED);
        if (wasApproved && freeingUp) {
            room.setOccupied(Math.max(0, room.getOccupied() - 1));
            room.setStatus(Room.RoomStatus.AVAILABLE);
            roomRepository.save(room);
        }

        booking.setStatus(newStatus);
        return bookingRepository.save(booking);
    }
}
