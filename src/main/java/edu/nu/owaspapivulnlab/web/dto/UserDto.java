package edu.nu.owaspapivulnlab.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// FIXED: DTO to control data exposure - never returns password, role, or admin flags
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String username;
    private String email;
    // Note: password, role, and isAdmin are intentionally excluded for security
}
