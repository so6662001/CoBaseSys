package com.cobasesys.module.notification.service;

import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.notification.dto.NotificationDTO;
import com.cobasesys.module.notification.entity.NotificationRecord;
import com.cobasesys.module.notification.entity.NotificationRule;
import com.cobasesys.module.notification.entity.NotificationTemplate;
import com.cobasesys.module.notification.repository.NotificationRecordRepository;
import com.cobasesys.module.notification.repository.NotificationRuleRepository;
import com.cobasesys.module.notification.repository.NotificationTemplateRepository;
import com.cobasesys.module.points.service.PointService;
import com.cobasesys.module.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRuleRepository ruleRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationRecordRepository recordRepository;
    private final JavaMailSender mailSender;

    @Value("${cobasesys.notification.low-balance-threshold:1000}")
    private long lowBalanceThreshold;

    @Value("${spring.mail.username:noreply@example.com}")
    private String mailFrom;

    // ==================== Event Listeners ====================

    @Async("notificationExecutor")
    @EventListener
    public void onWalletChange(WalletService.WalletChangeEvent event) {
        if (event.direction() == -1 && event.newBalance() <= lowBalanceThreshold) {
            triggerNotification(event.tenantId(), event.userId(), "low_balance",
                    Map.of("balance", String.valueOf(event.newBalance() / 100.0),
                            "threshold", String.valueOf(lowBalanceThreshold / 100.0)));
        }
        if (event.direction() == 1) {
            triggerNotification(event.tenantId(), event.userId(), "recharge_success",
                    Map.of("amount", String.valueOf(event.amount() / 100.0),
                            "balance", String.valueOf(event.newBalance() / 100.0)));
        }
    }

    @Async("notificationExecutor")
    @EventListener
    public void onPointChange(PointService.PointChangeEvent event) {
        // Currently no automatic point notification triggers; can be extended
    }

    // ==================== Core Logic ====================

    public void triggerNotification(Long tenantId, String userId, String triggerType, Map<String, String> variables) {
        List<NotificationRule> rules = ruleRepository
                .findByTenantIdAndTriggerTypeAndStatus(tenantId, triggerType, 1);

        for (NotificationRule rule : rules) {
            if (rule.getThresholdValue() != null && "low_balance".equals(triggerType)) {
                Long balance = variables.containsKey("balance")
                        ? (long) (Double.parseDouble(variables.get("balance")) * 100) : 0;
                if (balance > rule.getThresholdValue()) continue;
            }

            try {
                String content = "";
                String subject = "";
                if (rule.getTemplateId() != null) {
                    NotificationTemplate template = templateRepository.findById(rule.getTemplateId()).orElse(null);
                    if (template != null) {
                        content = renderTemplate(template.getContent(), variables);
                        subject = template.getSubject() != null
                                ? renderTemplate(template.getSubject(), variables) : "";
                    }
                }
                if (content.isEmpty()) {
                    content = buildDefaultContent(triggerType, variables);
                    subject = buildDefaultSubject(triggerType);
                }

                NotificationRecord record = new NotificationRecord();
                record.setTenantId(tenantId);
                record.setUserId(userId);
                record.setRuleId(rule.getId());
                record.setChannel(rule.getChannel());
                record.setSubject(subject);
                record.setContent(content);

                sendNotification(record);
                recordRepository.save(record);
            } catch (Exception e) {
                log.error("Failed to send notification for rule {}: {}", rule.getId(), e.getMessage());
            }
        }
    }

    private void sendNotification(NotificationRecord record) {
        try {
            switch (record.getChannel()) {
                case "email" -> sendEmail(record);
                case "in_app" -> {
                    record.setStatus(1);
                    record.setSentAt(LocalDateTime.now());
                }
                default -> {
                    record.setStatus(1);
                    record.setSentAt(LocalDateTime.now());
                    log.info("Notification [{}] sent to user {}: {}",
                            record.getChannel(), record.getUserId(), record.getContent());
                }
            }
        } catch (Exception e) {
            record.setStatus(2);
            record.setErrorMessage(e.getMessage());
            log.error("Notification send failed: {}", e.getMessage());
        }
    }

    private void sendEmail(NotificationRecord record) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(record.getRecipient());
            message.setSubject(record.getSubject());
            message.setText(record.getContent());
            mailSender.send(message);
            record.setStatus(1);
            record.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            record.setStatus(2);
            record.setErrorMessage(e.getMessage());
        }
    }

    private String renderTemplate(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private String buildDefaultContent(String triggerType, Map<String, String> variables) {
        return switch (triggerType) {
            case "low_balance" -> "您的账户余额已不足 " + variables.getOrDefault("balance", "0") + " 元，请及时充值。";
            case "recharge_success" -> "充值成功！充值金额：" + variables.getOrDefault("amount", "0")
                    + " 元，当前余额：" + variables.getOrDefault("balance", "0") + " 元。";
            case "level_upgrade" -> "恭喜您升级到 " + variables.getOrDefault("levelName", "") + " 会员等级！";
            default -> "系统通知";
        };
    }

    private String buildDefaultSubject(String triggerType) {
        return switch (triggerType) {
            case "low_balance" -> "余额不足提醒";
            case "recharge_success" -> "充值成功通知";
            case "level_upgrade" -> "会员升级通知";
            default -> "系统通知";
        };
    }

    // ==================== Admin API ====================

    @Transactional
    public NotificationDTO.TemplateResponse createTemplate(NotificationDTO.TemplateCreateRequest request) {
        NotificationTemplate template = new NotificationTemplate();
        BeanUtils.copyProperties(request, template);
        template.setTenantId(TenantContext.requireTenantId());
        templateRepository.save(template);
        return toTemplateResponse(template);
    }

    @Transactional
    public NotificationDTO.TemplateResponse updateTemplate(Long id, NotificationDTO.TemplateUpdateRequest request) {
        NotificationTemplate template = templateRepository.findById(id).orElseThrow();
        if (request.getTemplateName() != null) template.setTemplateName(request.getTemplateName());
        if (request.getSubject() != null) template.setSubject(request.getSubject());
        if (request.getContent() != null) template.setContent(request.getContent());
        if (request.getStatus() != null) template.setStatus(request.getStatus());
        templateRepository.save(template);
        return toTemplateResponse(template);
    }

    public PageResult<NotificationDTO.TemplateResponse> listTemplates(Pageable pageable) {
        Page<NotificationTemplate> page = templateRepository.findByTenantId(
                TenantContext.requireTenantId(), pageable);
        return PageResult.from(page.map(this::toTemplateResponse));
    }

    @Transactional
    public NotificationDTO.RuleResponse createRule(NotificationDTO.RuleCreateRequest request) {
        NotificationRule rule = new NotificationRule();
        BeanUtils.copyProperties(request, rule);
        rule.setTenantId(TenantContext.requireTenantId());
        ruleRepository.save(rule);
        return toRuleResponse(rule);
    }

    @Transactional
    public NotificationDTO.RuleResponse updateRule(Long id, NotificationDTO.RuleUpdateRequest request) {
        NotificationRule rule = ruleRepository.findById(id).orElseThrow();
        if (request.getRuleName() != null) rule.setRuleName(request.getRuleName());
        if (request.getTriggerType() != null) rule.setTriggerType(request.getTriggerType());
        if (request.getThresholdValue() != null) rule.setThresholdValue(request.getThresholdValue());
        if (request.getChannel() != null) rule.setChannel(request.getChannel());
        if (request.getTemplateId() != null) rule.setTemplateId(request.getTemplateId());
        if (request.getStatus() != null) rule.setStatus(request.getStatus());
        ruleRepository.save(rule);
        return toRuleResponse(rule);
    }

    public PageResult<NotificationDTO.RuleResponse> listRules(Pageable pageable) {
        Page<NotificationRule> page = ruleRepository.findByTenantId(
                TenantContext.requireTenantId(), pageable);
        return PageResult.from(page.map(this::toRuleResponse));
    }

    public PageResult<NotificationDTO.RecordResponse> listRecords(String userId, Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        Page<NotificationRecord> page = userId != null
                ? recordRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId, pageable)
                : recordRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        return PageResult.from(page.map(this::toRecordResponse));
    }

    private NotificationDTO.TemplateResponse toTemplateResponse(NotificationTemplate t) {
        NotificationDTO.TemplateResponse resp = new NotificationDTO.TemplateResponse();
        BeanUtils.copyProperties(t, resp);
        return resp;
    }

    private NotificationDTO.RuleResponse toRuleResponse(NotificationRule r) {
        NotificationDTO.RuleResponse resp = new NotificationDTO.RuleResponse();
        BeanUtils.copyProperties(r, resp);
        return resp;
    }

    private NotificationDTO.RecordResponse toRecordResponse(NotificationRecord r) {
        NotificationDTO.RecordResponse resp = new NotificationDTO.RecordResponse();
        BeanUtils.copyProperties(r, resp);
        resp.setStatusText(switch (r.getStatus()) {
            case 0 -> "待发送";
            case 1 -> "已发送";
            case 2 -> "发送失败";
            default -> "未知";
        });
        return resp;
    }
}
