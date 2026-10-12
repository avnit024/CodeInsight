package com.codeinsight.controller;

import com.codeinsight.model.User;
import com.codeinsight.repository.UserRepository;
import com.codeinsight.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** "Continue with GitHub" (OAuth 2.0 authorization code flow). Enabled only when client id/secret are set. */
@RestController
@RequestMapping("/api/auth")
public class OAuthController {
    private final RestTemplate rest = new RestTemplate();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UserRepository users;
    private final JwtUtil jwt;
    private final String clientId, clientSecret;

    public OAuthController(UserRepository users, JwtUtil jwt,
                           @Value("${app.oauth.github.client-id:}") String clientId,
                           @Value("${app.oauth.github.client-secret:}") String clientSecret) {
        this.users = users;
        this.jwt = jwt;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    private boolean enabled() { return !clientId.isBlank() && !clientSecret.isBlank(); }

    @GetMapping("/providers")
    public Map<String, Boolean> providers() { return Map.of("github", enabled()); }

    /** Step 1: send the browser to GitHub, with a random state stored in a cookie (CSRF protection). */
    @GetMapping("/github")
    public void start(HttpServletResponse res) throws IOException {
        if (!enabled()) { res.sendError(503, "GitHub login is not configured"); return; }
        String state = UUID.randomUUID().toString();
        Cookie c = new Cookie("oauth_state", state);
        c.setHttpOnly(true); c.setPath("/"); c.setMaxAge(300);
        res.addCookie(c);
        res.sendRedirect("https://github.com/login/oauth/authorize?client_id=" + clientId + "&scope=read:user&state=" + state);
    }

    /** Step 2: GitHub redirects back with a code; exchange it for a token, find/create the user, issue our JWT. */
    @GetMapping("/github/callback")
    public void callback(@RequestParam String code, @RequestParam String state,
                         @CookieValue(value = "oauth_state", required = false) String saved,
                         HttpServletResponse res) throws IOException {
        if (!enabled() || saved == null || !saved.equals(state)) { res.sendError(400, "Invalid OAuth state"); return; }
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            h.setAccept(List.of(MediaType.APPLICATION_JSON));
            Map<?, ?> t = rest.postForObject("https://github.com/login/oauth/access_token",
                    new HttpEntity<>(Map.of("client_id", clientId, "client_secret", clientSecret, "code", code), h), Map.class);
            HttpHeaders g = new HttpHeaders();
            g.setBearerAuth((String) t.get("access_token"));
            g.set("User-Agent", "CodeInsight");
            Map<?, ?> u = rest.exchange("https://api.github.com/user", HttpMethod.GET, new HttpEntity<>(g), Map.class).getBody();

            String name = "github_" + u.get("login");
            User user = users.findByUsername(name).orElseGet(() -> {
                User n = new User();
                n.setUsername(name);
                n.setPassword(encoder.encode(UUID.randomUUID().toString())); // no password login for OAuth accounts
                return users.save(n);
            });
            res.sendRedirect("/login.html#token=" + jwt.generateToken(user.getId(), user.getUsername())
                    + "&user=" + URLEncoder.encode(name, StandardCharsets.UTF_8));
        } catch (Exception e) {
            res.sendError(502, "GitHub login failed");
        }
    }
}
