package com.codesync.config;

import com.codesync.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.client-url:*}")
    private String clientUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/**", "OPTIONS")).permitAll()
                        .requestMatchers(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/auth/signup")).permitAll()
                        .requestMatchers(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/auth/login")).permitAll()
                        .requestMatchers(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/ping", "GET")).permitAll()
                        .requestMatchers(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/", "GET")).permitAll()
                        .requestMatchers(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/index.html", "GET")).permitAll()
                        .requestMatchers(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/rooms/code/**")).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        String normalizedOrigin = normalizeOrigin(clientUrl);
        if ("*".equals(normalizedOrigin)) {
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            configuration.setAllowedOrigins(List.of(normalizedOrigin));
            configuration.setAllowedOriginPatterns(List.of(normalizedOrigin));
        }
        
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private String normalizeOrigin(String rawClientUrl) {
        if (rawClientUrl == null || rawClientUrl.isBlank() || "*".equals(rawClientUrl)) {
            return "*";
        }
        return rawClientUrl.endsWith("/") ? rawClientUrl.substring(0, rawClientUrl.length() - 1) : rawClientUrl;
    }
}
