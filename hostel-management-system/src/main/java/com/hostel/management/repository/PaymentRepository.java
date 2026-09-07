package com.hostel.management.repository;

import com.hostel.management.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBookingStudentId(Long studentId);
}
