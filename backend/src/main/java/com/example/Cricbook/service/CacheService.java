package com.example.Cricbook.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class CacheService {

    private static final int MAX_CACHE_ENTRIES = 1_000;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T getOrCompute(String key, long ttlSeconds, Supplier<T> supplier) {
        LocalDateTime now = LocalDateTime.now();
        CacheEntry entry = cache.compute(key, (cacheKey, existing) -> {
            if (existing != null && !existing.expiresAt().isBefore(now)) {
                return existing;
            }
            return new CacheEntry(supplier.get(), now.plusSeconds(ttlSeconds));
        });
        compactIfNeeded();
        return (T) entry.value();
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(LocalDateTime.now())) {
            cache.remove(key);
            return Optional.empty();
        }
        return Optional.of((T) entry.value());
    }

    public void clear() {
        cache.clear();
    }

    public void evictByPrefix(String prefix) {
        cache.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public int size() {
        evictExpiredEntries();
        return cache.size();
    }

    private void compactIfNeeded() {
        if (cache.size() < MAX_CACHE_ENTRIES) {
            return;
        }

        evictExpiredEntries();

        if (cache.size() >= MAX_CACHE_ENTRIES) {
            cache.keySet().stream().findFirst().ifPresent(cache::remove);
        }
    }

    private void evictExpiredEntries() {
        LocalDateTime now = LocalDateTime.now();
        cache.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private record CacheEntry(Object value, LocalDateTime expiresAt) {
    }
}
