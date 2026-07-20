package com.snehil.project.lovable_clone.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Exposes the deployed git SHA so clients/ops can confirm which backend build is live. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AppRevisionFilter extends OncePerRequestFilter {

    @Value("${APP_GIT_SHA:unknown}")
    private String gitSha;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        response.setHeader("X-App-Git-Sha", gitSha);
        filterChain.doFilter(request, response);
    }
}
