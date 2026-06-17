package com.example.Cricbook.security;

import com.example.Cricbook.entity.Tenant;
import com.example.Cricbook.repository.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private final TenantRepository tenantRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantHeader = request.getHeader("X-Tenant-ID");
        if (tenantHeader != null && !tenantHeader.trim().isEmpty()) {
            try {
                Long tenantId = Long.parseLong(tenantHeader);
                TenantContext.setCurrentTenant(tenantId);
                return true;
            } catch (NumberFormatException e) {
                // Ignore and fall back to domain resolution
            }
        }

        // Domain matching resolution
        String serverName = request.getServerName();
        Optional<Tenant> tenantOpt = tenantRepository.findByDomain(serverName);
        if (tenantOpt.isPresent()) {
            TenantContext.setCurrentTenant(tenantOpt.get().getId());
        } else {
            TenantContext.setCurrentTenant(1L); // default fallback tenant ID
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        TenantContext.clear();
    }
}
