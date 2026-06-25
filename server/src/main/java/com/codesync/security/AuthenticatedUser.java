package com.codesync.security;

import com.codesync.entity.RegisteredUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class AuthenticatedUser implements UserDetails {

    private final UUID id;
    private final String email;
    private final String username;
    private final String passwordHash;

    public AuthenticatedUser(RegisteredUser user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
    }

    public String getRealUsername() {
        return username;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
