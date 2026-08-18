package com.example.demo.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Smart-ID session tokens are the only login path for both regular users and the
 * ADMIN role (ARCH_SPEC.md section 2.4) - no password grant, so this is stateless
 * bearer-token auth with no form login.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, TokenService tokenService) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/admin.html", "/static/**", "/assets/**", "/*.js", "/*.css", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/api/auth/smart-id/**").permitAll()
                        .requestMatchers("/api/suppliers", "/api/suppliers/**").permitAll()
                        .requestMatchers("/api/packages", "/api/packages/**").permitAll()
                        .requestMatchers("/api/switching/**").permitAll()
                        // Second layer independent of spring.h2.console.enabled (see
                        // application.properties): even with the console switched on for
                        // local dev, only a real ROLE_ADMIN session can reach it.
                        .requestMatchers("/h2-console/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        // Baseline defense-in-depth, not a substitute for correct escaping
                        // (index.html/admin.html already route dynamic content through
                        // escapeHtml() everywhere it's rendered) - if a future edit ever
                        // misses that, this at least blocks exfiltration to a third-party
                        // origin and stops an injected <script src="evil"> from loading.
                        // 'unsafe-inline' for script/style is required, not optional: both
                        // pages are single self-contained files with their JS/CSS inline,
                        // not split into external assets - a stricter policy would need
                        // that split done first (e.g. nonces on an external script file).
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; "
                                        + "script-src 'self' 'unsafe-inline'; "
                                        + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                                        + "font-src 'self' https://fonts.gstatic.com; "
                                        + "img-src 'self' data:; "
                                        + "connect-src 'self'; "
                                        + "frame-ancestors 'self'")))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(401, "Unauthorized")))
                .addFilterBefore(new BearerTokenAuthFilter(tokenService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
