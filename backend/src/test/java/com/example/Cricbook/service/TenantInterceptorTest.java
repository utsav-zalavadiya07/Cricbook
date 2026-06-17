package com.example.Cricbook.service;

import com.example.Cricbook.entity.Tenant;
import com.example.Cricbook.repository.TenantRepository;
import com.example.Cricbook.security.TenantContext;
import com.example.Cricbook.security.TenantInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TenantInterceptorTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Object handler;

    @InjectMocks
    private TenantInterceptor tenantInterceptor;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    void testPreHandle_WithTenantHeader() throws Exception {
        when(request.getHeader("X-Tenant-ID")).thenReturn("5");

        boolean result = tenantInterceptor.preHandle(request, response, handler);

        assertTrue(result);
        assertEquals(5L, TenantContext.getCurrentTenant());
    }

    @Test
    void testPreHandle_WithInvalidTenantHeader_FallbackToDefault() throws Exception {
        when(request.getHeader("X-Tenant-ID")).thenReturn("invalid_id");
        when(request.getServerName()).thenReturn("unknown-domain.com");
        when(tenantRepository.findByDomain("unknown-domain.com")).thenReturn(Optional.empty());

        boolean result = tenantInterceptor.preHandle(request, response, handler);

        assertTrue(result);
        assertEquals(1L, TenantContext.getCurrentTenant());
    }

    @Test
    void testPreHandle_WithDomainMatch() throws Exception {
        when(request.getHeader("X-Tenant-ID")).thenReturn(null);
        when(request.getServerName()).thenReturn("whitelabel-turfs.ae");
        
        Tenant matchedTenant = Tenant.builder()
                .id(10L)
                .name("Dubai Turf Group")
                .domain("whitelabel-turfs.ae")
                .build();
        
        when(tenantRepository.findByDomain("whitelabel-turfs.ae")).thenReturn(Optional.of(matchedTenant));

        boolean result = tenantInterceptor.preHandle(request, response, handler);

        assertTrue(result);
        assertEquals(10L, TenantContext.getCurrentTenant());
    }

    @Test
    void testPreHandle_DefaultFallback() throws Exception {
        when(request.getHeader("X-Tenant-ID")).thenReturn(null);
        when(request.getServerName()).thenReturn("localhost");
        when(tenantRepository.findByDomain("localhost")).thenReturn(Optional.empty());

        boolean result = tenantInterceptor.preHandle(request, response, handler);

        assertTrue(result);
        assertEquals(1L, TenantContext.getCurrentTenant());
    }
}
