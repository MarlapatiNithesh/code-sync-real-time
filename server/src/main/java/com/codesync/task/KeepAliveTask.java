package com.codesync.task;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class KeepAliveTask {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${app.client-url:*}")
    private String clientUrl;

    @Value("${app.backend-url}")
    private String backendUrl;

    @Scheduled(fixedDelayString = "${app.keep-alive-interval-ms}")
    public void pingServices() {
        pingUrl(clientUrl, false);
        pingUrl(backendUrl, true);
    }

    private void pingUrl(String url, boolean appendPingPath) {
        if (url == null || url.isBlank() || "*".equals(url) || url.contains("localhost")) {
            return;
        }

        String normalizedUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        String targetUrl = appendPingPath ? normalizedUrl + "/ping" : normalizedUrl;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Pinged " + targetUrl + " successfully. Status: " + response.statusCode());
        } catch (Exception exception) {
            System.err.println("Error pinging " + targetUrl + ": " + exception.getMessage());
        }
    }
}
