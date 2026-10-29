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
    private Long publisherId;
    private String publisherName;
    private String publisherRole;
    private LocalDateTime createdAt;

    public static NoticeResponse fromEntity(Notice notice) {
        if (notice == null) return null;

        Long publisherId = null;
        String publisherName = null;
        String publisherRole = null;

        if (notice.getPublisher() != null) {
            publisherId = notice.getPublisher().getId();
            publisherName = notice.getPublisher().getFullName();
            publisherRole = notice.getPublisher().getRole() != null ? notice.getPublisher().getRole().name() : null;
        } else if (notice.getPostedBy() != null && !notice.getPostedBy().isBlank()) {
            publisherName = notice.getPostedBy();
        } else {
            publisherName = "Publisher unavailable";
        }

        String postedBy = notice.getPostedBy();
        if (postedBy == null || postedBy.isBlank()) {
            if (publisherName != null && !publisherName.equals("Publisher unavailable")) {
                postedBy = publisherName + (publisherRole != null ? " (" + publisherRole + ")" : "");
            } else {
                postedBy = "Publisher unavailable";
            }
        }

        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .category(notice.getCategory())
                .priority(notice.getPriority())
                .targetAudience(notice.getTargetAudience())
                .postedBy(postedBy)
                .publisherId(publisherId)
                .publisherName(publisherName)
                .publisherRole(publisherRole)
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
