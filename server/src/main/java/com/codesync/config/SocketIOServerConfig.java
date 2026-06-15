package com.codesync.config;

import com.corundumstudio.socketio.SocketIOServer;
import com.codesync.socket.SocketEventListener;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SocketIOServerConfig {

    private final CustomSocketIOChannelInitializer channelInitializer;
    private final SocketEventListener socketEventListener;
    private SocketIOServer socketIOServer;

    @Value("${app.socket.port}")
    private int socketPort;

    @Value("${app.client-url:*}")
    private String clientUrl;

    public SocketIOServerConfig(
            CustomSocketIOChannelInitializer channelInitializer,
            SocketEventListener socketEventListener
    ) {
        this.channelInitializer = channelInitializer;
        this.socketEventListener = socketEventListener;
    }

    @Bean
    public SocketIOServer socketIOServer() {
        String normalizedClientUrl = normalizeClientUrl(clientUrl);

        com.corundumstudio.socketio.Configuration configuration =
                new com.corundumstudio.socketio.Configuration();
        configuration.setPort(socketPort);
        configuration.setOrigin(normalizedClientUrl);
        configuration.setAllowCustomRequests(true);
        configuration.setPingTimeout(60000);
        configuration.setMaxHttpContentLength(100_000_000);

        socketIOServer = new SocketIOServer(configuration);
        socketIOServer.setPipelineFactory(channelInitializer);
        socketEventListener.registerHandlers(socketIOServer);

        socketIOServer.start();
        System.out.println("Socket.IO running on http://localhost:" + socketPort);
        return socketIOServer;
    }

    @PreDestroy
    public void stopServer() {
        if (socketIOServer != null) {
            socketIOServer.stop();
        }
    }

    private String normalizeClientUrl(String rawClientUrl) {
        if (rawClientUrl == null || rawClientUrl.isBlank() || "*".equals(rawClientUrl)) {
            return "*";
        }
        return rawClientUrl.endsWith("/") ? rawClientUrl.substring(0, rawClientUrl.length() - 1) : rawClientUrl;
    }
}
