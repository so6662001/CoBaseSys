package com.cobasesys.common.auth;

import com.cobasesys.module.security.service.JwtService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class AdminAuthFilter implements Filter {

    private final JwtService jwtService;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/admin/auth/login", "/admin/auth/refresh"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        if (!path.startsWith("/admin/")) {
            chain.doFilter(request, response);
            return;
        }

        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            chain.doFilter(request, response);
            return;
        }

        String token = httpRequest.getHeader("Authorization");
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!StringUtils.hasText(token) || !jwtService.isValid(token)) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"code\":10002,\"message\":\"管理员认证失败，请重新登录\"}");
            return;
        }

        httpRequest.setAttribute("adminUserId", jwtService.getUserId(token));
        httpRequest.setAttribute("adminUsername", jwtService.getUsername(token));
        httpRequest.setAttribute("adminPermissions", jwtService.getPermissions(token));

        chain.doFilter(request, response);
    }
}
