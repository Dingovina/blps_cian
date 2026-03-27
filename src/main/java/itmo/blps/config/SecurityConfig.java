package itmo.blps.config;

import itmo.blps.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * Returns true for paths that have no real controller — i.e. paths that fall
     * through to ApiFallbackController. Such requests are permitted by Security
     * so the controller can return a proper 404 JSON response.
     */
    private static boolean isUnknownApiPath(jakarta.servlet.http.HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/cian/api/") &&
                !uri.startsWith("/cian/api/auth/") &&
                !uri.startsWith("/cian/api/listings/") &&
                !uri.startsWith("/cian/api/listings") &&
                !uri.startsWith("/cian/api/seller/") &&
                !uri.startsWith("/cian/api/inquiries/") &&
                !uri.startsWith("/cian/api/notifications/") &&
                !uri.startsWith("/cian/api/admin/") &&
                !uri.startsWith("/cian/api/webhooks/");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // unknown /api/** paths → let through to ApiFallbackController → 404
                        .requestMatchers(SecurityConfig::isUnknownApiPath).permitAll()
                        // public endpoints
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/listings/search").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // role-based
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // everything else requires authentication
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
