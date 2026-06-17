package com.example.Cricbook.config;

import com.example.Cricbook.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          UserDetailsService userDetailsService,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/api/v1/admin/auth/**", "/swagger-ui/**", "/v3/api-docs/**", "/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/search", "/api/v1/turfs/*", "/api/v1/turfs/*/availability", "/api/v1/reviews/turf/*", "/api/v1/platform/health", "/api/v1/sports/**", "/api/v1/developer-platform/docs", "/api/v1/developer-platform/marketplace", "/api/v1/platform-api/**", "/api/v1/academies/marketplace", "/api/v1/coaches/marketplace", "/api/v1/training/programs", "/api/v1/talent/discovery", "/api/v1/certificates/*/verify", "/api/v1/corporates/marketplace", "/api/v1/live-scores", "/api/v1/live-scores/**", "/api/v1/commerce/products", "/api/v1/commerce/products/*", "/api/v1/travel/packages", "/api/v1/travel/events", "/api/v1/travel/experiences", "/api/v1/travel/accommodations", "/api/v1/career/marketplace", "/api/v1/career/opportunities", "/api/v1/career/resumes/*", "/api/v1/governance/federations", "/api/v1/governance/rankings", "/api/v1/super-app/search", "/api/v1/super-app/passports/*", "/api/v1/sports-os/dashboard", "/api/v1/sports-os/facilities", "/api/v1/sports-os/facilities/*", "/api/v1/fan-economy/dashboard", "/api/v1/fan-economy/clubs", "/api/v1/fan-economy/collectibles", "/api/v1/fan-economy/predictions", "/api/v1/fan-economy/creators", "/api/v1/fan-economy/marketplace/listings", "/api/v1/fan-economy/watch-parties", "/api/v1/saas/tenant-context", "/api/v1/saas/marketplace/modules", "/api/v1/saas/regions").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/sports/rules/simulate", "/api/v1/sports/validate-booking", "/api/v1/sports/validate-team").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/developers/register", "/api/v1/developer-platform/oauth/token", "/api/v1/platform-api/**", "/api/v1/academies/register", "/api/v1/coaches/register", "/api/v1/corporates/register", "/api/v1/travel/partners", "/api/v1/career/scouts/register").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                writeSecurityError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "AUTH_REQUIRED", "Please sign in to continue.", request.getRequestURI()))
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            var authentication = SecurityContextHolder.getContext().getAuthentication();
                            boolean anonymous = authentication == null || authentication instanceof AnonymousAuthenticationToken;
                            if (anonymous) {
                                writeSecurityError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "AUTH_REQUIRED", "Please sign in to continue.", request.getRequestURI());
                                return;
                            }
                            writeSecurityError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden", "ACCESS_DENIED", "You do not have permission to access this resource.", request.getRequestURI());
                        })
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeSecurityError(HttpServletResponse response,
                                    int status,
                                    String error,
                                    String code,
                                    String message,
                                    String path) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("""
                {"status":%d,"error":"%s","code":"%s","message":"%s","path":"%s","details":null}
                """.formatted(status, error, code, message, path));
    }
}
