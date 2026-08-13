package com.example.demo.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Populates the Spring Security context from an {@code Authorization: Bearer <token>}
 * header. Smart-ID session tokens are the only login path (ARCH_SPEC.md section 2.4) -
 * there is no form login or client-credentials grant to fall back to.
 */
public class BearerTokenAuthFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    public BearerTokenAuthFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length());
            Optional<TokenService.Principal> principal = tokenService.validate(token);
            if (principal.isPresent()) {
                TokenService.Principal p = principal.get();
                var authority = new SimpleGrantedAuthority("ROLE_" + p.role().name());
                var authentication = new UsernamePasswordAuthenticationToken(p, token, List.of(authority));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
