package com.hostel.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.management.dto.NoticeRequest;
import com.hostel.management.model.Notice;
import com.hostel.management.model.Role;
import com.hostel.management.model.User;
import com.hostel.management.repository.NoticeRepository;
import com.hostel.management.repository.NotificationRepository;
import com.hostel.management.repository.UserRepository;
import com.hostel.management.security.JwtUtil;
import com.hostel.management.service.NoticeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class NoticeDuplicateAndPublisherTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin1;
    private User admin2;
    private User warden1;
    private User student1;

    private String tokenAdmin1;
    private String tokenAdmin2;
    private String tokenWarden1;
    private String tokenStudent1;

    @BeforeEach
    void setUp() {
        noticeRepository.deleteAll();

        long timestamp = System.currentTimeMillis();

        admin1 = userRepository.save(User.builder()
                .username("test_admin1_" + timestamp)
                .fullName("Senior Hostel Administrator")
                .email("admin1_" + timestamp + "@hostel.com")
                .mobileNumber("99000" + (timestamp % 100000))
                .password(passwordEncoder.encode("AdminPass123"))
                .role(Role.ADMIN)
                .enabled(true)
                .emailVerified(true)
                .build());

        admin2 = userRepository.save(User.builder()
                .username("test_admin2_" + timestamp)
                .fullName("Assistant Administrator")
                .email("admin2_" + timestamp + "@hostel.com")
                .mobileNumber("99100" + (timestamp % 100000))
                .password(passwordEncoder.encode("AdminPass123"))
                .role(Role.ADMIN)
                .enabled(true)
                .emailVerified(true)
                .build());

        warden1 = userRepository.save(User.builder()
                .username("test_warden1_" + timestamp)
                .fullName("Chief Hostel Warden")
                .email("warden1_" + timestamp + "@hostel.com")
                .mobileNumber("99200" + (timestamp % 100000))
                .password(passwordEncoder.encode("WardenPass123"))
                .role(Role.WARDEN)
                .enabled(true)
                .emailVerified(true)
                .build());

        student1 = userRepository.save(User.builder()
                .username("test_student1_" + timestamp)
                .fullName("Rahul Sharma")
                .email("student1_" + timestamp + "@hostel.com")
                .mobileNumber("99300" + (timestamp % 100000))
                .password(passwordEncoder.encode("StudentPass123"))
                .role(Role.USER)
                .enabled(true)
                .emailVerified(true)
                .build());

        tokenAdmin1 = jwtUtil.generateToken(admin1.getUsername(), admin1.getRole().name());
        tokenAdmin2 = jwtUtil.generateToken(admin2.getUsername(), admin2.getRole().name());
        tokenWarden1 = jwtUtil.generateToken(warden1.getUsername(), warden1.getRole().name());
        tokenStudent1 = jwtUtil.generateToken(student1.getUsername(), student1.getRole().name());
    }

    @Test
    @DisplayName("1. The first announcement with unique content is published successfully")
    void testPublishUniqueAnnouncementSucceeds() throws Exception {
        NoticeRequest req = NoticeRequest.builder()
                .title("Water Supply Maintenance")
                .content("Water supply will be unavailable from 10 AM to 2 PM tomorrow.")
                .category("MAINTENANCE")
                .priority("NORMAL")
                .targetAudience("ALL")
                .build();

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Water Supply Maintenance"))
                .andExpect(jsonPath("$.publisherName").value("Senior Hostel Administrator"))
                .andExpect(jsonPath("$.publisherRole").value("ADMIN"))
                .andExpect(jsonPath("$.postedBy").value("Senior Hostel Administrator (ADMIN)"));

        assertEquals(1, noticeRepository.count());
    }

    @Test
    @DisplayName("2. The same Admin cannot publish the same announcement again (HTTP 409)")
    void testSameAdminCannotPublishSameAnnouncement() throws Exception {
        NoticeRequest req = NoticeRequest.builder()
                .title("Power Outage Alert")
                .content("Scheduled generator testing on Saturday between 2 PM and 4 PM.")
                .priority("IMPORTANT")
                .targetAudience("ALL")
                .build();

        // First publication succeeds
        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Same admin attempting duplicate publication
        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(NoticeService.DUPLICATE_NOTICE_MESSAGE));

        assertEquals(1, noticeRepository.count());
    }

    @Test
    @DisplayName("3. A different Admin cannot publish the same announcement (HTTP 409)")
    void testDifferentAdminCannotPublishSameAnnouncement() throws Exception {
        NoticeRequest req = NoticeRequest.builder()
                .title("Mess Food Survey 2026")
                .content("Please submit your monthly dining hall survey via the student portal.")
                .priority("NORMAL")
                .targetAudience("ALL")
                .build();

        // Admin 1 publishes
        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Admin 2 attempts identical publication
        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(NoticeService.DUPLICATE_NOTICE_MESSAGE));

        assertEquals(1, noticeRepository.count());
    }

    @Test
    @DisplayName("4. A Warden cannot publish a duplicate created by an Admin (HTTP 409)")
    void testWardenCannotPublishDuplicateOfAdminNotice() throws Exception {
        NoticeRequest req = NoticeRequest.builder()
                .title("Hostel Wi-Fi Upgrade")
                .content("Network routers in Block B will be upgraded tonight.")
                .priority("NORMAL")
                .targetAudience("BLOCK_B")
                .build();

        // Admin publishes
        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Warden attempts duplicate publication
        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenWarden1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(NoticeService.DUPLICATE_NOTICE_MESSAGE));

        assertEquals(1, noticeRepository.count());
    }

    @Test
    @DisplayName("5. An Admin cannot publish a duplicate created by a Warden (HTTP 409)")
    void testAdminCannotPublishDuplicateOfWardenNotice() throws Exception {
        NoticeRequest req = NoticeRequest.builder()
                .title("Curfew Inspection Notice")
                .content("Night roll call will strictly begin at 10 PM tonight in all blocks.")
                .priority("HIGH")
                .targetAudience("ALL")
                .build();

        // Warden publishes
        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenWarden1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publisherName").value("Chief Hostel Warden"))
                .andExpect(jsonPath("$.publisherRole").value("WARDEN"));

        // Admin attempts duplicate publication
        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(NoticeService.DUPLICATE_NOTICE_MESSAGE));

        assertEquals(1, noticeRepository.count());
    }

    @Test
    @DisplayName("6. Leading and trailing whitespace does not bypass duplicate detection")
    void testLeadingTrailingWhitespaceDoesNotBypassDetection() throws Exception {
        NoticeRequest req1 = NoticeRequest.builder()
                .title("Sports Room Equipment")
                .content("Table tennis equipment must be returned before 8 PM daily.")
                .priority("NORMAL")
                .targetAudience("ALL")
                .build();

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        NoticeRequest req2 = NoticeRequest.builder()
                .title("   Sports Room Equipment   ")
                .content("   Table tennis equipment must be returned before 8 PM daily.   ")
                .priority("NORMAL")
                .targetAudience("  ALL  ")
                .build();

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenWarden1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(NoticeService.DUPLICATE_NOTICE_MESSAGE));

        assertEquals(1, noticeRepository.count());
    }

    @Test
    @DisplayName("7. Repeated whitespace and line endings are normalized according to policy")
    void testRepeatedWhitespaceAndLineEndingsNormalized() throws Exception {
        NoticeRequest req1 = NoticeRequest.builder()
                .title("Library Timing Notice")
                .content("Reading room will open at 8 AM\nand close at 11 PM.")
                .priority("NORMAL")
                .targetAudience("ALL")
                .build();

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        NoticeRequest req2 = NoticeRequest.builder()
                .title("Library    Timing    Notice")
                .content("Reading   room    will    open    at   8   AM\r\nand   close   at   11   PM.")
                .priority("NORMAL")
                .targetAudience("ALL")
                .build();

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(NoticeService.DUPLICATE_NOTICE_MESSAGE));

        assertEquals(1, noticeRepository.count());
    }

    @Test
    @DisplayName("8. Case-only differences do not bypass duplicate detection")
    void testCaseOnlyDifferencesDoNotBypassDetection() throws Exception {
        NoticeRequest req1 = NoticeRequest.builder()
                .title("Fire Safety Drill")
                .content("Assembly point is the central hostel ground.")
                .priority("IMPORTANT")
                .targetAudience("ALL")
                .build();

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        NoticeRequest req2 = NoticeRequest.builder()
                .title("FIRE SAFETY DRILL")
                .content("ASSEMBLY POINT IS THE CENTRAL HOSTEL GROUND.")
                .priority("IMPORTANT")
                .targetAudience("all")
                .build();

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenWarden1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(NoticeService.DUPLICATE_NOTICE_MESSAGE));

        assertEquals(1, noticeRepository.count());
    }

    @Test
    @DisplayName("9. Same title with genuinely different description content is allowed")
    void testSameTitleWithDifferentDescriptionAllowed() throws Exception {
        NoticeRequest req1 = NoticeRequest.builder()
                .title("Water Supply Maintenance")
                .content("Water supply will be unavailable from 10 AM to 2 PM tomorrow.")
                .priority("NORMAL")
                .targetAudience("ALL")
                .build();

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        NoticeRequest req2 = NoticeRequest.builder()
                .title("Water Supply Maintenance")
                .content("Water supply will be unavailable from 10 AM to 5 PM tomorrow due to pipe repairs.")
                .priority("NORMAL")
                .targetAudience("ALL")
                .build();

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenWarden1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk());

        assertEquals(2, noticeRepository.count());
    }

    @Test
    @DisplayName("10. Different target audience follows documented policy (allowed)")
    void testDifferentAudienceFollowsPolicy() throws Exception {
        NoticeRequest req1 = NoticeRequest.builder()
                .title("Geyser Repair Work")
                .content("Hot water geysers will undergo regular maintenance today.")
                .priority("NORMAL")
                .targetAudience("BLOCK_A")
                .build();

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        NoticeRequest req2 = NoticeRequest.builder()
                .title("Geyser Repair Work")
                .content("Hot water geysers will undergo regular maintenance today.")
                .priority("NORMAL")
                .targetAudience("BLOCK_B")
                .build();

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenWarden1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk());

        assertEquals(2, noticeRepository.count());
    }

    @Test
    @DisplayName("11. Changing only the priority does not bypass duplicate prevention (HTTP 409)")
    void testChangingOnlyPriorityDoesNotBypassDuplicatePrevention() throws Exception {
        NoticeRequest req1 = NoticeRequest.builder()
                .title("Elevator Inspection")
                .content("Technicians will service the elevator shafts between 1 PM and 3 PM.")
                .priority("NORMAL")
                .targetAudience("ALL")
                .build();

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        NoticeRequest req2 = NoticeRequest.builder()
                .title("Elevator Inspection")
                .content("Technicians will service the elevator shafts between 1 PM and 3 PM.")
                .priority("URGENT")
                .targetAudience("ALL")
                .build();

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenWarden1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(NoticeService.DUPLICATE_NOTICE_MESSAGE));

        assertEquals(1, noticeRepository.count());
    }

    @Test
    @DisplayName("12. Duplicate returns HTTP 409 with safe, understandable error message")
    void testDuplicateReturns409WithClearMessage() throws Exception {
        NoticeRequest req = NoticeRequest.builder()
                .title("Mess Holiday Menu")
                .content("Special Diwali feast dinner will be served on Sunday evening.")
                .priority("IMPORTANT")
                .targetAudience("ALL")
                .build();

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("This announcement has already been published with the same content. Please update the announcement before publishing."));
    }

    @Test
    @DisplayName("13. Rejected duplicate does not create an announcement record")
    void testRejectedDuplicateDoesNotCreateRecord() throws Exception {
        NoticeRequest req = NoticeRequest.builder()
                .title("Quiet Hours Reminder")
                .content("Strict quiet hours are in effect from 11 PM to 6 AM.")
                .priority("NORMAL")
                .targetAudience("ALL")
                .build();

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        long countAfterFirst = noticeRepository.count();
        assertEquals(1, countAfterFirst);

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenWarden1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());

        long countAfterDuplicate = noticeRepository.count();
        assertEquals(1, countAfterDuplicate, "Notice record count must remain exactly 1 after rejected duplicate");
    }

    @Test
    @DisplayName("14. Rejected duplicate does not generate notifications")
    void testRejectedDuplicateDoesNotGenerateNotifications() throws Exception {
        long notificationsBefore = notificationRepository.count();

        NoticeRequest req = NoticeRequest.builder()
                .title("Gym Cleanliness Policy")
                .content("Wipe down fitness machines after every usage.")
                .priority("NORMAL")
                .targetAudience("ALL")
                .build();

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenWarden1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());

        long notificationsAfter = notificationRepository.count();
        assertEquals(notificationsBefore, notificationsAfter, "No extraneous notifications should be created");
    }

    @Test
    @DisplayName("15. Concurrent duplicate publication attempts cannot both succeed (concurrency guarantee)")
    void testConcurrentDuplicatePublicationSafety() throws Exception {
        NoticeRequest req = NoticeRequest.builder()
                .title("Simultaneous Broadcast Emergency Drill")
                .content("Evacuate via fire stairs immediately upon hearing the continuous siren.")
                .priority("URGENT")
                .targetAudience("ALL")
                .build();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final String token = (i % 2 == 0) ? tokenAdmin1 : tokenWarden1;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    int status = mockMvc.perform(post("/api/notices")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req)))
                            .andReturn()
                            .getResponse()
                            .getStatus();

                    if (status == 200) {
                        successCount.incrementAndGet();
                    } else if (status == 409) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // unexpected error
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one concurrent publication must succeed");
        assertEquals(threadCount - 1, conflictCount.get(), "All other concurrent attempts must be rejected with 409 Conflict");
        assertEquals(1, noticeRepository.count(), "Exactly one notice record must exist in the database");
    }

    @Test
    @DisplayName("16. Existing historical duplicates do not prevent application or notice retrieval")
    void testHistoricalDuplicatesDoNotBreakApplication() throws Exception {
        // Create two historical notices with identical content and null fingerprints (simulating legacy data)
        Notice historical1 = noticeRepository.save(Notice.builder()
                .title("Annual Hostel Sports Meet 2026")
                .content("Registration starts tomorrow at the sports complex.")
                .category("SPORTS")
                .priority(Notice.NoticePriority.HIGH)
                .targetAudience("ALL")
                .postedBy("Senior Hostel Administrator")
                .contentFingerprint(null)
                .build());

        Notice historical2 = noticeRepository.save(Notice.builder()
                .title("Annual Hostel Sports Meet 2026")
                .content("Registration starts tomorrow at the sports complex.")
                .category("SPORTS")
                .priority(Notice.NoticePriority.HIGH)
                .targetAudience("ALL")
                .postedBy(null)
                .contentFingerprint(null)
                .build());

        // Verifying retrieval succeeds without throwing exceptions
        mockMvc.perform(get("/api/notices")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Annual Hostel Sports Meet 2026"));

        // Verifying new attempt to publish identical content to historical notice is blocked
        NoticeRequest duplicateOfHistorical = NoticeRequest.builder()
                .title("Annual Hostel Sports Meet 2026")
                .content("Registration starts tomorrow at the sports complex.")
                .priority("HIGH")
                .targetAudience("ALL")
                .build();

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateOfHistorical)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(NoticeService.DUPLICATE_NOTICE_MESSAGE));
    }

    @Test
    @DisplayName("17. A notice created by an Admin records the actual Admin publisher")
    void testNoticeCreatedByAdminRecordsActualPublisher() throws Exception {
        NoticeRequest req = NoticeRequest.builder()
                .title("Admin Office Working Hours")
                .content("Administrative inquiries are handled between 9 AM and 5 PM.")
                .priority("NORMAL")
                .targetAudience("ALL")
                .build();

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publisherId").value(admin1.getId()))
                .andExpect(jsonPath("$.publisherName").value("Senior Hostel Administrator"))
                .andExpect(jsonPath("$.publisherRole").value("ADMIN"))
                .andExpect(jsonPath("$.postedBy").value("Senior Hostel Administrator (ADMIN)"));

        Notice saved = noticeRepository.findAllByOrderByCreatedAtDesc().get(0);
        assertNotNull(saved.getPublisher());
        assertEquals(admin1.getId(), saved.getPublisher().getId());
        assertEquals("Senior Hostel Administrator", saved.getPublisher().getFullName());
    }

    @Test
    @DisplayName("18. A notice created by a Warden records the actual Warden publisher")
    void testNoticeCreatedByWardenRecordsActualPublisher() throws Exception {
        NoticeRequest req = NoticeRequest.builder()
                .title("Warden Welfare Consultation")
                .content("Students may visit the warden office for personal grievance consultations.")
                .priority("NORMAL")
                .targetAudience("ALL")
                .build();

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenWarden1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publisherId").value(warden1.getId()))
                .andExpect(jsonPath("$.publisherName").value("Chief Hostel Warden"))
                .andExpect(jsonPath("$.publisherRole").value("WARDEN"))
                .andExpect(jsonPath("$.postedBy").value("Chief Hostel Warden (WARDEN)"));

        Notice saved = noticeRepository.findAllByOrderByCreatedAtDesc().get(0);
        assertNotNull(saved.getPublisher());
        assertEquals(warden1.getId(), saved.getPublisher().getId());
        assertEquals("Chief Hostel Warden", saved.getPublisher().getFullName());
    }

    @Test
    @DisplayName("19. A client cannot impersonate another publisher by submitting custom postedBy or publisherId")
    void testClientCannotImpersonatePublisher() throws Exception {
        // Attacker sends postedBy="Chief Warden" and publisherId=999 in JSON body
        String maliciousPayload = "{" +
                "\"title\": \"Unauthorized Advisory\"," +
                "\"content\": \"Disregard all curfew warnings.\"," +
                "\"postedBy\": \"Chief Warden\"," +
                "\"publisherId\": 9999," +
                "\"priority\": \"NORMAL\"," +
                "\"targetAudience\": \"ALL\"" +
                "}";

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenAdmin1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousPayload))
                .andExpect(status().isOk())
                // Backend must record actual authenticated user (admin1), ignoring injected JSON fields
                .andExpect(jsonPath("$.publisherId").value(admin1.getId()))
                .andExpect(jsonPath("$.publisherName").value("Senior Hostel Administrator"))
                .andExpect(jsonPath("$.publisherRole").value("ADMIN"))
                .andExpect(jsonPath("$.postedBy").value("Senior Hostel Administrator (ADMIN)"));
    }

    @Test
    @DisplayName("20. An unauthorized Student cannot publish a notice (HTTP 403 Forbidden)")
    void testUnauthorizedStudentCannotPublishNotice() throws Exception {
        NoticeRequest req = NoticeRequest.builder()
                .title("Student Party Announcement")
                .content("DJ night tonight in the common hall.")
                .priority("URGENT")
                .targetAudience("ALL")
                .build();

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + tokenStudent1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        assertEquals(0, noticeRepository.count());
    }

    @Test
    @DisplayName("21. Historical notices without publisher render safely with 'Publisher unavailable'")
    void testHistoricalNoticeWithoutPublisherRendersSafely() throws Exception {
        Notice legacyNotice = noticeRepository.save(Notice.builder()
                .title("Legacy Circular from 2024")
                .content("Older hostel announcement without publisher association.")
                .category("ARCHIVE")
                .priority(Notice.NoticePriority.NORMAL)
                .targetAudience("ALL")
                .postedBy(null)
                .publisher(null)
                .contentFingerprint(null)
                .build());

        mockMvc.perform(get("/api/notices")
                        .header("Authorization", "Bearer " + tokenStudent1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].publisherName").value("Publisher unavailable"))
                .andExpect(jsonPath("$[0].postedBy").value("Publisher unavailable"));
    }
}
