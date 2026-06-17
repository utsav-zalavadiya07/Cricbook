package com.example.Cricbook.security;

import com.example.Cricbook.enums.RoleName;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.putIfAbsent("email", userDetails.getUsername());

        if (userDetails instanceof CustomUserDetails customUserDetails) {
            com.example.Cricbook.entity.User user = customUserDetails.getUser();
            claims.put("userId", user.getId());
            claims.put("role", resolveUserRole(user));
            claims.put("tokenVersion", user.getTokenVersion() == null ? 0 : user.getTokenVersion());
        } else if (userDetails instanceof AdminUserDetails adminUserDetails) {
            com.example.Cricbook.entity.AdminUser adminUser = adminUserDetails.getAdminUser();
            claims.put("userId", adminUser.getId());
            claims.put("role", adminUser.getRole());
            claims.put("admin", true);
        }

        return buildToken(claims, userDetails, jwtExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts
                .builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token)
                && isTokenVersionValid(token, userDetails);
    }

    public Integer extractTokenVersion(String token) {
        return extractClaim(token, claims -> claims.get("tokenVersion", Integer.class));
    }

    public String extractSessionId(String token) {
        return extractClaim(token, claims -> claims.get("sessionId", String.class));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private boolean isTokenVersionValid(String token, UserDetails userDetails) {
        if (!(userDetails instanceof CustomUserDetails customUserDetails)) {
            return true;
        }

        Integer tokenVersion = extractTokenVersion(token);
        Integer currentVersion = customUserDetails.getUser().getTokenVersion();
        return tokenVersion != null && tokenVersion.equals(currentVersion == null ? 0 : currentVersion);
    }

    private String resolveUserRole(com.example.Cricbook.entity.User user) {
        if (user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN)) {
            return "ADMIN";
        }
        if (user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_OWNER)) {
            return "OWNER";
        }
        return "USER";
    }
}
