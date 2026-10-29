package com.hostel.management.repository;

import com.hostel.management.model.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query("SELECT n FROM Notice n LEFT JOIN FETCH n.publisher ORDER BY n.createdAt DESC")
    List<Notice> findAllByOrderByCreatedAtDesc();

    boolean existsByContentFingerprint(String contentFingerprint);

    Optional<Notice> findByContentFingerprint(String contentFingerprint);
}
