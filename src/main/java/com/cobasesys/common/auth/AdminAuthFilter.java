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

        // Security headers
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

        String normalizedPath = path.replaceAll("/+", "/");
        if (PUBLIC_PATHS.stream().anyMatch(p -> normalizedPath.equals(p))) {
            chain.doFilter(request, response);
            return;
        }

        String token = httpRequest.getHeader("Authorization");
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!StringUtils.hasText(token) || !jwtService.isValid(token)) {
            sendUnauthorized(httpResponse, "管理员认证失败，请重新登录");
            return;
        }

        // Reject refresh tokens used as access tokens
        try {
            var claims = jwtService.parseToken(token);
            if ("refresh".equals(claims.get("type"))) {
                sendUnauthorized(httpResponse, "不能使用 RefreshToken 访问接口");
                return;
            }
        } catch (Exception e) {
            sendUnauthorized(httpResponse, "Token解析失败");
            return;
        }

        httpRequest.setAttribute("adminUserId", jwtService.getUserId(token));
        httpRequest.setAttribute("adminUsername", jwtService.getUsername(token));
        httpRequest.setAttribute("adminPermissions", jwtService.getPermissions(token));

        chain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":10002,\"message\":\"" + message + "\"}");
    }
}
