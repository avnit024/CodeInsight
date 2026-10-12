package com.codeinsight.controller;

import com.codeinsight.model.User;
import com.codeinsight.repository.ProblemRepository;
import com.codeinsight.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Account settings: change password, delete account. Protected by the JWT interceptor. */
@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final UserRepository users;
    private final ProblemRepository problems;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AccountController(UserRepository users, ProblemRepository problems) {
        this.users = users;
        this.problems = problems;
    }

    @PostMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body, HttpServletRequest req) {
        User user = users.findById((Long) req.getAttribute("userId")).orElseThrow();
        if (user.getUsername().startsWith("github_"))
            return ResponseEntity.badRequest().body(Map.of("error", "GitHub accounts have no password"));
        String next = body.getOrDefault("newPassword", "");
        if (!encoder.matches(body.getOrDefault("currentPassword", ""), user.getPassword()))
            return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));
        if (next.length() < 4)
            return ResponseEntity.badRequest().body(Map.of("error", "New password must be at least 4 characters"));
        user.setPassword(encoder.encode(next));
        users.save(user);
        return ResponseEntity.ok(Map.of("message", "Password updated"));
    }

    /** Permanently deletes the account and all its problems. The client must send the username as confirmation. */
    @PostMapping("/delete")
    @Transactional
    public ResponseEntity<?> delete(@RequestBody Map<String, String> body, HttpServletRequest req) {
        User user = users.findById((Long) req.getAttribute("userId")).orElseThrow();
        if (!user.getUsername().equals(body.get("confirm")))
            return ResponseEntity.badRequest().body(Map.of("error", "Type your username exactly to confirm"));
        problems.deleteAll(problems.findByUserId(user.getId()));
        users.delete(user);
        return ResponseEntity.ok(Map.of("message", "Account deleted"));
    }
}
