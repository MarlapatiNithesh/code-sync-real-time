package com.codesync.socket;

import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatListener implements MessageListener {

    private final SocketIOServer socketIOServer;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String body = new String(message.getBody(), StandardCharsets.UTF_8);

            // Channel format: room:chat:<roomId>
            if (channel.startsWith("room:chat:")) {
                String roomId = channel.substring("room:chat:".length());
                Object messageObj = objectMapper.readValue(body, Object.class);

                // Broadcast to local Socket.io room clients
                socketIOServer.getRoomOperations(roomId).sendEvent(SocketEvent.RECEIVE_MESSAGE, Map.of("message", messageObj));
                log.debug("Broadcasted Redis pub/sub chat message to room {}", roomId);
            }
        } catch (Exception e) {
            log.error("Failed to handle Redis pub/sub message", e);
        }
    }
}
