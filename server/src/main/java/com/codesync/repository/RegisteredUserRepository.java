package com.codesync.repository;

import com.codesync.entity.RegisteredUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RegisteredUserRepository extends JpaRepository<RegisteredUser, UUID> {

    Optional<RegisteredUser> findByEmail(String email);

    Optional<RegisteredUser> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
