package com.hostel.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomOccupantResponse {
    private Long studentId;
    private String studentCode; // e.g. STU001
    private String studentName;
    private String username;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private String address;
    private String course;
    private String yearSemester;
    private String department;
    private String rollNumber;
    private String hostelName;
    private Long roomId;
    private String roomNumber;
    private String roomType;
    private String bedNumber; // e.g. "Bed 1", "Bed 2"
    private Long bookingId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String bookingStatus;
    private LocalDateTime allocatedAt;
    private Double totalFee;
    private Double paidAmount;
    private Double pendingAmount;
    private String paymentStatus; // PAID, PARTIAL, PENDING
}
