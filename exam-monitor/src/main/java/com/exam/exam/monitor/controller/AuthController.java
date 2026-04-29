package com.exam.exam.monitor.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exam.exam.monitor.model.User;
import com.exam.exam.monitor.repository.UserRepository;
import com.exam.exam.monitor.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final Map<String, Integer> otpStore = new HashMap<>();

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already registered");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        User existing = userRepository.findByEmail(user.getEmail()).orElse(null);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        if (!passwordEncoder.matches(user.getPassword(), existing.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password");
        }
        int otp = (int)(Math.random() * 900000) + 100000;
        otpStore.put(user.getEmail(), otp);

        // ✅ OTP prints in your terminal window
        System.out.println("========================================");
        System.out.println("OTP for " + user.getEmail() + " --> " + otp);
        System.out.println("========================================");

        return ResponseEntity.ok("OTP sent successfully");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> data) {
        String email = data.get("email");
        String otpStr = data.get("otp");

        if (email == null || otpStr == null) {
            return ResponseEntity.badRequest().body("Email and OTP required");
        }

        int enteredOtp;
        try {
            enteredOtp = Integer.parseInt(otpStr.trim());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid OTP format");
        }

        Integer storedOtp = otpStore.get(email);
        if (storedOtp == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("OTP expired or not found");
        }
        if (!storedOtp.equals(enteredOtp)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid OTP");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        otpStore.remove(email);

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("role", user.getRole());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/migrate-passwords")
    public ResponseEntity<?> migratePasswords() {
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            if (!user.getPassword().startsWith("$2a$") && !user.getPassword().startsWith("$2b$")) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
                userRepository.save(user);
            }
        }
        return ResponseEntity.ok("Migration complete");
    }
}