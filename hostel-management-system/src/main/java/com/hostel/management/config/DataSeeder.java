package com.hostel.management.config;

import com.hostel.management.model.FeedbackQuestion;
import com.hostel.management.model.FeedbackQuestionOption;
import com.hostel.management.model.Role;
import com.hostel.management.model.Room;
import com.hostel.management.model.User;
import com.hostel.management.repository.FeedbackQuestionRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final FeedbackQuestionRepository feedbackQuestionRepository;
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

        // Seed initial structured feedback questionnaire if not already present
        if (feedbackQuestionRepository.count() == 0) {
            seedFeedbackQuestions();
        }
    }

    private void seedFeedbackQuestions() {
        List<String[]> standardRating = List.of(
                new String[]{"EXCELLENT", "Excellent"},
                new String[]{"GOOD", "Good"},
                new String[]{"AVERAGE", "Average"},
                new String[]{"POOR", "Poor"},
                new String[]{"VERY_POOR", "Very Poor"}
        );

        List<String[]> frequencyOptions = List.of(
                new String[]{"VERY_REGULARLY", "Very Regularly"},
                new String[]{"REGULARLY", "Regularly"},
                new String[]{"SOMETIMES", "Sometimes"},
                new String[]{"RARELY", "Rarely"},
                new String[]{"NEVER", "Never"}
        );

        List<String[]> reliabilityOptions = List.of(
                new String[]{"VERY_RELIABLE", "Very Reliable"},
                new String[]{"RELIABLE", "Reliable"},
                new String[]{"AVERAGE", "Average"},
                new String[]{"UNRELIABLE", "Unreliable"},
                new String[]{"VERY_UNRELIABLE", "Very Unreliable"}
        );

        List<String[]> satisfactionOptions = List.of(
                new String[]{"VERY_SATISFIED", "Very Satisfied"},
                new String[]{"SATISFIED", "Satisfied"},
                new String[]{"NEUTRAL", "Neutral"},
                new String[]{"DISSATISFIED", "Dissatisfied"},
                new String[]{"VERY_DISSATISFIED", "Very Dissatisfied"}
        );

        // 1. Cleanliness
        seedQuestion("How satisfied are you with the cleanliness of your hostel room?", "CLEANLINESS", 1, standardRating);
        seedQuestion("How satisfied are you with the cleanliness of common areas?", "CLEANLINESS", 2, standardRating);

        // 2. Washroom / Hygiene
        seedQuestion("How satisfied are you with the cleanliness of hostel washrooms?", "WASHROOM", 3, standardRating);
        seedQuestion("How regularly are the washrooms cleaned?", "WASHROOM", 4, frequencyOptions);

        // 3. Electricity
        seedQuestion("How satisfied are you with the availability of electricity in the hostel?", "ELECTRICITY", 5, standardRating);
        seedQuestion("How satisfied are you with the response to electricity-related problems?", "ELECTRICITY", 6, standardRating);

        // 4. Water
        seedQuestion("How satisfied are you with the availability of water?", "WATER", 7, standardRating);
        seedQuestion("How satisfied are you with the cleanliness and quality of drinking water facilities?", "WATER", 8, standardRating);

        // 5. Food / Mess
        seedQuestion("How satisfied are you with the quality of hostel/mess food?", "MESS", 9, standardRating);
        seedQuestion("How satisfied are you with the cleanliness of the mess/dining area?", "MESS", 10, standardRating);

        // 6. Internet / Wi-Fi
        seedQuestion("How satisfied are you with the availability of hostel Wi-Fi/internet?", "INTERNET", 11, standardRating);
        seedQuestion("How would you rate the reliability of hostel internet connectivity?", "INTERNET", 12, reliabilityOptions);

        // 7. Security
        seedQuestion("How satisfied are you with hostel security arrangements?", "SECURITY", 13, standardRating);

        // 8. Maintenance
        seedQuestion("How satisfied are you with the hostel's response to maintenance complaints?", "MAINTENANCE", 14, standardRating);

        // 9. Overall
        seedQuestion("Overall, how satisfied are you with the hostel facilities?", "OVERALL", 15, satisfactionOptions);
    }

    private void seedQuestion(String text, String category, int order, List<String[]> optionsList) {
        FeedbackQuestion q = FeedbackQuestion.builder()
                .questionText(text)
                .category(category)
                .displayOrder(order)
                .active(true)
                .build();

        List<FeedbackQuestionOption> options = new ArrayList<>();
        for (int i = 0; i < optionsList.size(); i++) {
            String[] opt = optionsList.get(i);
            options.add(FeedbackQuestionOption.builder()
                    .question(q)
                    .optionValue(opt[0])
                    .displayText(opt[1])
                    .displayOrder(i + 1)
                    .build());
        }
        q.setOptions(options);
        feedbackQuestionRepository.save(q);
    }
}
