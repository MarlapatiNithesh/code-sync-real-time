package com.codesync.service;

import com.codesync.dto.AuthResponse;
import com.codesync.dto.LoginRequest;
import com.codesync.dto.SignupRequest;
import com.codesync.dto.UserProfileResponse;
import com.codesync.entity.RegisteredUser;
import com.codesync.repository.RegisteredUserRepository;
import com.codesync.security.AuthenticatedUser;
import com.codesync.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RegisteredUserRepository registeredUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (registeredUserRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (registeredUserRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        // Encodes the password using BCrypt hashing with a secure, unique auto-generated salt.
        // BCryptPasswordEncoder automatically generates a cryptographically strong 16-byte salt,
        // combines it with the password, hashes it using 10 rounds of key derivation (cost factor),
        // and embeds the salt directly into the output hash string ($2a$10$[salt][hash]).
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        RegisteredUser user = RegisteredUser.builder()
                .email(request.getEmail().trim().toLowerCase())
                .username(request.getUsername().trim())
                .passwordHash(hashedPassword)
                .build();

        RegisteredUser savedUser = registeredUserRepository.save(user);
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(savedUser);
        return buildAuthResponse(authenticatedUser);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().trim().toLowerCase(),
                        request.getPassword()
                )
        );

        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
        return buildAuthResponse(authenticatedUser);
    }

    public UserProfileResponse getProfile(AuthenticatedUser user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getRealUsername())
                .build();
    }

    private AuthResponse buildAuthResponse(AuthenticatedUser user) {
        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getRealUsername())
                .build();
    }
}
