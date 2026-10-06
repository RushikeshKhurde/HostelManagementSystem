package com.hostel.management.service;

import com.hostel.management.dto.RoomOccupantResponse;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.Booking;
import com.hostel.management.model.Payment;
import com.hostel.management.model.Room;
import com.hostel.management.model.User;
import com.hostel.management.repository.BookingRepository;
import com.hostel.management.repository.PaymentRepository;
import com.hostel.management.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public List<Room> getAllRooms() {
        List<Room> rooms = roomRepository.findAll();
        for (Room room : rooms) {
            int approvedCount = (int) bookingRepository.countByRoomIdAndStatus(room.getId(), Booking.BookingStatus.APPROVED);
            boolean changed = false;
            if (room.getOccupied() == null || room.getOccupied() != approvedCount) {
                room.setOccupied(approvedCount);
                changed = true;
            }
            if (room.getStatus() != Room.RoomStatus.MAINTENANCE) {
                Room.RoomStatus expectedStatus = (room.getOccupied() >= room.getCapacity())
                        ? Room.RoomStatus.FULL
                        : Room.RoomStatus.AVAILABLE;
                if (room.getStatus() != expectedStatus) {
                    room.setStatus(expectedStatus);
                    changed = true;
                }
            }
            if (changed) {
                roomRepository.save(room);
            }
        }
        return rooms;
    }

    public Room getRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ApiException("Room not found", HttpStatus.NOT_FOUND));
    }

    public List<RoomOccupantResponse> getRoomOccupants(Long roomId) {
        Room room = getRoomById(roomId);
        List<Booking> activeBookings = bookingRepository.findByRoomIdAndStatusOrderByCreatedAtAsc(
                roomId, Booking.BookingStatus.APPROVED
        );

        List<RoomOccupantResponse> occupants = new ArrayList<>();

        for (int i = 0; i < activeBookings.size(); i++) {
            Booking booking = activeBookings.get(i);
            User student = booking.getStudent();

            double totalFee = room.getPricePerMonth() != null ? room.getPricePerMonth() : 0.0;
            List<Payment> payments = paymentRepository.findByBookingStudentId(student.getId());
            double paidAmount = payments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.SUCCESS)
                    .mapToDouble(Payment::getAmount)
                    .sum();
            double pendingAmount = Math.max(0.0, totalFee - paidAmount);
            String paymentStatus = (pendingAmount <= 0) ? "PAID" : (paidAmount > 0 ? "PARTIAL" : "PENDING");

            String block = room.getRoomNumber().contains("-") ? room.getRoomNumber().split("-")[0] : "A";
            String hostelName = "SmartHostel (Block " + block + ")";

            occupants.add(RoomOccupantResponse.builder()
                    .studentId(student.getId())
                    .studentCode(String.format("STU%03d", student.getId()))
                    .studentName(student.getFullName())
                    .username(student.getUsername())
                    .email(student.getEmail())
                    .phone(student.getMobileNumber())
                    .gender(student.getGender() != null ? student.getGender() : "Not specified")
                    .dateOfBirth(student.getDateOfBirth())
                    .address(student.getAddress() != null ? student.getAddress() : "Not specified")
                    .course("Not specified")
                    .yearSemester("Not specified")
                    .department("Not specified")
                    .rollNumber(String.format("STU%03d", student.getId()))
                    .hostelName(hostelName)
                    .roomId(room.getId())
                    .roomNumber(room.getRoomNumber())
                    .roomType(room.getRoomType().name())
                    .bedNumber("Bed " + (i + 1))
                    .bookingId(booking.getId())
                    .checkInDate(booking.getCheckInDate())
                    .checkOutDate(booking.getCheckOutDate())
                    .bookingStatus(booking.getStatus().name())
                    .allocatedAt(booking.getCreatedAt())
                    .totalFee(totalFee)
                    .paidAmount(paidAmount)
                    .pendingAmount(pendingAmount)
                    .paymentStatus(paymentStatus)
                    .build());
        }

        return occupants;
    }

    public Room addRoom(Room room) {
        if (roomRepository.existsByRoomNumber(room.getRoomNumber())) {
            throw new ApiException("Room number already exists", HttpStatus.CONFLICT);
        }
        room.setOccupied(0);
        room.setStatus(Room.RoomStatus.AVAILABLE);
        return roomRepository.save(room);
    }

    public Room updateRoom(Long id, Room updated) {
        Room room = getRoomById(id);
        room.setRoomType(updated.getRoomType());
        room.setCapacity(updated.getCapacity());
        room.setPricePerMonth(updated.getPricePerMonth());
        if (updated.getStatus() != null) {
            room.setStatus(updated.getStatus());
        }
        return roomRepository.save(room);
    }

    @Transactional
    public void deleteRoom(Long id) {
        Room room = getRoomById(id);
        long activeOccupants = bookingRepository.countByRoomIdAndStatus(id, Booking.BookingStatus.APPROVED);
        if (activeOccupants > 0) {
            throw new ApiException(
                    "Cannot delete Room " + room.getRoomNumber() + " because " + activeOccupants +
                            (activeOccupants == 1 ? " student is" : " students are") + " currently assigned to this room.",
                    HttpStatus.CONFLICT
            );
        }

        // Clean up inactive or cancelled bookings so FK constraint doesn't prevent deletion
        List<Booking> inactiveBookings = bookingRepository.findByRoomId(id);
        if (inactiveBookings != null && !inactiveBookings.isEmpty()) {
            bookingRepository.deleteAll(inactiveBookings);
        }

        roomRepository.delete(room);
    }
}
