package com.resumeiq.config;

import com.resumeiq.model.User;
import com.resumeiq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AppConfig {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
            return new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPassword(),
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );
        };
    }

    @Bean
    public CommandLineRunner seedData() {
        return args -> {
            if (!userRepository.existsByEmail("admin@resumeiq.local")) {
                userRepository.save(User.builder()
                        .email("admin@resumeiq.local")
                        .password(passwordEncoder.encode("admin123"))
                        .fullName("System Admin")
                        .role(User.Role.ADMIN)
                        .active(true)
                        .build());
            }
            if (!userRepository.existsByEmail("recruiter@resumeiq.local")) {
                userRepository.save(User.builder()
                        .email("recruiter@resumeiq.local")
                        .password(passwordEncoder.encode("recruit123"))
                        .fullName("HR Recruiter")
                        .role(User.Role.RECRUITER)
                        .active(true)
                        .build());
            }
        };
    }
}
