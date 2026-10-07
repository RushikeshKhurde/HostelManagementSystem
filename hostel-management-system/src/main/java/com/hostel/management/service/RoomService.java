package com.hostel.management.service;

import com.hostel.management.dto.RoomOccupantResponse;
import com.hostel.management.dto.RoomRequest;
import com.hostel.management.dto.RoomResponse;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public List<RoomResponse> getAllRooms() {
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
        return rooms.stream()
                .map(RoomResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        Room room = findRoomEntityById(id);
        return RoomResponse.fromEntity(room);
    }

    public Room findRoomEntityById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ApiException("Room not found", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<RoomOccupantResponse> getRoomOccupants(Long roomId) {
        Room room = findRoomEntityById(roomId);
        List<Booking> activeBookings = bookingRepository.findByRoomIdAndStatusOrderByCreatedAtAsc(
                roomId, Booking.BookingStatus.APPROVED
        );

        List<RoomOccupantResponse> occupants = new ArrayList<>();

        for (int i = 0; i < activeBookings.size(); i++) {
            Booking booking = activeBookings.get(i);
            User student = booking.getStudent();

            double totalFee = room.getPricePerMonth() != null ? room.getPricePerMonth().doubleValue() : 0.0;
            List<Payment> payments = paymentRepository.findByBookingStudentId(student.getId());
            double paidAmount = payments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.SUCCESS)
                    .map(p -> p.getAmount() != null ? p.getAmount().doubleValue() : 0.0)
                    .mapToDouble(Double::doubleValue)
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

    @Transactional
    public RoomResponse addRoom(RoomRequest req) {
        String roomNumber = req.getRoomNumber().trim().toUpperCase();
        if (roomRepository.existsByRoomNumber(roomNumber)) {
            throw new ApiException("Room number '" + roomNumber + "' already exists", HttpStatus.CONFLICT);
        }

        int initialOccupied = 0;
        Room.RoomStatus initialStatus;

        if (req.getStatus() != null) {
            if (req.getStatus() == Room.RoomStatus.MAINTENANCE) {
                initialStatus = Room.RoomStatus.MAINTENANCE;
            } else if (req.getStatus() == Room.RoomStatus.FULL) {
                throw new ApiException("Cannot set room status to FULL when occupied beds (" + initialOccupied + ") is less than capacity (" + req.getCapacity() + ")", HttpStatus.BAD_REQUEST);
            } else {
                initialStatus = Room.RoomStatus.AVAILABLE;
            }
        } else {
            initialStatus = Room.RoomStatus.AVAILABLE;
        }

        Room room = Room.builder()
                .roomNumber(roomNumber)
                .roomType(req.getRoomType())
                .capacity(req.getCapacity())
                .occupied(initialOccupied)
                .pricePerMonth(req.getPricePerMonth())
                .status(initialStatus)
                .build();

        Room saved = roomRepository.save(room);
        return RoomResponse.fromEntity(saved);
    }

    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest req) {
        Room room = findRoomEntityById(id);

        String updatedRoomNumber = req.getRoomNumber().trim().toUpperCase();
        if (!room.getRoomNumber().equalsIgnoreCase(updatedRoomNumber) && roomRepository.existsByRoomNumber(updatedRoomNumber)) {
            throw new ApiException("Room number '" + updatedRoomNumber + "' already exists", HttpStatus.CONFLICT);
        }

        int currentOccupied = room.getOccupied() != null ? room.getOccupied() : 0;
        if (req.getCapacity() < currentOccupied) {
            throw new ApiException("Capacity (" + req.getCapacity() + " beds) cannot be less than current occupied beds (" + currentOccupied + ")", HttpStatus.BAD_REQUEST);
        }

        room.setRoomNumber(updatedRoomNumber);
        room.setRoomType(req.getRoomType());
        room.setCapacity(req.getCapacity());
        room.setPricePerMonth(req.getPricePerMonth());

        if (req.getStatus() != null) {
            if (req.getStatus() == Room.RoomStatus.MAINTENANCE) {
                room.setStatus(Room.RoomStatus.MAINTENANCE);
            } else if (req.getStatus() == Room.RoomStatus.FULL) {
                if (currentOccupied < req.getCapacity()) {
                    throw new ApiException("Cannot set room status to FULL when occupied beds (" + currentOccupied + ") is less than capacity (" + req.getCapacity() + ")", HttpStatus.BAD_REQUEST);
                }
                room.setStatus(Room.RoomStatus.FULL);
            } else if (req.getStatus() == Room.RoomStatus.AVAILABLE) {
                if (currentOccupied >= req.getCapacity()) {
                    throw new ApiException("Cannot set room status to AVAILABLE when room is at full capacity (" + currentOccupied + "/" + req.getCapacity() + ")", HttpStatus.BAD_REQUEST);
                }
                room.setStatus(Room.RoomStatus.AVAILABLE);
            }
        } else if (room.getStatus() != Room.RoomStatus.MAINTENANCE) {
            // Recalculate status for non-maintenance rooms
            if (currentOccupied >= req.getCapacity()) {
                room.setStatus(Room.RoomStatus.FULL);
            } else {
                room.setStatus(Room.RoomStatus.AVAILABLE);
            }
        }

        Room saved = roomRepository.save(room);
        return RoomResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteRoom(Long id) {
        Room room = findRoomEntityById(id);

        long bookingCount = bookingRepository.countByRoomId(id);
        if (bookingCount > 0) {
            throw new ApiException(
                    "Cannot delete Room " + room.getRoomNumber() + " because " + bookingCount +
                    " booking record(s) reference this room. Set the room status to MAINTENANCE instead.",
                    HttpStatus.CONFLICT
            );
        }

        roomRepository.delete(room);
    }
}
