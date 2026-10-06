package com.hostel.management.service;

import com.hostel.management.dto.RoomRequest;
import com.hostel.management.dto.RoomResponse;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.Room;
import com.hostel.management.repository.BookingRepository;
import com.hostel.management.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll().stream()
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
