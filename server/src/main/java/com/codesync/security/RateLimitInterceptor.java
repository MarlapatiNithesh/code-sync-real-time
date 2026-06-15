package com.codesync.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    // Limit to 100 requests per minute per IP + path
    private static final int MAX_REQUESTS = 100;
    private static final long TIME_WINDOW_SECONDS = 60;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();

        // Skip rate limiting for non-API requests (e.g. static assets)
        if (!path.startsWith("/api/")) {
            return true;
        }

        String key = "rate:limit:" + ip + ":" + path;
        Long count = stringRedisTemplate.opsForValue().increment(key);

        if (count == null) {
            return true;
        }

        if (count == 1) {
            // First request in this window, set TTL
            stringRedisTemplate.expire(key, TIME_WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        if (count > MAX_REQUESTS) {
            log.warn("Rate limit exceeded for IP: {} on path: {}. Count: {}", ip, path, count);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("text/plain");
            response.getWriter().write("Too Many Requests. Please try again in a minute.");
            return false;
        }

        return true;
    }
}
