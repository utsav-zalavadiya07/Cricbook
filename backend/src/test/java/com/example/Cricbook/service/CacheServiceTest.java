package com.example.Cricbook.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CacheServiceTest {

    @Test
    void getOrComputeRunsSupplierOnceForConcurrentSameKey() throws Exception {
        CacheService cacheService = new CacheService();
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(8);

        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return cacheService.getOrCompute("same-search", 60, () -> {
                        calls.incrementAndGet();
                        return "cached";
                    });
                }));
            }

            ready.await();
            start.countDown();

            for (Future<String> future : futures) {
                assertEquals("cached", future.get());
            }
            assertEquals(1, calls.get());
        } finally {
            executor.shutdownNow();
        }
    }
}
