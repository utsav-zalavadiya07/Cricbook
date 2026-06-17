package com.example.Cricbook.security;

import com.example.Cricbook.entity.User;
import com.example.Cricbook.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.example.Cricbook.repository.AdminUserRepository adminUserRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void testLoadUserByUsername_EmailSuccess() {
        User user = User.builder().email("test@test.com").password("pass").build();
        when(adminUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("test@test.com");
        assertEquals("test@test.com", details.getUsername());
    }

    @Test
    void testLoadUserByUsername_PhoneSuccess() {
        User user = User.builder().phone("1234567890").password("pass").build();
        when(adminUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("1234567890")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("1234567890")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("1234567890");
        assertNotNull(details);
    }

    @Test
    void testLoadUserByUsername_NotFound() {
        when(adminUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByPhone(anyString())).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("none"));
    }
}
