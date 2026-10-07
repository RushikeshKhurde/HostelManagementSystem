package com.hostel.management.service;

import com.hostel.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentIdGeneratorService {

    private final UserRepository userRepository;

    @Transactional
    public synchronized String generateNextStudentId() {
        int year = LocalDate.now().getYear();
        String prefix = "STU-" + year + "-";
        
        Optional<String> maxStudentIdOpt = userRepository.findMaxStudentIdByPrefix(prefix + "%");
        int nextSequence = 1;

        if (maxStudentIdOpt.isPresent()) {
            String maxId = maxStudentIdOpt.get();
            try {
                String seqPart = maxId.substring(prefix.length());
                nextSequence = Integer.parseInt(seqPart) + 1;
            } catch (Exception e) {
                log.warn("Failed to parse sequence from max studentId '{}', falling back to count-based sequence", maxId);
                nextSequence = (int) (userRepository.count() + 1);
            }
        }

        String candidateId = String.format("%s%05d", prefix, nextSequence);
        while (userRepository.existsByStudentId(candidateId)) {
            nextSequence++;
            candidateId = String.format("%s%05d", prefix, nextSequence);
        }

        log.info("Generated unique Student ID: {}", candidateId);
        return candidateId;
    }
}
