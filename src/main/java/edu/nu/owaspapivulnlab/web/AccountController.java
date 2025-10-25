package edu.nu.owaspapivulnlab.web;

import edu.nu.owaspapivulnlab.model.Account;
import edu.nu.owaspapivulnlab.model.AppUser;
import edu.nu.owaspapivulnlab.repo.AccountRepository;
import edu.nu.owaspapivulnlab.repo.AppUserRepository;
import edu.nu.owaspapivulnlab.service.RateLimitService;
import edu.nu.owaspapivulnlab.web.dto.AccountDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accounts;
    private final AppUserRepository users;
    private final RateLimitService rateLimitService;

    public AccountController(AccountRepository accounts, AppUserRepository users, RateLimitService rateLimitService) {
        this.accounts = accounts;
        this.users = users;
        this.rateLimitService = rateLimitService;
    }

    // FIXED: Enforce ownership - users can only access their own accounts
    @GetMapping("/{id}/balance")
    public ResponseEntity<?> balance(@PathVariable Long id, Authentication auth) {
        AppUser currentUser = users.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "authentication required");
            return ResponseEntity.status(401).body(error);
        }

        Account account = accounts.findById(id).orElse(null);
        if (account == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "account not found");
            return ResponseEntity.status(404).body(error);
        }

        if (!account.getOwnerUserId().equals(currentUser.getId())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "access denied");
            return ResponseEntity.status(403).body(error);
        }

        return ResponseEntity.ok(account.getBalance());
    }

    // FIXED: Add rate limiting to transfer endpoint
    @PostMapping("/{id}/transfer")
    public ResponseEntity<?> transfer(@PathVariable Long id, @RequestParam Double amount, Authentication auth) {
        // FIXED: Rate limiting check
        if (!rateLimitService.isAllowed(auth.getName())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "rate limit exceeded");
            return ResponseEntity.status(429).body(error);
        }

        // FIXED: Input validation
        if (amount == null || amount <= 0) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "amount must be positive");
            return ResponseEntity.status(400).body(error);
        }

        if (amount > 10000) { // FIXED: Reasonable transfer limit
            Map<String, String> error = new HashMap<>();
            error.put("error", "transfer amount too large");
            return ResponseEntity.status(400).body(error);
        }

        AppUser currentUser = users.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "authentication required");
            return ResponseEntity.status(401).body(error);
        }

        Account account = accounts.findById(id).orElse(null);
        if (account == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "account not found");
            return ResponseEntity.status(404).body(error);
        }

        // FIXED: Enforce ownership
        if (!account.getOwnerUserId().equals(currentUser.getId())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "access denied");
            return ResponseEntity.status(403).body(error);
        }

        // FIXED: Check sufficient balance
        if (account.getBalance() < amount) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "insufficient funds");
            return ResponseEntity.status(400).body(error);
        }

        account.setBalance(account.getBalance() - amount);
        accounts.save(account);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("remaining", account.getBalance());
        return ResponseEntity.ok(response);
    }

    // FIXED: Return DTOs instead of full entities
    @GetMapping("/mine")
    public ResponseEntity<?> mine(Authentication auth) {
        AppUser me = users.findByUsername(auth != null ? auth.getName() : "anonymous").orElse(null);
        if (me == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "authentication required");
            return ResponseEntity.status(401).body(error);
        }

        List<AccountDto> myAccounts = accounts.findByOwnerUserId(me.getId()).stream()
                .map(account -> AccountDto.builder()
                        .id(account.getId())
                        .iban(account.getIban())
                        .balance(account.getBalance())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(myAccounts);
    }
}
