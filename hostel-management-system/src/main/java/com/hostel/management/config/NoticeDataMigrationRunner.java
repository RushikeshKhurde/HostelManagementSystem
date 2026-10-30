package com.hostel.management.config;

import com.hostel.management.model.Notice;
import com.hostel.management.repository.NoticeRepository;
import com.hostel.management.repository.UserRepository;
import com.hostel.management.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Order(10)
@RequiredArgsConstructor
public class NoticeDataMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(NoticeDataMigrationRunner.class);

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Inspecting notice records for publisher associations and canonical content fingerprints...");

        List<Notice> notices = noticeRepository.findAll();
        if (notices.isEmpty()) {
            return;
        }

        Set<String> seenFingerprints = new HashSet<>();
        // First collect all existing assigned fingerprints
        for (Notice n : notices) {
            if (n.getContentFingerprint() != null && !n.getContentFingerprint().isBlank()) {
                seenFingerprints.add(n.getContentFingerprint());
            }
        }

        int updatedCount = 0;
        int historicalDuplicatesPreserved = 0;

        for (Notice notice : notices) {
            boolean modified = false;

            // 1. Content Fingerprint backfill
            if (notice.getContentFingerprint() == null || notice.getContentFingerprint().isBlank()) {
                String fp = NoticeService.computeCanonicalFingerprint(
                        notice.getTitle(),
                        notice.getContent(),
                        notice.getTargetAudience()
                );

                if (!seenFingerprints.contains(fp)) {
                    notice.setContentFingerprint(fp);
                    seenFingerprints.add(fp);
                    modified = true;
                } else {
                    historicalDuplicatesPreserved++;
                    log.warn("Historical duplicate notice detected (id={}, title='{}', createdAt={}). " +
                             "Preserving record without duplicate fingerprint to maintain database uniqueness integrity.",
                            notice.getId(), notice.getTitle(), notice.getCreatedAt());
                }
            }

            // 2. Publisher association backfill (safely map legacy postedBy string if possible)
            if (notice.getPublisher() == null && notice.getPostedBy() != null) {
                String posted = notice.getPostedBy().trim().toLowerCase();
                if (posted.contains("warden")) {
                    userRepository.findByUsername("warden").ifPresent(w -> {
                        notice.setPublisher(w);
                    });
                    if (notice.getPublisher() != null) {
                        modified = true;
                    }
                } else if (posted.contains("admin") || posted.contains("administrator")) {
                    userRepository.findByUsername("admin").ifPresent(a -> {
                        notice.setPublisher(a);
                    });
                    if (notice.getPublisher() != null) {
                        modified = true;
                    }
                }
            }

            if (modified) {
                noticeRepository.save(notice);
                updatedCount++;
            }
        }

        log.info("Notice data migration complete: updated {} notices. Preserved {} historical duplicate records.",
                updatedCount, historicalDuplicatesPreserved);
    }
}
