package edu.nu.owaspapivulnlab.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// FIXED: DTO for account data exposure control
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDto {
    private Long id;
    private String iban;
    private Double balance;
    // Note: ownerUserId is intentionally excluded for security
}
