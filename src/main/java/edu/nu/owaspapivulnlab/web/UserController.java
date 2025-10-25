package edu.nu.owaspapivulnlab.web;

import edu.nu.owaspapivulnlab.model.AppUser;
import edu.nu.owaspapivulnlab.repo.AppUserRepository;
import edu.nu.owaspapivulnlab.web.dto.CreateUserDto;
import edu.nu.owaspapivulnlab.web.dto.UserDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AppUserRepository users;

    public UserController(AppUserRepository users) {
        this.users = users;
    }

    // FIXED: Enforce ownership - users can only access their own data
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id, Authentication auth) {
        AppUser currentUser = users.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null || !currentUser.getId().equals(id)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "access denied");
            return ResponseEntity.status(403).body(error);
        }
        
        UserDto userDto = UserDto.builder()
                .id(currentUser.getId())
                .username(currentUser.getUsername())
                .email(currentUser.getEmail())
                .build();
        return ResponseEntity.ok(userDto);
    }

    // FIXED: Prevent mass assignment with explicit DTO
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateUserDto body) {
        if (users.findByUsername(body.getUsername()).isPresent()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "username already exists");
            return ResponseEntity.status(400).body(error);
        }

        AppUser newUser = AppUser.builder()
                .username(body.getUsername())
                .password(body.getPassword()) // Note: This should be hashed by the service layer
                .email(body.getEmail())
                .role("USER")  // FIXED: Server controls role assignment
                .isAdmin(false)  // FIXED: Server controls admin status
                .build();

        AppUser savedUser = users.save(newUser);
        
        UserDto userDto = UserDto.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .build();
        return ResponseEntity.status(201).body(userDto);
    }

    // FIXED: Improved search with input validation
    @GetMapping("/search")
    public List<UserDto> search(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) {
            return List.of(); // FIXED: Require minimum search length
        }
        
        return users.search(q.trim()).stream()
                .map(user -> UserDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .build())
                .collect(Collectors.toList());
    }

    // FIXED: Return DTOs instead of full entities
    @GetMapping
    public List<UserDto> list() {
        return users.findAll().stream()
                .map(user -> UserDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .build())
                .collect(Collectors.toList());
    }

    // FIXED: Require admin role for user deletion
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        users.deleteById(id);
        Map<String, String> response = new HashMap<>();
        response.put("status", "deleted");
        return ResponseEntity.ok(response);
    }
}
