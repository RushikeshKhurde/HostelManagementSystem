package com.hostel.management.repository;

import com.hostel.management.model.Booking;
import com.hostel.management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStudent(User student);
    List<Booking> findByStudentId(Long studentId);
    boolean existsByStudentIdAndStatusIn(Long studentId, Collection<Booking.BookingStatus> statuses);
    List<Booking> findByStudentIdAndStatusIn(Long studentId, Collection<Booking.BookingStatus> statuses);
}
