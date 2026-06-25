package com.codesync.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FileLockService {

    // Key format of roomLocks: roomCode -> Map of (fileId + ":" + lineNumber ->
    // FileLockInfo)
    private final Map<String, Map<String, FileLockInfo>> roomLocks = new ConcurrentHashMap<>();

    public Optional<FileLockInfo> getLock(String roomCode, String fileId, int lineNumber) {
        Map<String, FileLockInfo> fileLocks = roomLocks.get(roomCode);
        if (fileLocks == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(fileLocks.get(fileId + ":" + lineNumber));
    }

    // lock is given based on the roomcode , file id and line number
    public synchronized LockResult acquireLock(String roomCode, String fileId, int lineNumber, String socketId,
            String username) {
        Map<String, FileLockInfo> fileLocks = roomLocks.computeIfAbsent(roomCode, key -> new ConcurrentHashMap<>());
        String lockKey = fileId + ":" + lineNumber;
        FileLockInfo existingLock = fileLocks.get(lockKey);

        if (existingLock == null || existingLock.getSocketId().equals(socketId)) {
            FileLockInfo lockInfo = FileLockInfo.builder()
                    .fileId(fileId)
                    .lineNumber(lineNumber)
                    .socketId(socketId)
                    .username(username)
                    .build();
            fileLocks.put(lockKey, lockInfo);
            return LockResult.granted(lockInfo);
        }
        // lock is denied
        return LockResult.denied(existingLock);
    }

    // lock is released based on the roomcode , file id and line number
    public synchronized Optional<FileLockInfo> releaseLock(String roomCode, String fileId, int lineNumber,
            String socketId) {
        Map<String, FileLockInfo> fileLocks = roomLocks.get(roomCode);
        if (fileLocks == null) {
            return Optional.empty();
        }

        String lockKey = fileId + ":" + lineNumber;
        FileLockInfo existingLock = fileLocks.get(lockKey);
        if (existingLock == null) {
            return Optional.empty();
        }

        if (!existingLock.getSocketId().equals(socketId)) {
            return Optional.empty();
        }

        fileLocks.remove(lockKey);
        if (fileLocks.isEmpty()) {
            roomLocks.remove(roomCode);
        }
        return Optional.of(existingLock);
    }

    // release all the locks for a socket
    public synchronized void releaseAllForSocket(String roomCode, String socketId) {
        Map<String, FileLockInfo> fileLocks = roomLocks.get(roomCode);
        if (fileLocks == null) {
            return;
        }

        fileLocks.entrySet().removeIf(entry -> entry.getValue().getSocketId().equals(socketId));
        if (fileLocks.isEmpty()) {
            roomLocks.remove(roomCode);
        }
    }

    // get the lock snapshot for a room
    public Map<String, FileLockInfo> getRoomLockSnapshot(String roomCode) {
        Map<String, FileLockInfo> fileLocks = roomLocks.get(roomCode);
        if (fileLocks == null) {
            return Map.of();
        }
        return new HashMap<>(fileLocks);
    }

    public boolean canEdit(String roomCode, String fileId, int lineNumber, String socketId) {
        return getLock(roomCode, fileId, lineNumber)
                .map(lock -> lock.getSocketId().equals(socketId))
                .orElse(true);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileLockInfo {
        private String fileId;
        private Integer lineNumber;
        private String socketId;
        private String username;
    }

    @Data
    @AllArgsConstructor
    public static class LockResult {
        private boolean granted;
        private FileLockInfo lockInfo;

        public static LockResult granted(FileLockInfo lockInfo) {
            return new LockResult(true, lockInfo);
        }

        public static LockResult denied(FileLockInfo lockInfo) {
            return new LockResult(false, lockInfo);
        }
    }
}
