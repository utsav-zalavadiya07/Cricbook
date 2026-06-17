package com.example.Cricbook.security;

public class TenantContext {

    private static final ThreadLocal<Long> currentTenant = new ThreadLocal<>();

    public static void setCurrentTenant(Long tenantId) {
        currentTenant.set(tenantId);
    }

    public static Long getCurrentTenant() {
        return currentTenant.get() != null ? currentTenant.get() : 1L; // fallback default tenant ID: 1
    }

    public static void clear() {
        currentTenant.remove();
    }
}
