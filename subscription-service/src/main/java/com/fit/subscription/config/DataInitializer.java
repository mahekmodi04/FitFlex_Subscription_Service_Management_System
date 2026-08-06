package com.fit.subscription.config;

import com.fit.subscription.entity.User;
import com.fit.subscription.enums.UserRole;
import com.fit.subscription.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    //for production purpose so that admin password and email is not exposed publicily
    @Value("${admin.seed.email:admin@gmail.com}")
    private String adminEmail;

    @Value("${admin.seed.password:admin123}")
    private String adminPassword;

    @Bean
    CommandLineRunner createAdmin() {

        return args -> {

            if (userRepository.findByEmail(adminEmail).isEmpty()) {

                User admin = new User();

                admin.setName("Administrator");

                admin.setEmail(adminEmail);

                admin.setPassword(
                        passwordEncoder.encode(adminPassword)
                );

                admin.setRole(UserRole.ADMIN);

                userRepository.save(admin);

                System.out.println("Admin user created successfully.");
            }

        };

    }

}