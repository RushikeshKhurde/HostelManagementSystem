package com.hostel.management.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintRequest {

    @Size(max = 2000, message = "Feedback cannot exceed 2000 characters")
    private String feedback;

    @Size(max = 100, message = "Title cannot exceed 100 characters")
    private String title;

    @Size(max = 50, message = "Category cannot exceed 50 characters")
    private String category;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    @Size(max = 20, message = "Room number cannot exceed 20 characters")
    private String roomNumber;

    private String priority;

    private String visibility;
}
