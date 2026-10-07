package com.hostel.management.dto;

import com.hostel.management.model.Notice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeResponse {
    private Long id;
    private String title;
    private String content;
    private String category;
    private Notice.NoticePriority priority;
    private String targetAudience;
    private String postedBy;
    private LocalDateTime createdAt;

    public static NoticeResponse fromEntity(Notice notice) {
        if (notice == null) return null;
        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .category(notice.getCategory())
                .priority(notice.getPriority())
                .targetAudience(notice.getTargetAudience())
                .postedBy(notice.getPostedBy())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
