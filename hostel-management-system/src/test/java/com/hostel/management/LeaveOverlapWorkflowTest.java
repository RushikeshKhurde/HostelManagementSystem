package com.hostel.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.management.dto.LeaveRequestDto;
import com.hostel.management.model.LeaveRequest;
import com.hostel.management.model.Role;
import com.hostel.management.model.User;
import com.hostel.management.repository.LeaveRequestRepository;
import com.hostel.management.repository.UserRepository;
import com.hostel.management.security.JwtUtil;
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

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class LeaveOverlapWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User student1;
    private User student2;
    private User warden;
    private User admin;

    private String tokenStudent1;
    private String tokenStudent2;
    private String tokenWarden;
    private String tokenAdmin;

    // Standard benchmark test dates
    private final LocalDate APPROVED_START = LocalDate.of(2026, 10, 20);
    private final LocalDate APPROVED_END = LocalDate.of(2026, 10, 26);

    @BeforeEach
    void setUp() {
        leaveRequestRepository.deleteAll();

        student1 = userRepository.findByUsername("leave_stu1").orElseGet(() -> {
            User u = User.builder()
                    .fullName("Leave Student One")
                    .username("leave_stu1")
                    .email("leave_stu1@hostel.com")
                    .mobileNumber("9876543101")
                    .password(passwordEncoder.encode("Password@123"))
                    .role(Role.USER)
                    .gender("Male")
                    .address("Room A-101")
                    .status("ACTIVE")
                    .enabled(true)
                    .build();
            return userRepository.save(u);
        });
        tokenStudent1 = jwtUtil.generateToken(student1.getUsername(), student1.getRole().name());

        student2 = userRepository.findByUsername("leave_stu2").orElseGet(() -> {
            User u = User.builder()
                    .fullName("Leave Student Two")
                    .username("leave_stu2")
                    .email("leave_stu2@hostel.com")
                    .mobileNumber("9876543102")
                    .password(passwordEncoder.encode("Password@123"))
                    .role(Role.USER)
                    .gender("Female")
                    .address("Room B-202")
                    .status("ACTIVE")
                    .enabled(true)
                    .build();
            return userRepository.save(u);
        });
        tokenStudent2 = jwtUtil.generateToken(student2.getUsername(), student2.getRole().name());

        warden = userRepository.findByUsername("warden").orElseGet(() -> {
            User w = User.builder()
                    .fullName("Hostel Warden")
                    .username("warden")
                    .email("warden@hostel.com")
                    .mobileNumber("9888888101")
                    .password(passwordEncoder.encode("Warden@123456"))
                    .role(Role.WARDEN)
                    .gender("Other")
                    .address("Warden Office")
                    .status("ACTIVE")
                    .enabled(true)
                    .build();
            return userRepository.save(w);
        });
        tokenWarden = jwtUtil.generateToken(warden.getUsername(), warden.getRole().name());

        admin = userRepository.findByUsername("admin").orElseGet(() -> {
            User a = User.builder()
                    .fullName("Hostel Admin")
                    .username("admin")
                    .email("admin@hostel.com")
                    .mobileNumber("9999999101")
                    .password(passwordEncoder.encode("Admin@123456"))
                    .role(Role.ADMIN)
                    .gender("Other")
                    .address("Admin Office")
                    .status("ACTIVE")
                    .enabled(true)
                    .build();
            return userRepository.save(a);
        });
        tokenAdmin = jwtUtil.generateToken(admin.getUsername(), admin.getRole().name());
    }

    private LeaveRequest seedApprovedLeave(User student, LocalDate start, LocalDate end) {
        LeaveRequest leave = LeaveRequest.builder()
                .student(student)
                .leaveType("HOME_VISIT")
                .startDate(start)
                .endDate(end)
                .reason("Attending family event")
                .emergencyContact("9876543210")
                .status(LeaveRequest.LeaveStatus.APPROVED)
                .adminRemarks("Approved by warden")
                .build();
        return leaveRequestRepository.save(leave);
    }

    @Test
    @DisplayName("1. Existing approved leave: 20/10/2026-26/10/2026. New request: 23/10/2026-28/10/2026. Must reject")
    void testOverlapPartialEndRejected() throws Exception {
        seedApprovedLeave(student1, APPROVED_START, APPROVED_END);

        LeaveRequestDto req = LeaveRequestDto.builder()
                .leaveType("HOME_VISIT")
                .startDate(LocalDate.of(2026, 10, 23))
                .endDate(LocalDate.of(2026, 10, 28))
                .reason("Visiting home for festival")
                .emergencyContact("9876543210")
                .build();

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + tokenStudent1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("approved leave from 20/10/2026 to 26/10/2026 prevents you from applying")))
                .andExpect(jsonPath("$.message", containsString("starting 27/10/2026")));
    }

    @Test
    @DisplayName("2. Same exact date range: 20/10/2026-26/10/2026. Must reject")
    void testExactDateRangeRejected() throws Exception {
        seedApprovedLeave(student1, APPROVED_START, APPROVED_END);

        LeaveRequestDto req = LeaveRequestDto.builder()
                .leaveType("HOME_VISIT")
                .startDate(APPROVED_START)
                .endDate(APPROVED_END)
                .reason("Duplicate range request")
                .build();

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + tokenStudent1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("prevents you from applying for leave on overlapping dates")));
    }

    @Test
    @DisplayName("3. New request contained entirely inside the approved period: 22/10/2026-24/10/2026. Must reject")
    void testSubIntervalInsideApprovedPeriodRejected() throws Exception {
        seedApprovedLeave(student1, APPROVED_START, APPROVED_END);

        LeaveRequestDto req = LeaveRequestDto.builder()
                .leaveType("MEDICAL")
                .startDate(LocalDate.of(2026, 10, 22))
                .endDate(LocalDate.of(2026, 10, 24))
                .reason("Doctor visit inside approved period")
                .build();

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + tokenStudent1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("prevents you from applying for leave on overlapping dates")));
    }

    @Test
    @DisplayName("4. New request contains the entire approved period: 18/10/2026-28/10/2026. Must reject")
    void testSuperIntervalEnclosingApprovedPeriodRejected() throws Exception {
        seedApprovedLeave(student1, APPROVED_START, APPROVED_END);

        LeaveRequestDto req = LeaveRequestDto.builder()
                .leaveType("ACADEMIC_EVENT")
                .startDate(LocalDate.of(2026, 10, 18))
                .endDate(LocalDate.of(2026, 10, 28))
                .reason("Extended conference trip enclosing existing leave")
                .build();

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + tokenStudent1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("prevents you from applying for leave on overlapping dates")));
    }

    @Test
    @DisplayName("5. New request overlaps only the first day: 15/10/2026-20/10/2026. Must reject")
    void testOverlapOnlyFirstDayRejected() throws Exception {
        seedApprovedLeave(student1, APPROVED_START, APPROVED_END);

        LeaveRequestDto req = LeaveRequestDto.builder()
                .leaveType("HOME_VISIT")
                .startDate(LocalDate.of(2026, 10, 15))
                .endDate(LocalDate.of(2026, 10, 20))
                .reason("Trip overlapping only the start date boundary")
                .build();

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + tokenStudent1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("prevents you from applying for leave on overlapping dates")));
    }

    @Test
    @DisplayName("6. New request overlaps only the last day: 26/10/2026-30/10/2026. Must reject")
    void testOverlapOnlyLastDayRejected() throws Exception {
        seedApprovedLeave(student1, APPROVED_START, APPROVED_END);

        LeaveRequestDto req = LeaveRequestDto.builder()
                .leaveType("WEEKEND_OUT")
                .startDate(LocalDate.of(2026, 10, 26))
                .endDate(LocalDate.of(2026, 10, 30))
                .reason("Trip overlapping only the end date boundary")
                .build();

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + tokenStudent1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("prevents you from applying for leave on overlapping dates")));
    }

    @Test
    @DisplayName("7. New request starts on 27/10/2026 (immediately after approved leave ends). Must allow")
    void testAdjacentDayAfterApprovedPeriodAllowed() throws Exception {
        seedApprovedLeave(student1, APPROVED_START, APPROVED_END);

        LeaveRequestDto req = LeaveRequestDto.builder()
                .leaveType("HOME_VISIT")
                .startDate(LocalDate.of(2026, 10, 27))
                .endDate(LocalDate.of(2026, 10, 30))
                .reason("Subsequent non-overlapping trip")
                .build();

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + tokenStudent1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.startDate").value("2026-10-27"))
                .andExpect(jsonPath("$.endDate").value("2026-10-30"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("8. New request ends on 19/10/2026 (immediately before approved leave starts). Must allow")
    void testAdjacentDayBeforeApprovedPeriodAllowed() throws Exception {
        seedApprovedLeave(student1, APPROVED_START, APPROVED_END);

        LeaveRequestDto req = LeaveRequestDto.builder()
                .leaveType("WEEKEND_OUT")
                .startDate(LocalDate.of(2026, 10, 15))
                .endDate(LocalDate.of(2026, 10, 19))
                .reason("Preceding non-overlapping trip")
                .build();

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + tokenStudent1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.startDate").value("2026-10-15"))
                .andExpect(jsonPath("$.endDate").value("2026-10-19"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("9. Different student applies for same dates (20/10/2026-26/10/2026). Must allow")
    void testDifferentStudentIndependentLeaveAllowed() throws Exception {
        // Student 1 has approved leave
        seedApprovedLeave(student1, APPROVED_START, APPROVED_END);

        // Student 2 applies for the exact same dates
        LeaveRequestDto req = LeaveRequestDto.builder()
                .leaveType("HOME_VISIT")
                .startDate(APPROVED_START)
                .endDate(APPROVED_END)
                .reason("Student 2 family function")
                .build();

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + tokenStudent2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.student.fullName").value("Leave Student Two"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("10. Pending leaves do not block new applications under the defined policy")
    void testPendingLeaveDoesNotBlockNewApplications() throws Exception {
        // Student 1 creates first pending leave
        LeaveRequestDto req1 = LeaveRequestDto.builder()
                .leaveType("HOME_VISIT")
                .startDate(LocalDate.of(2026, 11, 10))
                .endDate(LocalDate.of(2026, 11, 15))
                .reason("First pending request")
                .build();

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + tokenStudent1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Student 1 creates second pending request overlapping the first pending request
        LeaveRequestDto req2 = LeaveRequestDto.builder()
                .leaveType("WEEKEND_OUT")
                .startDate(LocalDate.of(2026, 11, 12))
                .endDate(LocalDate.of(2026, 11, 18))
                .reason("Second pending request overlapping first pending request")
                .build();

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + tokenStudent1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("11. Rejected leave overlapping dates does not block new applications")
    void testRejectedLeaveDoesNotBlockNewApplication() throws Exception {
        // Create rejected leave
        LeaveRequest rejected = LeaveRequest.builder()
                .student(student1)
                .leaveType("HOME_VISIT")
                .startDate(LocalDate.of(2026, 11, 20))
                .endDate(LocalDate.of(2026, 11, 25))
                .reason("Previously rejected request")
                .status(LeaveRequest.LeaveStatus.REJECTED)
                .adminRemarks("Staff shortage on those dates")
                .build();
        leaveRequestRepository.save(rejected);

        // Student applies for the exact same dates
        LeaveRequestDto req = LeaveRequestDto.builder()
                .leaveType("HOME_VISIT")
                .startDate(LocalDate.of(2026, 11, 20))
                .endDate(LocalDate.of(2026, 11, 25))
                .reason("Re-applying with updated details")
                .build();

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + tokenStudent1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("12. Start date after end date is rejected with 400 Bad Request")
    void testStartDateAfterEndDateRejected() throws Exception {
        LeaveRequestDto req = LeaveRequestDto.builder()
                .leaveType("HOME_VISIT")
                .startDate(LocalDate.of(2026, 10, 25))
                .endDate(LocalDate.of(2026, 10, 20))
                .reason("Inverted dates")
                .build();

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + tokenStudent1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("End date cannot be before start date")));
    }

    @Test
    @DisplayName("13. Missing dates are rejected with 400 Bad Request")
    void testMissingDatesRejected() throws Exception {
        LeaveRequestDto req = LeaveRequestDto.builder()
                .leaveType("HOME_VISIT")
                .startDate(null)
                .endDate(null)
                .reason("Missing dates test")
                .build();

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + tokenStudent1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("14. Warden cannot approve a leave that overlaps another approved leave for the same student")
    void testWardenApprovalBlockedByOverlappingApprovedLeave() throws Exception {
        // Seed first approved leave
        seedApprovedLeave(student1, APPROVED_START, APPROVED_END);

        // Seed second pending leave submitted earlier that overlaps
        LeaveRequest pending = LeaveRequest.builder()
                .student(student1)
                .leaveType("WEEKEND_OUT")
                .startDate(LocalDate.of(2026, 10, 24))
                .endDate(LocalDate.of(2026, 10, 28))
                .reason("Pending request submitted concurrently")
                .status(LeaveRequest.LeaveStatus.PENDING)
                .build();
        pending = leaveRequestRepository.save(pending);

        // Warden attempts to approve the overlapping pending leave
        mockMvc.perform(put("/api/leaves/" + pending.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenWarden)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "APPROVED", "adminRemarks", "Approved"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot approve leave: student already has an approved leave from 20/10/2026 to 26/10/2026")));

        // Verify status remains PENDING in database
        LeaveRequest refreshed = leaveRequestRepository.findById(pending.getId()).orElseThrow();
        assertEquals(LeaveRequest.LeaveStatus.PENDING, refreshed.getStatus());
    }

    @Test
    @DisplayName("15. Leave currently being approved is excluded from its own overlap check")
    void testLeaveBeingApprovedExcludedFromOwnCheck() throws Exception {
        LeaveRequest pending = LeaveRequest.builder()
                .student(student1)
                .leaveType("HOME_VISIT")
                .startDate(LocalDate.of(2026, 12, 1))
                .endDate(LocalDate.of(2026, 12, 5))
                .reason("Isolated vacation")
                .status(LeaveRequest.LeaveStatus.PENDING)
                .build();
        pending = leaveRequestRepository.save(pending);

        // Approving this pending leave succeeds (does not collide with itself)
        mockMvc.perform(put("/api/leaves/" + pending.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenWarden)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "APPROVED", "adminRemarks", "Approved safely."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Idempotent re-approval also succeeds
        mockMvc.perform(put("/api/leaves/" + pending.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenWarden)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "APPROVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("16. Leave can be approved when no conflicting approved leave exists")
    void testLeaveApprovalSucceedsWithoutConflict() throws Exception {
        LeaveRequest pending = LeaveRequest.builder()
                .student(student1)
                .leaveType("MEDICAL")
                .startDate(LocalDate.of(2026, 12, 10))
                .endDate(LocalDate.of(2026, 12, 12))
                .reason("Dental appointment")
                .status(LeaveRequest.LeaveStatus.PENDING)
                .build();
        pending = leaveRequestRepository.save(pending);

        mockMvc.perform(put("/api/leaves/" + pending.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenWarden)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "APPROVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("17. Unauthorized users cannot approve leaves or bypass authorization")
    void testUnauthorizedApprovalDenied() throws Exception {
        LeaveRequest pending = LeaveRequest.builder()
                .student(student1)
                .leaveType("HOME_VISIT")
                .startDate(LocalDate.of(2026, 12, 15))
                .endDate(LocalDate.of(2026, 12, 20))
                .reason("Winter break")
                .status(LeaveRequest.LeaveStatus.PENDING)
                .build();
        pending = leaveRequestRepository.save(pending);

        // Student attempts to approve own leave -> 403 Forbidden
        mockMvc.perform(put("/api/leaves/" + pending.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenStudent1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "APPROVED"))))
                .andExpect(status().isForbidden());

        // Anonymous user attempts to approve -> 401 Unauthorized
        mockMvc.perform(put("/api/leaves/" + pending.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "APPROVED"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("18. Sequential approval of two overlapping pending leaves results in first APPROVED and second rejected")
    void testSequentialApprovalOfOverlappingRequestsPreventsDualApproval() throws Exception {
        LeaveRequest pending1 = LeaveRequest.builder()
                .student(student1)
                .leaveType("HOME_VISIT")
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 7))
                .reason("First overlapping pending request")
                .status(LeaveRequest.LeaveStatus.PENDING)
                .build();
        pending1 = leaveRequestRepository.save(pending1);

        LeaveRequest pending2 = LeaveRequest.builder()
                .student(student1)
                .leaveType("WEEKEND_OUT")
                .startDate(LocalDate.of(2026, 11, 5))
                .endDate(LocalDate.of(2026, 11, 10))
                .reason("Second overlapping pending request")
                .status(LeaveRequest.LeaveStatus.PENDING)
                .build();
        pending2 = leaveRequestRepository.save(pending2);

        // Warden approves pending1 -> SUCCESS
        mockMvc.perform(put("/api/leaves/" + pending1.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenWarden)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "APPROVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Warden attempts to approve pending2 -> REJECTED due to overlap with pending1
        mockMvc.perform(put("/api/leaves/" + pending2.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenWarden)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "APPROVED"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot approve leave: student already has an approved leave")));

        // Confirm in DB that pending1 is APPROVED and pending2 is still PENDING
        assertEquals(LeaveRequest.LeaveStatus.APPROVED, leaveRequestRepository.findById(pending1.getId()).orElseThrow().getStatus());
        assertEquals(LeaveRequest.LeaveStatus.PENDING, leaveRequestRepository.findById(pending2.getId()).orElseThrow().getStatus());
    }
}
