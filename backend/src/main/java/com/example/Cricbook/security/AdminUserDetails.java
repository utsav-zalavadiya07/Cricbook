package com.example.Cricbook.security;

import com.example.Cricbook.entity.AdminUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminUserDetails implements UserDetails {

    private final AdminUser adminUser;
    private final Set<String> permissions;

    public AdminUserDetails(AdminUser adminUser) {
        this(adminUser, Set.of());
    }

    public AdminUserDetails(AdminUser adminUser, Set<String> permissions) {
        this.adminUser = adminUser;
        this.permissions = permissions;
    }

    public AdminUser getAdminUser() {
        return adminUser;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<SimpleGrantedAuthority> authorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + adminUser.getRole()));
        return authorities;
    }

    @Override
    public String getPassword() {
        return adminUser.getPassword();
    }

    @Override
    public String getUsername() {
        return adminUser.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return "ACTIVE".equalsIgnoreCase(adminUser.getStatus());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equalsIgnoreCase(adminUser.getStatus());
    }
}
