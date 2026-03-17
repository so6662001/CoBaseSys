package com.cobasesys.common.tenant;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantFilter implements Filter {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        if (isExcluded(path)) {
            chain.doFilter(request, response);
            return;
        }

        String tenantIdStr = httpRequest.getHeader(TENANT_HEADER);
        if (!StringUtils.hasText(tenantIdStr)) {
            tenantIdStr = httpRequest.getParameter("tenantId");
        }

        if (StringUtils.hasText(tenantIdStr)) {
            try {
                TenantContext.setTenantId(Long.parseLong(tenantIdStr));
            } catch (NumberFormatException e) {
                httpResponse.setStatus(400);
                httpResponse.getWriter().write("{\"code\":10007,\"message\":\"Invalid tenant ID\"}");
                return;
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isExcluded(String path) {
        return path.startsWith("/actuator")
                || path.startsWith("/swagger")
                || path.startsWith("/api-docs")
                || path.startsWith("/v3/api-docs");
    }
}
