package com.hostel.management.service;

import com.hostel.management.dto.NoticeRequest;
import com.hostel.management.dto.NoticeResponse;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.Notice;
import com.hostel.management.model.Role;
import com.hostel.management.model.User;
import com.hostel.management.repository.NoticeRepository;
import com.hostel.management.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private static final Logger log = LoggerFactory.getLogger(NoticeService.class);

    public static final String DUPLICATE_NOTICE_MESSAGE =
            "This announcement has already been published with the same content. Please update the announcement before publishing.";

    private final NoticeRepository noticeRepository;

    @Transactional(readOnly = true)
    public List<NoticeResponse> getAllNotices() {
        return noticeRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(NoticeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public NoticeResponse createNotice(UserPrincipal principal, NoticeRequest req) {
        if (principal == null || principal.getUser() == null) {
            throw new ApiException("Authentication required to publish notices", HttpStatus.UNAUTHORIZED);
        }

        User publisher = principal.getUser();
        Role role = publisher.getRole();
        if (role != Role.ADMIN && role != Role.WARDEN) {
            throw new ApiException("Only Administrators and Wardens are authorized to publish announcements", HttpStatus.FORBIDDEN);
        }

        if (req.getTitle() == null || req.getTitle().trim().isBlank()) {
            throw new ApiException("Notice title is required", HttpStatus.BAD_REQUEST);
        }
        if (req.getContent() == null || req.getContent().trim().isBlank()) {
            throw new ApiException("Notice content is required", HttpStatus.BAD_REQUEST);
        }

        Notice.NoticePriority priority = Notice.NoticePriority.NORMAL;
        if (req.getPriority() != null && !req.getPriority().isBlank()) {
            try {
                priority = Notice.NoticePriority.valueOf(req.getPriority().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new ApiException("Invalid notice priority: " + req.getPriority(), HttpStatus.BAD_REQUEST);
            }
        }

        String targetAudience = req.getTargetAudience() != null && !req.getTargetAudience().trim().isBlank()
                ? req.getTargetAudience().trim()
                : "ALL";

        // 1. Compute canonical content fingerprint
        String fingerprint = computeCanonicalFingerprint(req.getTitle(), req.getContent(), targetAudience);

        // 2. Pre-check for duplicate announcement by fingerprint across all active notices (Admin & Warden)
        if (noticeRepository.existsByContentFingerprint(fingerprint)) {
            log.warn("Duplicate announcement detected by fingerprint {} for user {}", fingerprint, publisher.getUsername());
            throw new ApiException(DUPLICATE_NOTICE_MESSAGE, HttpStatus.CONFLICT);
        }

        // 3. Defensive check against legacy un-fingerprinted records
        if (hasMatchingLegacyNotice(req.getTitle(), req.getContent(), targetAudience)) {
            log.warn("Duplicate announcement detected by legacy match for user {}", publisher.getUsername());
            throw new ApiException(DUPLICATE_NOTICE_MESSAGE, HttpStatus.CONFLICT);
        }

        // 4. Associate real authenticated publisher
        String postedBy = publisher.getFullName() + (publisher.getRole() != null ? " (" + publisher.getRole().name() + ")" : "");

        Notice notice = Notice.builder()
                .title(req.getTitle().trim())
                .content(req.getContent().trim())
                .category(req.getCategory() != null ? req.getCategory().trim() : null)
                .priority(priority)
                .targetAudience(targetAudience)
                .publisher(publisher)
                .postedBy(postedBy)
                .contentFingerprint(fingerprint)
                .build();

        try {
            Notice saved = noticeRepository.saveAndFlush(notice);
            log.info("Announcement '{}' published successfully by {} ({}) with id={}",
                    saved.getTitle(), publisher.getUsername(), role, saved.getId());
            return NoticeResponse.fromEntity(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent duplicate announcement insertion conflict for fingerprint {}", fingerprint);
            throw new ApiException(DUPLICATE_NOTICE_MESSAGE, HttpStatus.CONFLICT);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> checkDuplicate(NoticeRequest req) {
        if (req.getTitle() == null || req.getContent() == null) {
            return Map.of("duplicate", false);
        }
        String audience = req.getTargetAudience() != null && !req.getTargetAudience().trim().isBlank()
                ? req.getTargetAudience().trim()
                : "ALL";
        String fingerprint = computeCanonicalFingerprint(req.getTitle(), req.getContent(), audience);

        Optional<Notice> existing = noticeRepository.findByContentFingerprint(fingerprint);
        if (existing.isPresent()) {
            Notice n = existing.get();
            return Map.of(
                    "duplicate", true,
                    "message", DUPLICATE_NOTICE_MESSAGE,
                    "existingTitle", n.getTitle(),
                    "publishedAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : "Recent"
            );
        }

        if (hasMatchingLegacyNotice(req.getTitle(), req.getContent(), audience)) {
            return Map.of(
                    "duplicate", true,
                    "message", DUPLICATE_NOTICE_MESSAGE
            );
        }

        return Map.of("duplicate", false);
    }

    @Transactional
    public void deleteNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new ApiException("Notice not found", HttpStatus.NOT_FOUND));
        noticeRepository.delete(notice);
    }

    /**
     * Defensive check for historical records that may not have had a fingerprint backfilled.
     */
    private boolean hasMatchingLegacyNotice(String title, String content, String audience) {
        String normTitle = normalizeText(title);
        String normContent = normalizeText(content);
        String normAudience = normalizeAudience(audience);

        List<Notice> allNotices = noticeRepository.findAll();
        for (Notice existing : allNotices) {
            if (existing.getContentFingerprint() != null) {
                // Already checked via existsByContentFingerprint
                continue;
            }
            String existingTitle = normalizeText(existing.getTitle());
            String existingContent = normalizeText(existing.getContent());
            String existingAudience = normalizeAudience(existing.getTargetAudience());

            if (normTitle.equals(existingTitle) &&
                normContent.equals(existingContent) &&
                normAudience.equals(existingAudience)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Normalizes text for canonical comparison:
     * 1. Trim leading and trailing whitespace.
     * 2. Normalize Windows/Unix line endings (\r\n and \r to \n).
     * 3. Collapse multiple whitespace characters (\s+) to a single space.
     * 4. Lowercase case-insensitively using Locale.ROOT.
     */
    public static String normalizeText(String input) {
        if (input == null) {
            return "";
        }
        String normalizedEndings = input.replace("\r\n", "\n").replace('\r', '\n');
        String trimmed = normalizedEndings.trim().toLowerCase(Locale.ROOT);
        return trimmed.replaceAll("\\s+", " ");
    }

    /**
     * Normalizes audience:
     * Defaults null, empty, or "ALL" to "ALL".
     * Case-insensitive uppercase trimmed value (e.g., "BLOCK_A").
     */
    public static String normalizeAudience(String audience) {
        if (audience == null || audience.trim().isBlank()) {
            return "ALL";
        }
        String trimmed = audience.trim().toUpperCase(Locale.ROOT);
        return trimmed.replaceAll("\\s+", " ");
    }

    /**
     * Computes a canonical SHA-256 fingerprint for duplicate detection:
     * SHA-256(normalizeText(title) + "\n---\n" + normalizeText(content) + "\n---\n" + normalizeAudience(audience))
     */
    public static String computeCanonicalFingerprint(String title, String content, String audience) {
        String canonical = normalizeText(title) + "\n---\n" +
                           normalizeText(content) + "\n---\n" +
                           normalizeAudience(audience);
        return sha256Hex(canonical);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
