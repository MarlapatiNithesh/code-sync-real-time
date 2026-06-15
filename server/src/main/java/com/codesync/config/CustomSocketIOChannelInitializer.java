package com.codesync.config;

import com.corundumstudio.socketio.SocketIOChannelInitializer;
import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

@Component
public class CustomSocketIOChannelInitializer extends SocketIOChannelInitializer {

    private final HttpRequestHandler httpRequestHandler;

    public CustomSocketIOChannelInitializer(HttpRequestHandler httpRequestHandler) {
        this.httpRequestHandler = httpRequestHandler;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        super.initChannel(channel);
        channel.pipeline().addBefore(PACKET_HANDLER, "httpRequestHandler", httpRequestHandler);
    }
}
