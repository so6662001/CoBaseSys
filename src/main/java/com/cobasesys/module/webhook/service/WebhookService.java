package com.cobasesys.module.webhook.service;

import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.common.util.SignatureUtil;
import com.cobasesys.module.member.service.MemberService;
import com.cobasesys.module.points.service.PointService;
import com.cobasesys.module.wallet.service.WalletService;
import com.cobasesys.module.webhook.dto.WebhookDTO;
import com.cobasesys.module.webhook.entity.WebhookConfig;
import com.cobasesys.module.webhook.entity.WebhookLog;
import com.cobasesys.module.webhook.repository.WebhookConfigRepository;
import com.cobasesys.module.webhook.repository.WebhookLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookConfigRepository configRepository;
    private final WebhookLogRepository logRepository;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    @Value("${cobasesys.webhook.max-retry:3}")
    private int maxRetry;

    @Value("${cobasesys.webhook.timeout-seconds:10}")
    private int timeoutSeconds;

    // ==================== Event Listeners ====================

    @Async("webhookExecutor")
    @EventListener
    public void onPointChange(PointService.PointChangeEvent event) {
        String eventType = event.direction() == 1 ? "point.earned" : "point.deducted";
        Map<String, Object> payload = Map.of(
                "eventType", eventType,
                "tenantId", event.tenantId(),
                "userId", event.userId(),
                "points", event.points(),
                "newBalance", event.newBalance(),
                "timestamp", System.currentTimeMillis()
        );
        dispatch(event.tenantId(), eventType, payload);
    }

    @Async("webhookExecutor")
    @EventListener
    public void onWalletChange(WalletService.WalletChangeEvent event) {
        String eventType = event.direction() == 1 ? "wallet.recharged" : "wallet.consumed";
        Map<String, Object> payload = Map.of(
                "eventType", eventType,
                "tenantId", event.tenantId(),
                "userId", event.userId(),
                "amount", event.amount(),
                "newBalance", event.newBalance(),
                "timestamp", System.currentTimeMillis()
        );
        dispatch(event.tenantId(), eventType, payload);
    }

    @Async("webhookExecutor")
    @EventListener
    public void onMemberUpgrade(MemberService.MemberUpgradeEvent event) {
        Map<String, Object> payload = Map.of(
                "eventType", "member.upgraded",
                "tenantId", event.tenantId(),
                "userId", event.userId(),
                "levelCode", event.levelCode(),
                "levelName", event.levelName(),
                "levelRank", event.levelRank(),
                "timestamp", System.currentTimeMillis()
        );
        dispatch(event.tenantId(), "member.upgraded", payload);
    }

    // ==================== Core Logic ====================

    private void dispatch(Long tenantId, String eventType, Map<String, Object> payload) {
        List<WebhookConfig> configs = configRepository.findByTenantIdAndStatus(tenantId, 1);
        for (WebhookConfig config : configs) {
            if (!config.getEvents().contains(eventType)) continue;
            try {
                String payloadJson = objectMapper.writeValueAsString(payload);
                WebhookLog webhookLog = new WebhookLog();
                webhookLog.setTenantId(tenantId);
                webhookLog.setConfigId(config.getId());
                webhookLog.setEventType(eventType);
                webhookLog.setPayload(payloadJson);
                webhookLog.setStatus(0);
                logRepository.save(webhookLog);

                deliverWebhook(config, webhookLog);
            } catch (Exception e) {
                log.error("Failed to dispatch webhook for config {}: {}", config.getId(), e.getMessage());
            }
        }
    }

    private void deliverWebhook(WebhookConfig config, WebhookLog webhookLog) {
        try {
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String signature = config.getSecret() != null
                    ? SignatureUtil.hmacSha256(config.getSecret(), webhookLog.getPayload() + timestamp)
                    : "";

            String responseBody = webClientBuilder.build()
                    .post()
                    .uri(config.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Webhook-Signature", signature)
                    .header("X-Webhook-Timestamp", timestamp)
                    .header("X-Webhook-Event", webhookLog.getEventType())
                    .bodyValue(webhookLog.getPayload())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            webhookLog.setResponseStatus(200);
            webhookLog.setResponseBody(responseBody != null && responseBody.length() > 2000
                    ? responseBody.substring(0, 2000) : responseBody);
            webhookLog.setStatus(1);
        } catch (Exception e) {
            webhookLog.setStatus(2);
            webhookLog.setErrorMessage(e.getMessage() != null && e.getMessage().length() > 1000
                    ? e.getMessage().substring(0, 1000) : e.getMessage());
            webhookLog.setRetryCount(webhookLog.getRetryCount() + 1);
        }
        logRepository.save(webhookLog);
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void retryFailedWebhooks() {
        List<WebhookLog> failedLogs = logRepository.findByStatusAndRetryCountLessThan(2, maxRetry);
        for (WebhookLog webhookLog : failedLogs) {
            WebhookConfig config = configRepository.findById(webhookLog.getConfigId()).orElse(null);
            if (config == null || config.getStatus() != 1) continue;
            deliverWebhook(config, webhookLog);
        }
    }

    // ==================== Admin API ====================

    @Transactional
    public WebhookDTO.ConfigResponse createConfig(WebhookDTO.ConfigCreateRequest request) {
        WebhookConfig config = new WebhookConfig();
        BeanUtils.copyProperties(request, config);
        config.setTenantId(TenantContext.requireTenantId());
        if (config.getSecret() == null) {
            config.setSecret(SignatureUtil.generateAppSecret().substring(0, 32));
        }
        configRepository.save(config);
        return toConfigResponse(config);
    }

    @Transactional
    public WebhookDTO.ConfigResponse updateConfig(Long id, WebhookDTO.ConfigUpdateRequest request) {
        WebhookConfig config = configRepository.findById(id).orElseThrow();
        if (request.getWebhookName() != null) config.setWebhookName(request.getWebhookName());
        if (request.getUrl() != null) config.setUrl(request.getUrl());
        if (request.getSecret() != null) config.setSecret(request.getSecret());
        if (request.getEvents() != null) config.setEvents(request.getEvents());
        if (request.getStatus() != null) config.setStatus(request.getStatus());
        configRepository.save(config);
        return toConfigResponse(config);
    }

    public PageResult<WebhookDTO.ConfigResponse> listConfigs(Pageable pageable) {
        Page<WebhookConfig> page = configRepository.findByTenantId(
                TenantContext.requireTenantId(), pageable);
        return PageResult.from(page.map(this::toConfigResponse));
    }

    public PageResult<WebhookDTO.LogResponse> listLogs(Long configId, Pageable pageable) {
        Page<WebhookLog> page = configId != null
                ? logRepository.findByConfigIdOrderByCreatedAtDesc(configId, pageable)
                : logRepository.findByTenantIdOrderByCreatedAtDesc(TenantContext.requireTenantId(), pageable);
        return PageResult.from(page.map(this::toLogResponse));
    }

    @Transactional
    public void deleteConfig(Long id) {
        configRepository.deleteById(id);
    }

    private WebhookDTO.ConfigResponse toConfigResponse(WebhookConfig config) {
        WebhookDTO.ConfigResponse resp = new WebhookDTO.ConfigResponse();
        BeanUtils.copyProperties(config, resp);
        return resp;
    }

    private WebhookDTO.LogResponse toLogResponse(WebhookLog webhookLog) {
        WebhookDTO.LogResponse resp = new WebhookDTO.LogResponse();
        BeanUtils.copyProperties(webhookLog, resp);
        resp.setStatusText(switch (webhookLog.getStatus()) {
            case 0 -> "待发送";
            case 1 -> "成功";
            case 2 -> "失败";
            default -> "未知";
        });
        return resp;
    }
}
