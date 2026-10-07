package com.hostel.management.repository;

import com.hostel.management.model.Booking;
import com.hostel.management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStudent(User student);
    List<Booking> findByStudentId(Long studentId);
    boolean existsByStudentIdAndStatusIn(Long studentId, Collection<Booking.BookingStatus> statuses);
    List<Booking> findByStudentIdAndStatusIn(Long studentId, Collection<Booking.BookingStatus> statuses);
    boolean existsByRoomId(Long roomId);
    long countByRoomId(Long roomId);
    long countByRoomIdAndStatus(Long roomId, Booking.BookingStatus status);
    List<Booking> findByRoomId(Long roomId);
    List<Booking> findByRoomIdAndStatus(Long roomId, Booking.BookingStatus status);
    List<Booking> findByRoomIdAndStatusOrderByCreatedAtAsc(Long roomId, Booking.BookingStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdWithLock(@Param("id") Long id);
}
