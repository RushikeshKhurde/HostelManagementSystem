package com.hostel.management.service;

import com.hostel.management.exception.ApiException;
import com.hostel.management.model.*;
import com.hostel.management.repository.BookingRepository;
import com.hostel.management.repository.NotificationRepository;
import com.hostel.management.repository.PaymentRepository;
import com.hostel.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    public List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCountForUser(Long userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    public Notification markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException("Notification not found", HttpStatus.NOT_FOUND));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new ApiException("You are not authorized to update this notification", HttpStatus.FORBIDDEN);
        }

        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByRecipientIdAndIsReadFalse(userId);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public Map<String, Object> sendFeeReminder(Long studentId, User sender) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ApiException("Student not found", HttpStatus.NOT_FOUND));

        if (student.getRole() != Role.USER) {
            throw new ApiException("Fee reminders can only be sent to students", HttpStatus.BAD_REQUEST);
        }

        // Check for active approved room booking
        List<Booking> approvedBookings = bookingRepository.findByStudentIdAndStatusIn(
                studentId, List.of(Booking.BookingStatus.APPROVED)
        );

        if (approvedBookings.isEmpty()) {
            throw new ApiException("This student has no pending hostel fees.", HttpStatus.BAD_REQUEST);
        }

        Booking activeBooking = approvedBookings.get(0);
        double totalFee = activeBooking.getRoom().getPricePerMonth();

        // Calculate paid amount
        List<Payment> payments = paymentRepository.findByBookingStudentId(studentId);
        double totalPaid = payments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.SUCCESS)
                .mapToDouble(Payment::getAmount)
                .sum();

        double remainingAmount = totalFee - totalPaid;

        if (remainingAmount <= 0) {
            throw new ApiException("This student has no pending hostel fees.", HttpStatus.BAD_REQUEST);
        }

        // Duplicate spam protection: check if an unread fee reminder for same student and same amount exists
        boolean duplicateExists = notificationRepository.existsByRecipientIdAndTypeAndAmountAndIsReadFalse(
                studentId, NotificationType.FEE_REMINDER, remainingAmount
        );
        if (duplicateExists) {
            throw new ApiException(
                    "A fee reminder for the current pending amount (\u20B9" + String.format("%,.0f", remainingAmount) + ") has already been sent to this student.",
                    HttpStatus.CONFLICT
            );
        }

        String formattedTotal = String.format("%,.0f", totalFee);
        String formattedPaid = String.format("%,.0f", totalPaid);
        String formattedRemaining = String.format("%,.0f", remainingAmount);

        String title = "Hostel Fee Reminder";
        String message = "Hello " + student.getFullName() + ",\n\nYour hostel fee is currently pending.\n\nTotal Hostel Fee: \u20B9" + formattedTotal + "\nAmount Paid: \u20B9" + formattedPaid + "\nRemaining Amount: \u20B9" + formattedRemaining + "\n\nPlease pay the remaining hostel fee as soon as possible.";

        Notification notification = Notification.builder()
                .recipient(student)
                .title(title)
                .message(message)
                .type(NotificationType.FEE_REMINDER)
                .amount(remainingAmount)
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Fee reminder sent successfully to " + student.getFullName());
        result.put("studentId", student.getId());
        result.put("studentName", student.getFullName());
        result.put("totalFee", totalFee);
        result.put("totalPaid", totalPaid);
        result.put("remainingAmount", remainingAmount);
        result.put("notificationId", notification.getId());
        return result;
    }

    @Transactional
    public Map<String, Object> sendBulkFeeReminders(User sender) {
        List<User> students = userRepository.findByRole(Role.USER);
        int totalStudentsChecked = students.size();
        int studentsWithPendingFees = 0;
        int notificationsCreated = 0;
        int notificationsSkipped = 0;

        for (User student : students) {
            List<Booking> approvedBookings = bookingRepository.findByStudentIdAndStatusIn(
                    student.getId(), List.of(Booking.BookingStatus.APPROVED)
            );

            if (approvedBookings.isEmpty()) {
                continue;
            }

            Booking activeBooking = approvedBookings.get(0);
            double totalFee = activeBooking.getRoom().getPricePerMonth();

            List<Payment> payments = paymentRepository.findByBookingStudentId(student.getId());
            double totalPaid = payments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.SUCCESS)
                    .mapToDouble(Payment::getAmount)
                    .sum();

            double remainingAmount = totalFee - totalPaid;

            if (remainingAmount <= 0) {
                continue;
            }

            studentsWithPendingFees++;

            boolean duplicateExists = notificationRepository.existsByRecipientIdAndTypeAndAmountAndIsReadFalse(
                    student.getId(), NotificationType.FEE_REMINDER, remainingAmount
            );

            if (duplicateExists) {
                notificationsSkipped++;
                continue;
            }

            String formattedTotal = String.format("%,.0f", totalFee);
            String formattedPaid = String.format("%,.0f", totalPaid);
            String formattedRemaining = String.format("%,.0f", remainingAmount);

            String title = "Hostel Fee Reminder";
            String message = "Hello " + student.getFullName() + ",\n\nYour hostel fee is currently pending.\n\nTotal Hostel Fee: \u20B9" + formattedTotal + "\nAmount Paid: \u20B9" + formattedPaid + "\nRemaining Amount: \u20B9" + formattedRemaining + "\n\nPlease pay the remaining hostel fee as soon as possible.";

            Notification notification = Notification.builder()
                    .recipient(student)
                    .title(title)
                    .message(message)
                    .type(NotificationType.FEE_REMINDER)
                    .amount(remainingAmount)
                    .isRead(false)
                    .build();

            notificationRepository.save(notification);
            notificationsCreated++;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Fee reminders processed successfully");
        response.put("totalStudentsChecked", totalStudentsChecked);
        response.put("studentsWithPendingFees", studentsWithPendingFees);
        response.put("notificationsCreated", notificationsCreated);
        response.put("notificationsSkipped", notificationsSkipped);
        return response;
    }
}
