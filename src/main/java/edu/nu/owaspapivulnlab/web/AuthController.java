package edu.nu.owaspapivulnlab.web;

import edu.nu.owaspapivulnlab.model.AppUser;
import edu.nu.owaspapivulnlab.repo.AppUserRepository;
import edu.nu.owaspapivulnlab.service.JwtService;
import edu.nu.owaspapivulnlab.service.PasswordService;
import edu.nu.owaspapivulnlab.web.dto.CreateUserDto;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AppUserRepository users;
    private final JwtService jwt;
    private final PasswordService passwordService;

    public AuthController(AppUserRepository users, JwtService jwt, PasswordService passwordService) {
        this.users = users;
        this.jwt = jwt;
        this.passwordService = passwordService;
    }

    public static class LoginReq {
        @NotBlank
        private String username;
        @NotBlank
        private String password;

        public LoginReq() {}

        public LoginReq(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String username() { return username; }
        public String password() { return password; }

        public void setUsername(String username) { this.username = username; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class TokenRes {
        private String token;

        public TokenRes() {}

        public TokenRes(String token) {
            this.token = token;
        }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }

    // FIXED: Secure login with BCrypt password verification
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReq req) {
        AppUser user = users.findByUsername(req.username()).orElse(null);
        if (user != null && passwordService.matches(req.password(), user.getPassword())) {
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", user.getRole());
            claims.put("isAdmin", user.isAdmin());
            String token = jwt.issue(user.getUsername(), claims);
            return ResponseEntity.ok(new TokenRes(token));
        }
        Map<String, String> error = new HashMap<>();
        error.put("error", "invalid credentials");
        return ResponseEntity.status(401).body(error);
    }

    // FIXED: Added secure signup endpoint
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody CreateUserDto req) {
        if (users.findByUsername(req.getUsername()).isPresent()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "username already exists");
            return ResponseEntity.status(400).body(error);
        }

        AppUser newUser = AppUser.builder()
                .username(req.getUsername())
                .password(passwordService.encodePassword(req.getPassword()))
                .email(req.getEmail())
                .role("USER")  // FIXED: Server controls role assignment
                .isAdmin(false)  // FIXED: Server controls admin status
                .build();

        users.save(newUser);

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", newUser.getRole());
        claims.put("isAdmin", newUser.isAdmin());
        String token = jwt.issue(newUser.getUsername(), claims);
        return ResponseEntity.ok(new TokenRes(token));
    }
}
