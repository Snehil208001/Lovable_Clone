package com.snehil.project.lovable_clone.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String message = authException.getMessage() != null ? authException.getMessage() : "Authentication required";
        response.getWriter().write(String.format(
                "{\"status\":\"UNAUTHORIZED\",\"message\":\"%s\"}",
                message.replace("\\", "\\\\").replace("\"", "\\\"")));
    }
}
