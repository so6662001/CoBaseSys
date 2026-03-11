package com.cobasesys.module.billing.scheduler;

import com.cobasesys.module.billing.entity.BillingRenewalReminder;
import com.cobasesys.module.billing.entity.BillingSubscription;
import com.cobasesys.module.billing.repository.BillingRenewalReminderRepository;
import com.cobasesys.module.billing.repository.BillingSubscriptionRepository;
import com.cobasesys.module.system.entity.Tenant;
import com.cobasesys.module.system.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RenewalReminderScheduler {

    private final BillingSubscriptionRepository subscriptionRepository;
    private final BillingRenewalReminderRepository reminderRepository;
    private final TenantRepository tenantRepository;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(cron = "0 0 14 * * ?")
    public void dailyRenewalReminder() {
        String lockKey = "billing:reminder:daily:lock";
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofMinutes(30));
        if (!Boolean.TRUE.equals(locked)) return;

        try {
            log.info("Starting daily renewal reminder task");
            List<Tenant> tenants = tenantRepository.findAll();
            for (Tenant tenant : tenants) {
                processExpiringSubscriptions(tenant.getId());
                processExpiredSubscriptions(tenant.getId());
            }
            log.info("Daily renewal reminder task completed");
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional("billingTransactionManager")
    public void updateDaysUsed() {
        String lockKey = "billing:days-update:lock";
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofMinutes(10));
        if (!Boolean.TRUE.equals(locked)) return;
        try {
            int updated = subscriptionRepository.updateDaysUsedBatch();
            log.info("Updated days_used for {} subscriptions", updated);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private void processExpiringSubscriptions(Long tenantId) {
        LocalDate deadline = LocalDate.now().plusDays(15);
        List<BillingSubscription> subs = subscriptionRepository.findExpiringSoon(tenantId, deadline);

        for (BillingSubscription sub : subs) {
            LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            long count = reminderRepository.countTodayReminders(
                    sub.getId(), "PRE_EXPIRY", startOfDay, endOfDay);
            if (count > 0) continue;

            int daysLeft = (int) (sub.getEndDate().toEpochDay() - LocalDate.now().toEpochDay());

            BillingRenewalReminder reminder = new BillingRenewalReminder();
            reminder.setTenantId(tenantId);
            reminder.setSubscriptionId(sub.getId());
            reminder.setCustomerId(sub.getCustomerId());
            reminder.setReminderType(daysLeft > 0 ? "PRE_EXPIRY" : "POST_EXPIRY");
            reminder.setChannel("SMS");
            reminder.setContent(String.format("您的%s将于%s到期（剩余%d天），请及时续费。",
                    sub.getSourceName(), sub.getEndDate(), Math.max(daysLeft, 0)));
            reminder.setStatus(0);
            reminderRepository.save(reminder);

            log.info("Sent renewal reminder for subscription {} (customer: {}, days left: {})",
                    sub.getSubscriptionNo(), sub.getCustomerId(), daysLeft);
        }
    }

    private void processExpiredSubscriptions(Long tenantId) {
        List<BillingSubscription> expired = subscriptionRepository.findExpired(tenantId, LocalDate.now());
        for (BillingSubscription sub : expired) {
            if (!"EXPIRED".equals(sub.getStatus())) {
                sub.setStatus("EXPIRED");
                subscriptionRepository.save(sub);
                log.info("Subscription {} expired", sub.getSubscriptionNo());
            }
        }
    }
}
