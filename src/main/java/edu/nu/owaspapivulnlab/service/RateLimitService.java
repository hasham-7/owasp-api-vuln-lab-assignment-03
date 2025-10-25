package edu.nu.owaspapivulnlab.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitService {
    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastResetTimes = new ConcurrentHashMap<>();
    
    // Rate limit: 10 requests per minute per user
    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_SIZE_MS = 60 * 1000; // 1 minute

    public boolean isAllowed(String userId) {
        long currentTime = System.currentTimeMillis();
        String key = userId + "_" + (currentTime / WINDOW_SIZE_MS);
        
        AtomicInteger count = requestCounts.computeIfAbsent(key, k -> new AtomicInteger(0));
        long lastReset = lastResetTimes.computeIfAbsent(key, k -> currentTime);
        
        // Clean up old entries
        if (currentTime - lastReset > WINDOW_SIZE_MS) {
            requestCounts.remove(key);
            lastResetTimes.remove(key);
            return true;
        }
        
        return count.incrementAndGet() <= MAX_REQUESTS;
    }
}
