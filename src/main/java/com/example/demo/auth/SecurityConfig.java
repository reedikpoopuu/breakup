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
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(401, "Unauthorized")))
                .addFilterBefore(new BearerTokenAuthFilter(tokenService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
