package com.snehil.project.lovable_clone.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AuthUtil authUtil;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;

    /**
     * SSE / {@code Flux} endpoints use Servlet async. By default {@link OncePerRequestFilter} does not run
     * on ASYNC dispatch, so {@link SecurityContextHolder} stays empty there and secured streaming returns 401.
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        log.info("incoming request: {}", request.getRequestURI());

        final String requestHeaderToken = request.getHeader("Authorization");
        if (requestHeaderToken == null || !requestHeaderToken.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwtToken = requestHeaderToken.substring("Bearer ".length()).trim();
        if (jwtToken.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            JwtUserPrincipal user = authUtil.verifyAccessToken(jwtToken);
            // Always attach JWT when the header is present and valid. On Servlet ASYNC dispatches the context
            // may already hold AnonymousAuthenticationToken (non-null), which would skip the old null-check and
            // leave the thread unauthenticated → AuthorizationDeniedException on streamed endpoints.
            if (user != null) {
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        user, null, user.authorities()
                );
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            jwtAuthEntryPoint.commence(request, response, new BadCredentialsException("Invalid or expired token"));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
