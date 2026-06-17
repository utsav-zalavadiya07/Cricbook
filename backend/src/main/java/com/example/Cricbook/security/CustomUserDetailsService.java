package com.example.Cricbook.security;

import com.example.Cricbook.entity.AdminUser;
import com.example.Cricbook.entity.TemporaryPermission;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.repository.AdminRoleRepository;
import com.example.Cricbook.repository.AdminUserRepository;
import com.example.Cricbook.repository.TemporaryPermissionRepository;
import com.example.Cricbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final TemporaryPermissionRepository temporaryPermissionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        String normalizedLoginId = loginId == null ? "" : loginId.trim();
        String normalizedEmailLoginId = normalizedLoginId.contains("@") ? normalizedLoginId.toLowerCase() : normalizedLoginId;

        Optional<AdminUser> adminUserOpt = adminUserRepository.findByEmail(normalizedEmailLoginId);
        if (adminUserOpt.isPresent()) {
            AdminUser adminUser = adminUserOpt.get();
            Set<String> permissions = new HashSet<>();
            adminRoleRepository.findByName(adminUser.getRole())
                    .filter(role -> !Boolean.FALSE.equals(role.getActive()))
                    .ifPresent(role -> permissions.addAll(role.getPermissions().stream()
                            .map(permission -> permission.getCode())
                            .collect(Collectors.toSet())));
            permissions.addAll(temporaryPermissionRepository.findByUserIdAndExpiresAtAfter(adminUser.getId(), LocalDateTime.now()).stream()
                    .map(TemporaryPermission::getPermission)
                    .map(permission -> permission.getCode())
                    .collect(Collectors.toSet()));
            return new AdminUserDetails(adminUser, permissions);
        }

        User user = userRepository.findByEmailIgnoreCase(normalizedEmailLoginId)
                .or(() -> userRepository.findByPhone(normalizedLoginId))
                .orElseThrow(() -> new UsernameNotFoundException("User/Admin not found: " + loginId));
        return new CustomUserDetails(user);
    }
}
