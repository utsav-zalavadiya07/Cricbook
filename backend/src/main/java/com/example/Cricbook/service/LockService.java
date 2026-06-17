package com.example.Cricbook.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class LockService {

    private final ConcurrentHashMap<String, Long> locks = new ConcurrentHashMap<>();

    public boolean acquireLock(String lockKey, long timeoutInSeconds) {
        long expireAt = System.currentTimeMillis() + (timeoutInSeconds * 1000);
        
        return locks.compute(lockKey, (key, currentExpire) -> {
            if (currentExpire == null || currentExpire < System.currentTimeMillis()) {
                return expireAt;
            }
            return currentExpire;
        }) == expireAt;
    }

    public void releaseLock(String lockKey) {
        locks.remove(lockKey);
    }
}
