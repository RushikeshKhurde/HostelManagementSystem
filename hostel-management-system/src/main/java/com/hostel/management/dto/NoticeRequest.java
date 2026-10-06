package com.hostel.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(min = 3, max = 2000, message = "Content must be between 3 and 2000 characters")
    private String content;

    @Size(max = 50, message = "Category cannot exceed 50 characters")
    private String category;

    private String priority;

    @Size(max = 50, message = "Target audience cannot exceed 50 characters")
    private String targetAudience;
}
