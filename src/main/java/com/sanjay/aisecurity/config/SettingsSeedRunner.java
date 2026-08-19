package com.sanjay.aisecurity.config;

import com.sanjay.aisecurity.service.ApplicationSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.sanjay.aisecurity.entity.User;
import com.sanjay.aisecurity.enums.Role;
import com.sanjay.aisecurity.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds default platform settings into the database on startup
 * if they have not been configured yet, and ensures an admin user exists.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettingsSeedRunner implements ApplicationRunner {

    private final ApplicationSettingsService settingsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Seeding default application settings...");
        settingsService.seedDefaultSettings();
        
        seedAdminUser();
    }

    private void seedAdminUser() {
        String adminEmail = "admin@codesentry.com";
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            log.info("Seeding default admin user: {}", adminEmail);
            User admin = User.builder()
                    .firstName("System")
                    .lastName("Administrator")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("AdminPassword123!"))
                    .role(Role.ADMIN)
                    .active(true)
                    .emailVerified(true)
                    .build();
            userRepository.save(admin);
            log.info("Default admin user created successfully.");
        }
    }
}
