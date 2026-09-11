package com.hostel.management.controller;

import com.hostel.management.model.*;
import com.hostel.management.repository.*;
import com.hostel.management.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ComplaintRepository complaintRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final NoticeRepository noticeRepository;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        long totalUsers = userRepository.count();
        long totalStudents = userRepository.countByRole(Role.USER);
        List<Room> rooms = roomRepository.findAll();
        long totalRooms = rooms.size();
        long occupiedRooms = rooms.stream().filter(r -> r.getOccupied() != null && r.getOccupied() > 0).count();
        int totalCapacity = rooms.stream().mapToInt(r -> r.getCapacity() != null ? r.getCapacity() : 0).sum();
        int occupiedBeds = rooms.stream().mapToInt(r -> r.getOccupied() != null ? r.getOccupied() : 0).sum();
        int availableBeds = Math.max(0, totalCapacity - occupiedBeds);

        List<Payment> payments = paymentRepository.findAll();
        double totalRevenue = payments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.SUCCESS)
                .mapToDouble(Payment::getAmount)
                .sum();

        List<Booking> bookings = bookingRepository.findAll();
        long pendingBookings = bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.PENDING).count();
        long pendingComplaints = complaintRepository.countByStatus(Complaint.ComplaintStatus.PENDING);
        long pendingLeaves = leaveRequestRepository.countByStatus(LeaveRequest.LeaveStatus.PENDING);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalStudents", totalStudents);
        stats.put("totalHostels", 3);
        stats.put("totalRooms", totalRooms);
        stats.put("occupiedRooms", occupiedRooms);
        stats.put("totalCapacity", totalCapacity);
        stats.put("occupiedBeds", occupiedBeds);
        stats.put("availableBeds", availableBeds);
        stats.put("totalRevenue", totalRevenue);
        stats.put("pendingBookings", pendingBookings);
        stats.put("pendingComplaints", pendingComplaints);
        stats.put("pendingLeaves", pendingLeaves);

        // Breakdown by room type
        Map<String, Long> roomTypeCounts = rooms.stream()
                .collect(Collectors.groupingBy(r -> r.getRoomType().name(), Collectors.counting()));
        stats.put("roomTypeDistribution", roomTypeCounts);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/student-stats")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getStudentStats(@AuthenticationPrincipal UserPrincipal principal) {
        Long studentId = principal.getUser().getId();

        List<Booking> bookings = bookingRepository.findByStudentId(studentId);
        Booking activeBooking = bookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.APPROVED)
                .findFirst()
                .orElse(bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.PENDING).findFirst().orElse(null));

        List<Payment> payments = paymentRepository.findByBookingStudentId(studentId);
        double totalPaid = payments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.SUCCESS)
                .mapToDouble(Payment::getAmount)
                .sum();

        List<Complaint> complaints = complaintRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        List<LeaveRequest> leaves = leaveRequestRepository.findByStudentIdOrderByCreatedAtDesc(studentId);

        // Roommates search if student has an approved room
        List<Map<String, String>> roommates = new ArrayList<>();
        if (activeBooking != null && activeBooking.getRoom() != null && activeBooking.getStatus() == Booking.BookingStatus.APPROVED) {
            Long roomId = activeBooking.getRoom().getId();
            List<Booking> roomBookings = bookingRepository.findAll().stream()
                    .filter(b -> b.getRoom() != null && b.getRoom().getId().equals(roomId)
                            && b.getStatus() == Booking.BookingStatus.APPROVED
                            && !b.getStudent().getId().equals(studentId))
                    .collect(Collectors.toList());

            for (Booking b : roomBookings) {
                roommates.add(Map.of(
                        "name", b.getStudent().getFullName(),
                        "email", b.getStudent().getEmail(),
                        "mobile", b.getStudent().getMobileNumber()
                ));
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeBooking", activeBooking);
        stats.put("totalPaid", totalPaid);
        stats.put("totalBookings", bookings.size());
        stats.put("totalComplaints", complaints.size());
        stats.put("pendingComplaints", complaints.stream().filter(c -> c.getStatus() == Complaint.ComplaintStatus.PENDING).count());
        stats.put("totalLeaves", leaves.size());
        stats.put("pendingLeaves", leaves.stream().filter(l -> l.getStatus() == LeaveRequest.LeaveStatus.PENDING).count());
        stats.put("roommates", roommates);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/warden-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<Map<String, Object>> getWardenStats() {
        long totalStudents = userRepository.countByRole(Role.USER);
        List<Room> rooms = roomRepository.findAll();
        long totalRooms = rooms.size();
        long occupiedRooms = rooms.stream().filter(r -> r.getOccupied() != null && r.getOccupied() > 0).count();
        int totalCapacity = rooms.stream().mapToInt(r -> r.getCapacity() != null ? r.getCapacity() : 0).sum();
        int occupiedBeds = rooms.stream().mapToInt(r -> r.getOccupied() != null ? r.getOccupied() : 0).sum();
        int availableBeds = Math.max(0, totalCapacity - occupiedBeds);

        List<Booking> bookings = bookingRepository.findAll();
        long pendingBookings = bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.PENDING).count();
        long pendingComplaints = complaintRepository.countByStatus(Complaint.ComplaintStatus.PENDING);
        long pendingLeaves = leaveRequestRepository.countByStatus(LeaveRequest.LeaveStatus.PENDING);
        long activeNotices = noticeRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", totalStudents);
        stats.put("totalRooms", totalRooms);
        stats.put("occupiedRooms", occupiedRooms);
        stats.put("totalCapacity", totalCapacity);
        stats.put("occupiedBeds", occupiedBeds);
        stats.put("availableBeds", availableBeds);
        stats.put("pendingBookings", pendingBookings);
        stats.put("pendingComplaints", pendingComplaints);
        stats.put("pendingLeaves", pendingLeaves);
        stats.put("activeNotices", activeNotices);

        // Breakdown by room type for operational planning
        Map<String, Long> roomTypeCounts = rooms.stream()
                .collect(Collectors.groupingBy(r -> r.getRoomType().name(), Collectors.counting()));
        stats.put("roomTypeDistribution", roomTypeCounts);

        return ResponseEntity.ok(stats);
    }
}
