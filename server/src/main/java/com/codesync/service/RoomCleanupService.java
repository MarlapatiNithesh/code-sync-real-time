package com.codesync.service;

import com.codesync.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomCleanupService {

    private final RoomRepository roomRepository;

    // Run every 15 minutes (900,000 ms)
    @Scheduled(fixedDelay = 900000)
    @Transactional
    public void cleanupEmptyRooms() {
        Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);
        log.info("Starting cleanup of empty rooms created before: {}", cutoff);
        
        try {
            int deletedCount = roomRepository.deleteEmptyRooms(cutoff);
            if (deletedCount > 0) {
                log.info("Successfully cleaned up {} empty rooms from database", deletedCount);
            }
        } catch (Exception e) {
            log.error("Failed to run scheduled empty room cleanup", e);
        }
    }
}
