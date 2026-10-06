package com.hostel.management.dto;

import com.hostel.management.model.Booking;
import com.hostel.management.model.Room;
import com.hostel.management.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private StudentSummary student;
    private RoomSummary room;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String status;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentSummary {
        private Long id;
        private String fullName;
        private String username;
        private String email;
        private String mobileNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomSummary {
        private Long id;
        private String roomNumber;
        private String roomType;
        private Integer capacity;
        private Integer occupied;
        private BigDecimal pricePerMonth;
        private String status;
    }

    public static BookingResponse fromEntity(Booking booking) {
        if (booking == null) return null;

        StudentSummary studentSummary = null;
        if (booking.getStudent() != null) {
            User s = booking.getStudent();
            studentSummary = StudentSummary.builder()
                    .id(s.getId())
                    .fullName(s.getFullName())
                    .username(s.getUsername())
                    .email(s.getEmail())
                    .mobileNumber(s.getMobileNumber())
                    .build();
        }

        RoomSummary roomSummary = null;
        if (booking.getRoom() != null) {
            Room r = booking.getRoom();
            roomSummary = RoomSummary.builder()
                    .id(r.getId())
                    .roomNumber(r.getRoomNumber())
                    .roomType(r.getRoomType() != null ? r.getRoomType().name() : null)
                    .capacity(r.getCapacity())
                    .occupied(r.getOccupied())
                    .pricePerMonth(r.getPricePerMonth())
                    .status(r.getStatus() != null ? r.getStatus().name() : null)
                    .build();
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .student(studentSummary)
                .room(roomSummary)
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .status(booking.getStatus() != null ? booking.getStatus().name() : null)
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
