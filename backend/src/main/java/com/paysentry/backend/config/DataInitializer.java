package com.paysentry.backend.config;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.paysentry.backend.entity.Role;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.monitoring.BankAccountService;
import com.paysentry.backend.repository.UserRepository;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedAdminUser(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        BankAccountService bankAccountService,
        @Value("${paysentry.app.admin-email}") String adminEmail,
        @Value("${paysentry.app.admin-password}") String adminPassword,
        @Value("${paysentry.app.admin-name}") String adminName
    ) {
        return args -> {
            String normalizedEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
            User adminUser = userRepository.findByEmailIgnoreCase(normalizedEmail).orElseGet(User::new);
            adminUser.setFullName(adminName.trim());
            adminUser.setEmail(normalizedEmail);
            adminUser.setPassword(passwordEncoder.encode(adminPassword));
            adminUser.setRole(Role.ADMIN);
            adminUser.setActive(true);
            if (adminUser.getTrustedDevices() == null) {
                adminUser.setTrustedDevices("");
            }
            if (adminUser.getTrustedLocations() == null) {
                adminUser.setTrustedLocations("");
            }
            User savedUser = userRepository.save(adminUser);
            bankAccountService.createForUser(savedUser);
        };
    }
}
