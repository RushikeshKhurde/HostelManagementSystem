package com.hostel.management.config;

import com.hostel.management.model.Role;
import com.hostel.management.model.Room;
import com.hostel.management.model.User;
import com.hostel.management.repository.RoomRepository;
import com.hostel.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Creates the single default administrator account if it does not already exist
        if (!userRepository.existsByUsername("Admin") && !userRepository.existsByEmail("admin@hostel.com")) {
            User admin = User.builder()
                    .fullName("System Administrator")
                    .username("Admin")
                    .email("admin@hostel.com")
                    .mobileNumber("9999999999")
                    .password(passwordEncoder.encode("Admin123"))
                    .role(Role.ADMIN)
                    .gender("Other")
                    .address("Hostel Administrative Office, Block A")
                    .status("ACTIVE")
                    .enabled(true)
                    .build();
            userRepository.save(admin);
        }

        // Creates the single default hostel warden account if it does not already exist
        if (!userRepository.existsByUsername("Warden") && !userRepository.existsByEmail("warden@hostel.com")) {
            User warden = User.builder()
                    .fullName("Chief Hostel Warden")
                    .username("Warden")
                    .email("warden@hostel.com")
                    .mobileNumber("9888888888")
                    .password(passwordEncoder.encode("Warden123"))
                    .role(Role.WARDEN)
                    .gender("Other")
                    .address("Hostel Warden Office, Ground Floor")
                    .status("ACTIVE")
                    .enabled(true)
                    .build();
            userRepository.save(warden);
        }

        // Seed initial room inventory if database is empty
        if (roomRepository.count() == 0) {
            roomRepository.save(Room.builder().roomNumber("A-101").roomType(Room.RoomType.SINGLE).capacity(1).occupied(0).pricePerMonth(8000.0).status(Room.RoomStatus.AVAILABLE).build());
            roomRepository.save(Room.builder().roomNumber("A-102").roomType(Room.RoomType.DOUBLE).capacity(2).occupied(0).pricePerMonth(5500.0).status(Room.RoomStatus.AVAILABLE).build());
            roomRepository.save(Room.builder().roomNumber("A-103").roomType(Room.RoomType.TRIPLE).capacity(3).occupied(1).pricePerMonth(4200.0).status(Room.RoomStatus.AVAILABLE).build());
            roomRepository.save(Room.builder().roomNumber("B-201").roomType(Room.RoomType.DOUBLE).capacity(2).occupied(2).pricePerMonth(5500.0).status(Room.RoomStatus.FULL).build());
            roomRepository.save(Room.builder().roomNumber("B-202").roomType(Room.RoomType.SINGLE).capacity(1).occupied(0).pricePerMonth(8500.0).status(Room.RoomStatus.AVAILABLE).build());
            roomRepository.save(Room.builder().roomNumber("B-203").roomType(Room.RoomType.DORMITORY).capacity(4).occupied(2).pricePerMonth(3500.0).status(Room.RoomStatus.AVAILABLE).build());
            roomRepository.save(Room.builder().roomNumber("C-301").roomType(Room.RoomType.DOUBLE).capacity(2).occupied(0).pricePerMonth(6000.0).status(Room.RoomStatus.MAINTENANCE).build());
        }
    }
}
