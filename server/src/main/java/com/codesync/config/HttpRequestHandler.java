package com.codesync.config;

import io.netty.channel.ChannelHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
@ChannelHandler.Sharable
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final byte[] INDEX_HTML = loadIndexHtml();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        String path = uri.contains("?") ? uri.substring(0, uri.indexOf('?')) : uri;

        if ("/ping".equals(path)) {
            writeResponse(ctx, request, HttpResponseStatus.OK, "pong", "text/plain");
            return;
        }

        if ("/".equals(path) || "/index.html".equals(path)) {
            writeResponse(ctx, request, HttpResponseStatus.OK, INDEX_HTML, "text/html; charset=UTF-8");
            return;
        }

        ctx.fireChannelRead(request.retain());
    }

    private void writeResponse(
            ChannelHandlerContext ctx,
            FullHttpRequest request,
            HttpResponseStatus status,
            Object content,
            String contentType
    ) {
        byte[] bytes = content instanceof byte[] body
                ? body
                : content.toString().getBytes(StandardCharsets.UTF_8);

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.wrappedBuffer(bytes)
        );

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, bytes.length);

        if (request.headers().contains(HttpHeaderNames.CONNECTION, "keep-alive", true)) {
            response.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
        }

        ctx.writeAndFlush(response);
    }

    private static byte[] loadIndexHtml() {
        try (InputStream inputStream = new ClassPathResource("static/index.html").getInputStream()) {
            return StreamUtils.copyToByteArray(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load static/index.html", exception);
        }
    }
}
