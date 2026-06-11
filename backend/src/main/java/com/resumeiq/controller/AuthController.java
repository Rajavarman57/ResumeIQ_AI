package com.resumeiq.controller;

import com.resumeiq.model.User;
import com.resumeiq.repository.UserRepository;
import com.resumeiq.security.JwtUtil;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder encoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String,String> req) {
        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.get("email"), req.get("password")));
            User user = userRepo.findByEmail(req.get("email")).orElseThrow();
            String token = jwtUtil.generateToken(user.getEmail());
            return ResponseEntity.ok(Map.of(
                "token", token,
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "role", user.getRole().name(),
                "id", user.getId()
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String,String> req) {
        if (userRepo.existsByEmail(req.get("email"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }
        User.Role role;
        try { role = User.Role.valueOf(req.getOrDefault("role","RECRUITER").toUpperCase()); }
        catch (Exception e) { role = User.Role.RECRUITER; }

        User user = User.builder()
                .email(req.get("email"))
                .password(encoder.encode(req.get("password")))
                .fullName(req.get("fullName"))
                .role(role)
                .active(true)
                .build();
        userRepo.save(user);
        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(org.springframework.security.core.Authentication auth) {
        User user = userRepo.findByEmail(auth.getName()).orElseThrow();
        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "fullName", user.getFullName(),
            "role", user.getRole().name()
        ));
    }
}
