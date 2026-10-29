package com.hostel.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintCreateDto {
    private String feedback;
    private String description;
    private String title;
    private String category;
    private String roomNumber;
    private String priority;
    private String visibility;
    private MultipartFile image;
}
