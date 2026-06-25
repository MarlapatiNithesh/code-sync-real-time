package com.codesync.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PistonRuntimeInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) {
        log.info("Initializing Piston code execution runtimes...");
        new Thread(() -> {
            RestTemplate restTemplate = new RestTemplate();
            String runtimesUrl = "http://piston:2000/api/v2/packages";

            // Wait for Piston service to be up
            int maxRetries = 30;
            boolean connected = false;
            for (int i = 0; i < maxRetries; i++) {
                try {
                    restTemplate.getForObject(runtimesUrl, Object[].class);
                    connected = true;
                    log.info("Successfully connected to Piston API.");
                    break;
                } catch (Exception e) {
                    log.info("Waiting for Piston API to start... (retry {}/{})", i + 1, maxRetries);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }

            if (!connected) {
                log.error("Could not connect to Piston API after {} retries.", maxRetries);
                return;
            }

            // Install JavaScript (Node) and Python
            installRuntime(restTemplate, runtimesUrl, "node", "*");
            installRuntime(restTemplate, runtimesUrl, "python", "*");
            installRuntime(restTemplate, runtimesUrl, "c++", "*");
            installRuntime(restTemplate, runtimesUrl, "java", "*");
            installRuntime(restTemplate, runtimesUrl, "go", "*");
            installRuntime(restTemplate, runtimesUrl, "ruby", "*");
            installRuntime(restTemplate, runtimesUrl, "c#", "*");
            installRuntime(restTemplate, runtimesUrl, "rust", "*");
            installRuntime(restTemplate, runtimesUrl, "typescript", "*");
            installRuntime(restTemplate, runtimesUrl, "php", "*");
            installRuntime(restTemplate, runtimesUrl, "kotlin", "*");
            installRuntime(restTemplate, runtimesUrl, "swift", "*");
            installRuntime(restTemplate, runtimesUrl, "dart", "*");
            installRuntime(restTemplate, runtimesUrl, "scala", "*");
            installRuntime(restTemplate, runtimesUrl, "haskell", "*");
            installRuntime(restTemplate, runtimesUrl, "lua", "*");
            installRuntime(restTemplate, runtimesUrl, "r", "*");
            installRuntime(restTemplate, runtimesUrl, "matlab", "*");
            installRuntime(restTemplate, runtimesUrl, "perl", "*");
            installRuntime(restTemplate, runtimesUrl, "powershell", "*");
            installRuntime(restTemplate, runtimesUrl, "bash", "*");

        }).start();
    }

    private void installRuntime(RestTemplate restTemplate, String url, String language, String version) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> request = Map.of("language", language, "version", version);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            log.info("Requesting Piston to install runtime: {} ({})", language, version);
            Object response = restTemplate.postForObject(url, entity, Object.class);
            log.info("Piston response for {}: {}", language, response);
        } catch (Exception e) {
            log.error("Failed to install Piston runtime {}: {}", language, e.getMessage());
        }
    }
}
