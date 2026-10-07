package com.hostel.management.service;

import com.hostel.management.dto.NoticeRequest;
import com.hostel.management.dto.NoticeResponse;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.Notice;
import com.hostel.management.repository.NoticeRepository;
import com.hostel.management.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    @Transactional(readOnly = true)
    public List<NoticeResponse> getAllNotices() {
        return noticeRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(NoticeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public NoticeResponse createNotice(UserPrincipal principal, NoticeRequest req) {
        Notice.NoticePriority priority = Notice.NoticePriority.NORMAL;
        if (req.getPriority() != null && !req.getPriority().isBlank()) {
            try {
                priority = Notice.NoticePriority.valueOf(req.getPriority().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException("Invalid notice priority: " + req.getPriority(), HttpStatus.BAD_REQUEST);
            }
        }

        String postedBy = principal != null && principal.getUser() != null
                ? principal.getUser().getFullName() + " (" + principal.getUser().getRole().name() + ")"
                : "Hostel Administration";

        Notice notice = Notice.builder()
                .title(req.getTitle().trim())
                .content(req.getContent().trim())
                .category(req.getCategory() != null ? req.getCategory().trim() : null)
                .priority(priority)
                .targetAudience(req.getTargetAudience() != null ? req.getTargetAudience().trim() : null)
                .postedBy(postedBy)
                .build();

        Notice saved = noticeRepository.save(notice);
        return NoticeResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new ApiException("Notice not found", HttpStatus.NOT_FOUND));
        noticeRepository.delete(notice);
    }
}
