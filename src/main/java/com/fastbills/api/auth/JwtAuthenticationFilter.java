package com.fastbills.api.auth;

import com.fastbills.api.common.tenant.TenantContext;
import com.fastbills.api.user.UserRole;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = extractBearerToken(request);
            if (token != null) {
                Claims claims = jwtService.parseAndValidateToken(token);
                UUID userId = jwtService.extractUserId(claims);
                UUID organisationId = jwtService.extractOrganisationId(claims);
                String email = jwtService.extractEmail(claims);
                String roleStr = jwtService.extractRole(claims);
                String fullName = claims.get("fullName", String.class);

                UserRole role = UserRole.valueOf(roleStr);
                AuthenticatedUser principal = new AuthenticatedUser(userId, organisationId, email, fullName, role);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Set thread-local tenant scope
                TenantContext.setCurrentTenant(organisationId);
            }

            filterChain.doFilter(request, response);
        } finally {
            // Always clean up thread-local tenant context to prevent leak
            TenantContext.clear();
        }
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }
}
