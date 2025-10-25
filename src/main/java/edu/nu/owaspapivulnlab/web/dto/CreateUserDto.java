package edu.nu.owaspapivulnlab.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// FIXED: DTO for user creation to prevent mass assignment
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserDto {
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;
    
    @NotBlank
    @Size(min = 8, max = 100)
    private String password;
    
    @Email
    private String email;
    
    // Note: role and isAdmin are intentionally excluded - server controls these
}
