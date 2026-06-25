package com.codesync.service;

import com.codesync.entity.RoomEntity;
import com.codesync.entity.RoomFileEntity;
import com.codesync.repository.RoomFileRepository;
import com.codesync.repository.RoomRepository;
import com.codesync.util.CompressionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomDataCacheService {

    public static final String DEFAULT_FILE_STRUCTURE = """
            {"name":"root","id":"root","type":"directory","children":[{"id":"index-js","type":"file","name":"index.js","content":"function sayHi() {\\n  console.log(\\"Hello world\\");\\n}\\n\\nsayHi()"}]}
            """;

    private final RoomRepository roomRepository;
    private final RoomFileRepository roomFileRepository;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private final Map<String, CachedRoomData> cache = new ConcurrentHashMap<>();

    @Value("${app.cache.flush-interval-ms}")
    private long flushIntervalMs;

    public CachedRoomData getOrLoad(String roomCode) {
        return cache.computeIfAbsent(roomCode, code -> {
            try {
                // Try loading from Redis first
                String redisFileStructure = stringRedisTemplate.opsForValue().get("room:data:fileStructure:" + code);
                String redisDrawingData = stringRedisTemplate.opsForValue().get("room:data:drawingData:" + code);

                if (redisFileStructure != null) {
                    log.debug("Loaded room {} file structure from Redis", code);
                    return CachedRoomData.builder()
                            .fileStructureJson(redisFileStructure)
                            .drawingDataJson(redisDrawingData)
                            .dirty(true) // Mark dirty so it eventually gets flushed to MySQL
                            .build();
                }
            } catch (Exception e) {
                log.warn("Failed to load room data from Redis for room {}: {}", code, e.getMessage());
            }

            // Fall back to database
            CachedRoomData dbData = loadFromDatabase(code);

            try {
                // Seed Redis cache
                if (dbData != null) {
                    if (dbData.getFileStructureJson() != null) {
                        stringRedisTemplate.opsForValue().set("room:data:fileStructure:" + code, dbData.getFileStructureJson());
                    }
                    if (dbData.getDrawingDataJson() != null) {
                        stringRedisTemplate.opsForValue().set("room:data:drawingData:" + code, dbData.getDrawingDataJson());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to seed Redis cache for room {}: {}", code, e.getMessage());
            }

            return dbData;
        });
    }

    public void updateFileStructure(String roomCode, Object fileStructure) {
        try {
            String json = objectMapper.writeValueAsString(fileStructure);
            CachedRoomData cached = getOrLoad(roomCode);
            cached.setFileStructureJson(json);
            cached.setDirty(true);

            // Update in Redis
            stringRedisTemplate.opsForValue().set("room:data:fileStructure:" + roomCode, json);
        } catch (Exception exception) {
            log.error("Failed to cache file structure for room {}", roomCode, exception);
        }
    }

    public void updateDrawingData(String roomCode, Object drawingData) {
        try {
            String json = objectMapper.writeValueAsString(drawingData);
            CachedRoomData cached = getOrLoad(roomCode);
            cached.setDrawingDataJson(json);
            cached.setDirty(true);

            // Update in Redis
            stringRedisTemplate.opsForValue().set("room:data:drawingData:" + roomCode, json);
        } catch (Exception exception) {
            log.error("Failed to cache drawing data for room {}", roomCode, exception);
        }
    }

    public void patchFileContent(String roomCode, String fileId, String newContent) {
        try {
            CachedRoomData cached = getOrLoad(roomCode);
            JsonNode root = objectMapper.readTree(cached.getFileStructureJson());
            if (patchFileNode(root, fileId, newContent)) {
                String json = objectMapper.writeValueAsString(root);
                cached.setFileStructureJson(json);
                cached.setDirty(true);

                // Update in Redis
                stringRedisTemplate.opsForValue().set("room:data:fileStructure:" + roomCode, json);
            }
        } catch (Exception exception) {
            log.error("Failed to patch file {} in room {}", fileId, roomCode, exception);
        }
    }

    private boolean patchFileNode(JsonNode node, String fileId, String newContent) {
        if (node.has("id") && fileId.equals(node.get("id").asText())) {
            if (node instanceof ObjectNode objectNode) {
                objectNode.put("content", newContent);
                return true;
            }
        }

        if (node.has("children") && node.get("children").isArray()) {
            for (JsonNode child : node.get("children")) {
                if (patchFileNode(child, fileId, newContent)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void seedCache(RoomEntity room) {
        String fileJson = room.getFileStructureJson();
        String drawingJson = room.getDrawingDataJson();

        cache.put(room.getRoomCode(), CachedRoomData.builder()
                .fileStructureJson(fileJson)
                .drawingDataJson(drawingJson)
                .dirty(false)
                .build());

        try {
            if (fileJson != null) {
                stringRedisTemplate.opsForValue().set("room:data:fileStructure:" + room.getRoomCode(), fileJson);
            }
            if (drawingJson != null) {
                stringRedisTemplate.opsForValue().set("room:data:drawingData:" + room.getRoomCode(), drawingJson);
            }
        } catch (Exception e) {
            log.warn("Failed to seed Redis cache for room {}: {}", room.getRoomCode(), e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${app.cache.flush-interval-ms}")
    @Transactional
    public void flushDirtyRooms() {
        cache.forEach((roomCode, cachedData) -> {
            if (!cachedData.isDirty()) {
                return;
            }
            persistRoom(roomCode, cachedData);
        });
    }

    @PreDestroy
    @Transactional
    public void flushAllOnShutdown() {
        cache.forEach(this::persistRoom);
    }

    private CachedRoomData loadFromDatabase(String roomCode) {
        Optional<RoomEntity> roomOptional = roomRepository.findByRoomCode(roomCode);
        if (roomOptional.isEmpty()) {
            return CachedRoomData.builder()
                    .fileStructureJson(DEFAULT_FILE_STRUCTURE)
                    .drawingDataJson(null)
                    .dirty(false)
                    .build();
        }

        RoomEntity room = roomOptional.get();
        List<RoomFileEntity> dbFiles = roomFileRepository.findByRoom(room);
        Map<String, String> fileContents = new HashMap<>();
        for (RoomFileEntity file : dbFiles) {
            String decompressed = CompressionUtils.decompress(file.getCompressedContent());
            fileContents.put(file.getFileId(), decompressed);
        }

        String reconstructedJson = DEFAULT_FILE_STRUCTURE;
        try {
            String strippedJson = room.getFileStructureJson();
            if (strippedJson != null && !strippedJson.isBlank()) {
                JsonNode root = objectMapper.readTree(strippedJson);
                populateFiles(root, fileContents);
                reconstructedJson = objectMapper.writeValueAsString(root);
            }
        } catch (Exception e) {
            log.error("Failed to reconstruct file structure for room {}", roomCode, e);
        }

        return CachedRoomData.builder()
                .fileStructureJson(reconstructedJson)
                .drawingDataJson(room.getDrawingDataJson())
                .dirty(false)
                .build();
    }

    private void populateFiles(JsonNode node, Map<String, String> fileContents) {
        if (node.has("type") && "file".equals(node.get("type").asText())) {
            String fileId = node.get("id").asText();
            String content = fileContents.getOrDefault(fileId, "");
            if (node instanceof ObjectNode objectNode) {
                objectNode.put("content", content);
            }
        }

        if (node.has("children") && node.get("children").isArray()) {
            for (JsonNode child : node.get("children")) {
                populateFiles(child, fileContents);
            }
        }
    }

    @Transactional
    public void persistRoom(String roomCode, CachedRoomData cachedData) {
        if (!cachedData.isDirty()) {
            return;
        }

        roomRepository.findByRoomCode(roomCode).ifPresent(room -> {
            try {
                String fullJson = cachedData.getFileStructureJson();
                JsonNode root = objectMapper.readTree(fullJson);

                List<RoomFileEntity> extractedFiles = new ArrayList<>();
                extractAndStripFiles(root, extractedFiles, room);

                // Update room entity with stripped JSON (without file content)
                room.setFileStructureJson(objectMapper.writeValueAsString(root));
                room.setDrawingDataJson(cachedData.getDrawingDataJson());
                roomRepository.save(room);

                // Delete old file contents and save new ones
                roomFileRepository.deleteByRoom(room);
                roomFileRepository.saveAll(extractedFiles);

                cachedData.setDirty(false);
                log.debug("Flushed room {} to MySQL (Stripped metadata + compressed file contents)", roomCode);
            } catch (Exception e) {
                log.error("Failed to persist room {}", roomCode, e);
            }
        });
    }

    private void extractAndStripFiles(JsonNode node, List<RoomFileEntity> files, RoomEntity room) {
        if (node.has("type") && "file".equals(node.get("type").asText())) {
            String fileId = node.get("id").asText();
            String content = node.has("content") ? node.get("content").asText() : "";

            byte[] compressed = CompressionUtils.compress(content);
            files.add(RoomFileEntity.builder()
                    .room(room)
                    .fileId(fileId)
                    .compressedContent(compressed)
                    .build());

            if (node instanceof ObjectNode objectNode) {
                objectNode.remove("content");
            }
        }

        if (node.has("children") && node.get("children").isArray()) {
            for (JsonNode child : node.get("children")) {
                extractAndStripFiles(child, files, room);
            }
        }
    }
}
