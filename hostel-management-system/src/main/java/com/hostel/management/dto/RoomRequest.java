package com.hostel.management.dto;

import com.hostel.management.model.Room;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomRequest {

    @NotBlank(message = "Room number is required")
    @Size(min = 1, max = 20, message = "Room number must be between 1 and 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\-_]+$", message = "Room number can only contain letters, numbers, hyphens, and underscores")
    private String roomNumber;

    @NotNull(message = "Room type is required")
    private Room.RoomType roomType;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 20, message = "Capacity cannot exceed 20 beds")
    private Integer capacity;

    @NotNull(message = "Monthly rent price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price per month cannot be negative")
    private BigDecimal pricePerMonth;

    private Room.RoomStatus status;
}
