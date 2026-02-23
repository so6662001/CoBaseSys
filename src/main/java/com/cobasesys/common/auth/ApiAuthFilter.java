package com.cobasesys.common.auth;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.common.exception.ErrorCode;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.common.util.SignatureUtil;
import com.cobasesys.module.system.entity.ExternalSystem;
import com.cobasesys.module.system.repository.ExternalSystemRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiAuthFilter implements Filter {

    private final ExternalSystemRepository systemRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${cobasesys.auth.timestamp-tolerance-seconds:300}")
    private int timestampTolerance;

    @Value("${cobasesys.auth.nonce-expire-seconds:300}")
    private int nonceExpire;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        if (!path.startsWith("/api/v1/")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
            authenticate(wrappedRequest);
            chain.doFilter(wrappedRequest, response);
        } catch (BizException e) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write(
                    "{\"code\":" + e.getCode() + ",\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    private void authenticate(ContentCachingRequestWrapper request) throws IOException {
        String appKey = request.getHeader("X-App-Key");
        String timestamp = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String signature = request.getHeader("X-Signature");

        if (!StringUtils.hasText(appKey) || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(nonce) || !StringUtils.hasText(signature)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Missing authentication headers");
        }

        long ts = Long.parseLong(timestamp);
        long now = System.currentTimeMillis() / 1000;
        if (Math.abs(now - ts) > timestampTolerance) {
            throw new BizException(ErrorCode.TIMESTAMP_EXPIRED);
        }

        String nonceKey = "nonce:" + nonce;
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(nonceKey, "1", Duration.ofSeconds(nonceExpire));
        if (Boolean.FALSE.equals(isNew)) {
            throw new BizException(ErrorCode.DUPLICATE_REQUEST);
        }

        ExternalSystem system = systemRepository.findByAppKey(appKey)
                .orElseThrow(() -> new BizException(ErrorCode.SYSTEM_NOT_FOUND));

        if (system.getStatus() != 1) {
            throw new BizException(ErrorCode.SYSTEM_DISABLED);
        }

        String body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        String bodyMd5 = StringUtils.hasText(body) ? SignatureUtil.md5(body) : "";

        boolean valid = SignatureUtil.verifySign(
                system.getAppSecret(),
                request.getMethod(),
                request.getRequestURI(),
                timestamp, nonce, bodyMd5, signature
        );

        if (!valid) {
            throw new BizException(ErrorCode.SIGNATURE_INVALID);
        }

        if (StringUtils.hasText(system.getIpWhitelist())) {
            String clientIp = getClientIp(request);
            if (!system.getIpWhitelist().contains(clientIp)) {
                throw new BizException(ErrorCode.IP_NOT_ALLOWED);
            }
        }

        TenantContext.setTenantId(system.getTenantId());
        request.setAttribute("currentSystem", system);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip)) return ip;
        return request.getRemoteAddr();
    }
}
