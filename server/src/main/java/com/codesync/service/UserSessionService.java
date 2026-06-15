package com.codesync.service;

import com.codesync.model.User;
import com.codesync.model.UserConnectionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

@Service
@RequiredArgsConstructor
public class UserSessionService {

    private static final String SESSIONS_HASH_KEY = "codesync:sessions";
    private static final String ROOM_USERS_SET_KEY_PREFIX = "codesync:room:users:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private User convertToUser(Object obj) {
        if (obj == null) {
            return null;
        }
        return objectMapper.convertValue(obj, User.class);
    }

    public List<User> getUsersInRoom(String roomId) {
        String roomKey = ROOM_USERS_SET_KEY_PREFIX + roomId;
        Set<Object> socketIds = redisTemplate.opsForSet().members(roomKey);
        if (socketIds == null || socketIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Object> userObjects = redisTemplate.opsForHash().multiGet(
                SESSIONS_HASH_KEY,
                socketIds.stream().map(id -> (Object) id.toString()).toList()
        );

        return userObjects.stream()
                .filter(Objects::nonNull)
                .map(this::convertToUser)
                .toList();
    }

    public Optional<String> getRoomId(String socketId) {
        return getUserBySocketId(socketId).map(User::getRoomId);
    }

    public Optional<User> getUserBySocketId(String socketId) {
        Object userObj = redisTemplate.opsForHash().get(SESSIONS_HASH_KEY, socketId);
        if (userObj == null) {
            System.err.println("User not found in Redis for socket ID: " + socketId);
            return Optional.empty();
        }
        return Optional.ofNullable(convertToUser(userObj));
    }

    public boolean isUsernameTaken(String roomId, String username) {
        return getUsersInRoom(roomId).stream()
                .anyMatch(user -> username.equalsIgnoreCase(user.getUsername()));
    }

    public User createUser(String username, String roomId, String socketId) {
        User user = new User(
                username,
                roomId,
                UserConnectionStatus.ONLINE,
                0,
                false,
                null,
                socketId
        );

        // Save session in hash
        redisTemplate.opsForHash().put(SESSIONS_HASH_KEY, socketId, user);
        // Add socket to room set
        redisTemplate.opsForSet().add(ROOM_USERS_SET_KEY_PREFIX + roomId, socketId);

        return user;
    }

    public void removeUser(String socketId) {
        getUserBySocketId(socketId).ifPresent(user -> {
            redisTemplate.opsForSet().remove(ROOM_USERS_SET_KEY_PREFIX + user.getRoomId(), socketId);
            redisTemplate.opsForHash().delete(SESSIONS_HASH_KEY, socketId);
        });
    }

    public void updateUser(String socketId, UnaryOperator<User> updater) {
        getUserBySocketId(socketId).ifPresent(user -> {
            User updatedUser = updater.apply(user);
            redisTemplate.opsForHash().put(SESSIONS_HASH_KEY, socketId, updatedUser);
        });
    }

    public List<User> snapshotUsersInRoom(String roomId) {
        return getUsersInRoom(roomId);
    }
}
