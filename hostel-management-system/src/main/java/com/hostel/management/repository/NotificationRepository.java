package com.hostel.management.repository;

import com.hostel.management.model.Notification;
import com.hostel.management.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    long countByRecipientIdAndIsReadFalse(Long recipientId);
    boolean existsByRecipientIdAndTypeAndAmountAndIsReadFalse(Long recipientId, NotificationType type, Double amount);
    List<Notification> findByRecipientIdAndIsReadFalse(Long recipientId);
}
