package com.hostel.management.config;

import com.hostel.management.model.Role;
import com.hostel.management.model.Room;
import com.hostel.management.model.User;
import com.hostel.management.repository.RoomRepository;
import com.hostel.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.username:admin}")
    private String adminUsername;

    @Value("${app.seed.admin.email:admin@hostel.com}")
    private String adminEmail;

    @Value("${app.seed.admin.password:Admin@123456}")
    private String adminPassword;

    @Value("${app.seed.warden.username:warden}")
    private String wardenUsername;

    @Value("${app.seed.warden.email:warden@hostel.com}")
    private String wardenEmail;

    @Value("${app.seed.warden.password:Warden@123456}")
    private String wardenPassword;

    @Override
    public void run(String... args) {
        String normalizedAdminUsername = adminUsername.trim().toLowerCase(Locale.ROOT);
        String normalizedAdminEmail = adminEmail.trim().toLowerCase(Locale.ROOT);

        // Creates the single default administrator account if it does not already exist
        if (!userRepository.existsByUsername(normalizedAdminUsername) && !userRepository.existsByEmail(normalizedAdminEmail)) {
            if (adminPassword == null || adminPassword.isBlank()) {
                throw new IllegalStateException("Admin seed password must be configured via app.seed.admin.password or HOSTEL_ADMIN_PASSWORD.");
            }
            User admin = User.builder()
                    .fullName("System Administrator")
                    .username(normalizedAdminUsername)
                    .email(normalizedAdminEmail)
                    .mobileNumber("9999999999")
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .gender("Other")
                    .address("Hostel Administrative Office, Block A")
                    .status("ACTIVE")
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.info("Initialized default administrator account: {}", normalizedAdminUsername);
        }

        String normalizedWardenUsername = wardenUsername.trim().toLowerCase(Locale.ROOT);
        String normalizedWardenEmail = wardenEmail.trim().toLowerCase(Locale.ROOT);

        // Creates the single default hostel warden account if it does not already exist
        if (!userRepository.existsByUsername(normalizedWardenUsername) && !userRepository.existsByEmail(normalizedWardenEmail)) {
            if (wardenPassword == null || wardenPassword.isBlank()) {
                throw new IllegalStateException("Warden seed password must be configured via app.seed.warden.password or HOSTEL_WARDEN_PASSWORD.");
            }
            User warden = User.builder()
                    .fullName("Chief Hostel Warden")
                    .username(normalizedWardenUsername)
                    .email(normalizedWardenEmail)
                    .mobileNumber("9888888888")
                    .password(passwordEncoder.encode(wardenPassword))
                    .role(Role.WARDEN)
                    .gender("Other")
                    .address("Hostel Warden Office, Ground Floor")
                    .status("ACTIVE")
                    .enabled(true)
                    .build();
            userRepository.save(warden);
            log.info("Initialized default hostel warden account: {}", normalizedWardenUsername);
        }

        // Seed initial room inventory if database is empty
        if (roomRepository.count() == 0) {
            roomRepository.save(Room.builder().roomNumber("A-101").roomType(Room.RoomType.SINGLE).capacity(1).occupied(0).pricePerMonth(BigDecimal.valueOf(8000.00)).status(Room.RoomStatus.AVAILABLE).build());
            roomRepository.save(Room.builder().roomNumber("A-102").roomType(Room.RoomType.DOUBLE).capacity(2).occupied(0).pricePerMonth(BigDecimal.valueOf(5500.00)).status(Room.RoomStatus.AVAILABLE).build());
            roomRepository.save(Room.builder().roomNumber("A-103").roomType(Room.RoomType.TRIPLE).capacity(3).occupied(0).pricePerMonth(BigDecimal.valueOf(4200.00)).status(Room.RoomStatus.AVAILABLE).build());
            roomRepository.save(Room.builder().roomNumber("B-201").roomType(Room.RoomType.DOUBLE).capacity(2).occupied(0).pricePerMonth(BigDecimal.valueOf(5500.00)).status(Room.RoomStatus.AVAILABLE).build());
            roomRepository.save(Room.builder().roomNumber("B-202").roomType(Room.RoomType.SINGLE).capacity(1).occupied(0).pricePerMonth(BigDecimal.valueOf(8500.00)).status(Room.RoomStatus.AVAILABLE).build());
            roomRepository.save(Room.builder().roomNumber("B-203").roomType(Room.RoomType.DORMITORY).capacity(4).occupied(0).pricePerMonth(BigDecimal.valueOf(3500.00)).status(Room.RoomStatus.AVAILABLE).build());
            roomRepository.save(Room.builder().roomNumber("C-301").roomType(Room.RoomType.DOUBLE).capacity(2).occupied(0).pricePerMonth(BigDecimal.valueOf(6000.00)).status(Room.RoomStatus.MAINTENANCE).build());
        }
    }
}
