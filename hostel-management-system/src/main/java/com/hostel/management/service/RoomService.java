package com.hostel.management.service;

import com.hostel.management.exception.ApiException;
import com.hostel.management.model.Room;
import com.hostel.management.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Room getRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ApiException("Room not found", HttpStatus.NOT_FOUND));
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

    public void deleteRoom(Long id) {
        Room room = getRoomById(id);
        roomRepository.delete(room);
    }
}
