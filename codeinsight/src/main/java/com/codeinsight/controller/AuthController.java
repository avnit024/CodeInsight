package com.codeinsight.controller;

import com.codeinsight.model.User;
import com.codeinsight.repository.UserRepository;
import com.codeinsight.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "");

        if (username.length() < 3 || password.length() < 4) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username min 3 chars, password min 4 chars"));
        }
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already taken"));
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Registered successfully"));
    }

    private final Map<String, long[]> attempts = new ConcurrentHashMap<>(); // {failures, windowStart}

    private boolean blocked(String k) {
        long[] a = attempts.get(k);
        if (a == null) return false;
        if (System.currentTimeMillis() - a[1] > 60_000) { attempts.remove(k); return false; }
        return a[0] >= 5;
    }

    private void fail(String k) {
        attempts.compute(k, (x, a) -> {
            long now = System.currentTimeMillis();
            if (a == null || now - a[1] > 60_000) return new long[]{1, now};
            a[0]++;
            return a;
        });
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "");

        String key = username.toLowerCase();
        if (blocked(key)) {
            return ResponseEntity.status(429).body(Map.of("error", "Too many failed attempts. Try again in a minute."));
        }
        Optional<User> found = userRepository.findByUsername(username);
        if (found.isEmpty() || !encoder.matches(password, found.get().getPassword())) {
            fail(key);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }
        attempts.remove(key);
        User user = found.get();
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return ResponseEntity.ok(Map.of("token", token, "username", user.getUsername()));
    }
}
