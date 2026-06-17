package com.example.Cricbook.dto;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionRequestContext {
    private String ipAddress;
    private String userAgent;
    private String browser;
    private String os;
    private String deviceName;

    public static SessionRequestContext system() {
        return SessionRequestContext.builder()
                .ipAddress("system")
                .userAgent("system")
                .browser("System")
                .os("System")
                .deviceName("System")
                .build();
    }

    public static SessionRequestContext from(HttpServletRequest request) {
        if (request == null) {
            return system();
        }
        String userAgent = trim(request.getHeader("User-Agent"), 500);
        String browser = detectBrowser(userAgent);
        String os = detectOs(userAgent);
        return SessionRequestContext.builder()
                .ipAddress(resolveIpAddress(request))
                .userAgent(userAgent == null || userAgent.isBlank() ? "unknown" : userAgent)
                .browser(browser)
                .os(os)
                .deviceName(browser + " on " + os)
                .build();
    }

    private static String resolveIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return trim(forwardedFor.split(",")[0].trim(), 100);
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return trim(realIp.trim(), 100);
        }
        return trim(request.getRemoteAddr(), 100);
    }

    private static String detectBrowser(String userAgent) {
        String normalized = userAgent == null ? "" : userAgent.toLowerCase();
        if (normalized.contains("edg/")) return "Microsoft Edge";
        if (normalized.contains("opr/") || normalized.contains("opera")) return "Opera";
        if (normalized.contains("firefox/")) return "Firefox";
        if (normalized.contains("chrome/") || normalized.contains("crios/")) return "Chrome";
        if (normalized.contains("safari/")) return "Safari";
        if (normalized.contains("expo")) return "Expo Go";
        return "Unknown browser";
    }

    private static String detectOs(String userAgent) {
        String normalized = userAgent == null ? "" : userAgent.toLowerCase();
        if (normalized.contains("android")) return "Android";
        if (normalized.contains("iphone") || normalized.contains("ipad")) return "iOS";
        if (normalized.contains("windows")) return "Windows";
        if (normalized.contains("mac os") || normalized.contains("macintosh")) return "macOS";
        if (normalized.contains("linux")) return "Linux";
        return "Unknown OS";
    }

    private static String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
