package com.codesync.socket;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.codesync.model.User;
import com.codesync.model.UserConnectionStatus;
import com.codesync.service.CachedRoomData;
import com.codesync.service.FileLockService;
import com.codesync.service.RoomDataCacheService;
import com.codesync.service.RoomService;
import com.codesync.service.UserSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SocketEventListener {

    private final UserSessionService userSessionService;
    private final RoomService roomService;
    private final RoomDataCacheService roomDataCacheService;
    private final FileLockService fileLockService;
    private final ObjectMapper objectMapper;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    private SocketIOServer server;

    public void registerHandlers(SocketIOServer socketIOServer) {
        this.server = socketIOServer;

        server.addConnectListener(client ->
                System.out.println("Socket connected: " + client.getSessionId())
        );

        server.addDisconnectListener(client -> {
            Optional<User> userOptional = userSessionService.getUserBySocketId(sessionId(client));
            if (userOptional.isEmpty()) {
                return;
            }

            User user = userOptional.get();
            fileLockService.releaseAllForSocket(user.getRoomId(), sessionId(client));
            broadcastLockState(user.getRoomId(), client);
            broadcastToRoom(user.getRoomId(), client, SocketEvent.USER_DISCONNECTED, Map.of("user", user));
            userSessionService.removeUser(sessionId(client));
            roomDataCacheService.flushDirtyRooms();
        });

        server.addEventListener(SocketEvent.JOIN_REQUEST, Map.class, this::handleJoinRequest);
        server.addEventListener(SocketEvent.SYNC_FILE_STRUCTURE, Map.class, this::handleSyncFileStructure);
        server.addEventListener(SocketEvent.ROOM_SNAPSHOT, Map.class, this::handleRoomSnapshot);
        server.addEventListener(SocketEvent.FILE_LOCK_REQUEST, Map.class, this::handleFileLockRequest);
        server.addEventListener(SocketEvent.FILE_LOCK_RELEASE, Map.class, this::handleFileLockRelease);
        server.addEventListener(SocketEvent.DIRECTORY_CREATED, Map.class, (client, data, ack) ->
                broadcastRoomEvent(client, SocketEvent.DIRECTORY_CREATED, data));
        server.addEventListener(SocketEvent.DIRECTORY_UPDATED, Map.class, (client, data, ack) ->
                broadcastRoomEvent(client, SocketEvent.DIRECTORY_UPDATED, data));
        server.addEventListener(SocketEvent.DIRECTORY_RENAMED, Map.class, (client, data, ack) ->
                broadcastRoomEvent(client, SocketEvent.DIRECTORY_RENAMED, data));
        server.addEventListener(SocketEvent.DIRECTORY_DELETED, Map.class, (client, data, ack) ->
                broadcastRoomEvent(client, SocketEvent.DIRECTORY_DELETED, data));
        server.addEventListener(SocketEvent.FILE_CREATED, Map.class, (client, data, ack) ->
                broadcastRoomEvent(client, SocketEvent.FILE_CREATED, data));
        server.addEventListener(SocketEvent.FILE_UPDATED, Map.class, this::handleFileUpdated);
        server.addEventListener(SocketEvent.FILE_RENAMED, Map.class, (client, data, ack) ->
                broadcastRoomEvent(client, SocketEvent.FILE_RENAMED, data));
        server.addEventListener(SocketEvent.FILE_DELETED, Map.class, (client, data, ack) ->
                broadcastRoomEvent(client, SocketEvent.FILE_DELETED, data));
        server.addEventListener(SocketEvent.USER_OFFLINE, Map.class, this::handleUserOffline);
        server.addEventListener(SocketEvent.USER_ONLINE, Map.class, this::handleUserOnline);
        server.addEventListener(SocketEvent.SEND_MESSAGE, Map.class, this::handleSendMessage);
        server.addEventListener(SocketEvent.TYPING_START, Map.class, this::handleTypingStart);
        server.addEventListener(SocketEvent.TYPING_PAUSE, Map.class, this::handleTypingPause);
        server.addEventListener(SocketEvent.REQUEST_DRAWING, Map.class, this::handleRequestDrawing);
        server.addEventListener(SocketEvent.SYNC_DRAWING, Map.class, this::handleSyncDrawing);
        server.addEventListener(SocketEvent.DRAWING_UPDATE, Map.class, this::handleDrawingUpdate);
    }

    private void handleJoinRequest(SocketIOClient client, Map<String, Object> data, AckRequest ackRequest) {
        String roomId = stringValue(data.get("roomId"));
        String username = stringValue(data.get("username"));

        if (roomId == null || username == null) {
            return;
        }

        if (!roomService.roomExists(roomId)) {
            client.sendEvent(SocketEvent.ROOM_NOT_FOUND);
            return;
        }

        if (userSessionService.isUsernameTaken(roomId, username)) {
            client.sendEvent(SocketEvent.USERNAME_EXISTS);
            return;
        }

        User user = userSessionService.createUser(username, roomId, sessionId(client));
        client.joinRoom(roomId);

        // Track participant in Redis and clear cache for updates
        stringRedisTemplate.opsForSet().add("room:participants:" + roomId, username);
        roomService.evictRoomCaches();

        broadcastToRoom(roomId, client, SocketEvent.USER_JOINED, Map.of("user", user));

        CachedRoomData cachedRoomData = roomDataCacheService.getOrLoad(roomId);
        List<User> users = userSessionService.snapshotUsersInRoom(roomId);

        // Retrieve chat history from Redis
        String historyKey = "room:chat:history:" + roomId;
        List<String> rawMessages = stringRedisTemplate.opsForList().range(historyKey, 0, -1);
        java.util.List<Object> chatHistory = new java.util.ArrayList<>();
        if (rawMessages != null) {
            for (String rawMsg : rawMessages) {
                try {
                    chatHistory.add(objectMapper.readValue(rawMsg, Object.class));
                } catch (Exception e) {
                    // Ignore malformed messages
                }
            }
        }

        Map<String, Object> joinPayload = new HashMap<>();
        joinPayload.put("user", user);
        joinPayload.put("users", users);
        joinPayload.put("fileStructure", parseJson(cachedRoomData.getFileStructureJson()));
        joinPayload.put("drawingData", parseJson(cachedRoomData.getDrawingDataJson()));
        joinPayload.put("messages", chatHistory);
        joinPayload.put("locks", fileLockService.getRoomLockSnapshot(roomId));
        client.sendEvent(SocketEvent.JOIN_ACCEPTED, joinPayload);
    }

    private void handleFileUpdated(SocketIOClient client, Map<String, Object> data, AckRequest ackRequest) {
        String fileId = stringValue(data.get("fileId"));
        String newContent = stringValue(data.get("newContent"));
        int lineNumber = numberValue(data.get("lineNumber"));
        Optional<String> roomIdOptional = userSessionService.getRoomId(sessionId(client));

        if (fileId == null || roomIdOptional.isEmpty()) {
            return;
        }

        String roomId = roomIdOptional.get();
        if (lineNumber > 0 && !fileLockService.canEdit(roomId, fileId, lineNumber, sessionId(client))) {
            FileLockService.FileLockInfo lockInfo = fileLockService.getLock(roomId, fileId, lineNumber).orElse(null);
            client.sendEvent(SocketEvent.FILE_LOCK_DENIED, Map.of("fileId", fileId, "lineNumber", lineNumber, "lock", lockInfo));
            return;
        }

        roomDataCacheService.patchFileContent(roomId, fileId, newContent);
        broadcastRoomEvent(client, SocketEvent.FILE_UPDATED, data);
    }

    private void handleRoomSnapshot(SocketIOClient client, Map<String, Object> data, AckRequest ackRequest) {
        userSessionService.getRoomId(sessionId(client)).ifPresent(roomId -> {
            roomDataCacheService.updateFileStructure(roomId, data.get("fileStructure"));
            if (data.get("drawingData") != null) {
                roomDataCacheService.updateDrawingData(roomId, data.get("drawingData"));
            }
        });
    }

    private void handleFileLockRequest(SocketIOClient client, Map<String, Object> data, AckRequest ackRequest) {
        String fileId = stringValue(data.get("fileId"));
        int lineNumber = numberValue(data.get("lineNumber"));
        Optional<User> userOptional = userSessionService.getUserBySocketId(sessionId(client));
        Optional<String> roomIdOptional = userSessionService.getRoomId(sessionId(client));

        if (fileId == null || userOptional.isEmpty() || roomIdOptional.isEmpty() || lineNumber <= 0) {
            return;
        }

        User user = userOptional.get();
        String roomId = roomIdOptional.get();
        FileLockService.LockResult result = fileLockService.acquireLock(
                roomId,
                fileId,
                lineNumber,
                sessionId(client),
                user.getUsername()
        );

        if (result.isGranted()) {
            client.sendEvent(SocketEvent.FILE_LOCK_GRANTED, Map.of("fileId", fileId, "lineNumber", lineNumber, "lock", result.getLockInfo()));
            broadcastLockState(roomId, client);
        } else {
            client.sendEvent(SocketEvent.FILE_LOCK_DENIED, Map.of("fileId", fileId, "lineNumber", lineNumber, "lock", result.getLockInfo()));
        }
    }

    private void handleFileLockRelease(SocketIOClient client, Map<String, Object> data, AckRequest ackRequest) {
        String fileId = stringValue(data.get("fileId"));
        int lineNumber = numberValue(data.get("lineNumber"));
        Optional<String> roomIdOptional = userSessionService.getRoomId(sessionId(client));

        if (fileId == null || roomIdOptional.isEmpty() || lineNumber <= 0) {
            return;
        }

        String roomId = roomIdOptional.get();
        fileLockService.releaseLock(roomId, fileId, lineNumber, sessionId(client));
        broadcastLockState(roomId, client);
    }

    private void handleSyncFileStructure(SocketIOClient client, Map<String, Object> data, AckRequest ackRequest) {
        String targetSocketId = stringValue(data.get("socketId"));
        if (targetSocketId == null) {
            return;
        }

        SocketIOClient targetClient = findClientBySessionId(targetSocketId);
        if (targetClient == null) {
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("fileStructure", data.get("fileStructure"));
        payload.put("openFiles", data.get("openFiles"));
        payload.put("activeFile", data.get("activeFile"));
        targetClient.sendEvent(SocketEvent.SYNC_FILE_STRUCTURE, payload);
    }

    private void handleUserOffline(SocketIOClient client, Map<String, Object> data, AckRequest ackRequest) {
        String targetSocketId = stringValue(data.get("socketId"));
        if (targetSocketId == null) {
            return;
        }

        userSessionService.updateUser(targetSocketId, user -> {
            user.setStatus(UserConnectionStatus.OFFLINE);
            return user;
        });

        userSessionService.getRoomId(targetSocketId).ifPresent(roomId ->
                broadcastToRoom(roomId, client, SocketEvent.USER_OFFLINE, Map.of("socketId", targetSocketId))
        );
    }

    private void handleUserOnline(SocketIOClient client, Map<String, Object> data, AckRequest ackRequest) {
        String targetSocketId = stringValue(data.get("socketId"));
        if (targetSocketId == null) {
            return;
        }

        userSessionService.updateUser(targetSocketId, user -> {
            user.setStatus(UserConnectionStatus.ONLINE);
            return user;
        });

        userSessionService.getRoomId(targetSocketId).ifPresent(roomId ->
                broadcastToRoom(roomId, client, SocketEvent.USER_ONLINE, Map.of("socketId", targetSocketId))
        );
    }

    private void handleSendMessage(SocketIOClient client, Map<String, Object> data, AckRequest ackRequest) {
        userSessionService.getRoomId(sessionId(client)).ifPresent(roomId -> {
            try {
                String topic = "room:chat:" + roomId;
                Object messagePayload = data.get("message");
                String jsonMessage = objectMapper.writeValueAsString(messagePayload);

                // Store chat history in Redis list and trim it to last 100 messages
                String historyKey = "room:chat:history:" + roomId;
                stringRedisTemplate.opsForList().rightPush(historyKey, jsonMessage);
                stringRedisTemplate.opsForList().trim(historyKey, -100, -1);

                // Publish for real-time broadcast
                stringRedisTemplate.convertAndSend(topic, jsonMessage);
            } catch (Exception e) {
                System.err.println("Failed to publish/store chat message: " + e.getMessage());
            }
        });
    }

    private void handleTypingStart(SocketIOClient client, Map<String, Object> data, AckRequest ackRequest) {
        int cursorPosition = numberValue(data.get("cursorPosition"));

        userSessionService.updateUser(sessionId(client), user -> {
            user.setTyping(true);
            user.setCursorPosition(cursorPosition);
            return user;
        });

        userSessionService.getUserBySocketId(sessionId(client)).ifPresent(user ->
                broadcastToRoom(user.getRoomId(), client, SocketEvent.TYPING_START, Map.of("user", user))
        );
    }

    private void handleTypingPause(SocketIOClient client, Map<String, Object> data, AckRequest ackRequest) {
        userSessionService.updateUser(sessionId(client), user -> {
            user.setTyping(false);
            return user;
        });

        userSessionService.getUserBySocketId(sessionId(client)).ifPresent(user ->
                broadcastToRoom(user.getRoomId(), client, SocketEvent.TYPING_PAUSE, Map.of("user", user))
        );
    }

    private void handleRequestDrawing(SocketIOClient client, Map<String, Object> data, AckRequest ackRequest) {
        userSessionService.getRoomId(sessionId(client)).ifPresent(roomId ->
                broadcastToRoom(roomId, client, SocketEvent.REQUEST_DRAWING, Map.of("socketId", sessionId(client)))
        );
    }

    private void handleSyncDrawing(SocketIOClient client, Map<String, Object> data, AckRequest ackRequest) {
        String targetSocketId = stringValue(data.get("socketId"));
        if (targetSocketId == null) {
            return;
        }

        SocketIOClient targetClient = findClientBySessionId(targetSocketId);
        if (targetClient == null) {
            return;
        }

        targetClient.sendEvent(SocketEvent.SYNC_DRAWING, Map.of("drawingData", data.get("drawingData")));
    }

    private void handleDrawingUpdate(SocketIOClient client, Map<String, Object> data, AckRequest ackRequest) {
        userSessionService.getRoomId(sessionId(client)).ifPresent(roomId -> {
            broadcastToRoom(roomId, client, SocketEvent.DRAWING_UPDATE, Map.of("snapshot", data.get("snapshot")));
        });
    }

    private void broadcastRoomEvent(SocketIOClient client, String eventName, Map<String, Object> data) {
        userSessionService.getRoomId(sessionId(client)).ifPresent(roomId ->
                broadcastToRoom(roomId, client, eventName, data)
        );
    }

    private void broadcastLockState(String roomId, SocketIOClient sender) {
        broadcastToRoom(
                roomId,
                sender,
                SocketEvent.FILE_LOCK_STATE,
                Map.of("locks", fileLockService.getRoomLockSnapshot(roomId))
        );
    }

    private void broadcastToRoom(String roomId, SocketIOClient sender, String eventName, Object payload) {
        server.getRoomOperations(roomId).sendEvent(eventName, sender, payload);
    }

    private SocketIOClient findClientBySessionId(String socketId) {
        for (SocketIOClient client : server.getAllClients()) {
            if (sessionId(client).equals(socketId)) {
                return client;
            }
        }
        return null;
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception exception) {
            return null;
        }
    }

    private String sessionId(SocketIOClient client) {
        return client.getSessionId().toString();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int numberValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
