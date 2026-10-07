package com.hostel.management.dto;

import com.hostel.management.model.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {
    private Long id;
    private String roomNumber;
    private Room.RoomType roomType;
    private Integer capacity;
    private Integer occupied;
    private BigDecimal pricePerMonth;
    private Room.RoomStatus status;

    public static RoomResponse fromEntity(Room room) {
        if (room == null) return null;
        return RoomResponse.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .roomType(room.getRoomType())
                .capacity(room.getCapacity())
                .occupied(room.getOccupied())
                .pricePerMonth(room.getPricePerMonth())
                .status(room.getStatus())
                .build();
    }
}
