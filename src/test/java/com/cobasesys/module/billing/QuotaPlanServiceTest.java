package com.cobasesys.module.billing;

import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.billing.entity.BillingSubscription;
import com.cobasesys.module.billing.entity.BillingSubscriptionQuota;
import com.cobasesys.module.billing.repository.BillingSubscriptionQuotaRepository;
import com.cobasesys.module.billing.repository.BillingSubscriptionRepository;
import com.cobasesys.module.billing.service.QuotaPlanService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuotaPlanServiceTest {

    @Mock private BillingSubscriptionRepository subscriptionRepository;
    @Mock private BillingSubscriptionQuotaRepository quotaRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private QuotaPlanService service;

    @BeforeEach
    void setUp() { TenantContext.setTenantId(1L); }
    @AfterEach
    void tearDown() { TenantContext.clear(); }

    private BillingSubscription createSub(Long id, String quotaConfig) {
        BillingSubscription sub = new BillingSubscription();
        sub.setId(id); sub.setTenantId(1L); sub.setCustomerId("C1");
        sub.setPricingModel("QUOTA_PLAN"); sub.setQuotaConfig(quotaConfig);
        sub.setStartDate(LocalDate.now().withDayOfMonth(1));
        sub.setEndDate(LocalDate.now().withDayOfMonth(1).plusMonths(1));
        return sub;
    }

    @Test
    void checkQuota_withinLimit_shouldAllow() {
        BillingSubscription sub = createSub(1L, null);
        BillingSubscriptionQuota q = new BillingSubscriptionQuota();
        q.setId(1L); q.setDimension("customer_count"); q.setDimensionLabel("客户数");
        q.setQuotaLimit(50L); q.setQuotaUsed(35L); q.setVersion(0L);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(quotaRepository.findBySubscriptionIdAndDimensionAndPeriodStartAndPeriodEnd(
                eq(1L), eq("customer_count"), any(), any())).thenReturn(Optional.of(q));

        var result = service.checkQuota("C1", 1L, "customer_count");
        assertTrue(result.isAllowed());
        assertEquals(50, result.getLimit());
        assertEquals(35, result.getUsed());
        assertEquals(15, result.getRemaining());
    }

    @Test
    void checkQuota_atLimit_shouldDeny() {
        BillingSubscription sub = createSub(1L, null);
        BillingSubscriptionQuota q = new BillingSubscriptionQuota();
        q.setId(1L); q.setDimension("customer_count");
        q.setQuotaLimit(50L); q.setQuotaUsed(50L); q.setVersion(0L);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(quotaRepository.findBySubscriptionIdAndDimensionAndPeriodStartAndPeriodEnd(
                eq(1L), eq("customer_count"), any(), any())).thenReturn(Optional.of(q));

        var result = service.checkQuota("C1", 1L, "customer_count");
        assertFalse(result.isAllowed());
        assertEquals(0, result.getRemaining());
    }

    @Test
    void checkQuota_unlimited_shouldAllow() {
        BillingSubscription sub = createSub(1L, null);
        BillingSubscriptionQuota q = new BillingSubscriptionQuota();
        q.setId(1L); q.setDimension("customer_count");
        q.setQuotaLimit(-1L); q.setQuotaUsed(9999L); q.setVersion(0L);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(quotaRepository.findBySubscriptionIdAndDimensionAndPeriodStartAndPeriodEnd(
                eq(1L), eq("customer_count"), any(), any())).thenReturn(Optional.of(q));

        var result = service.checkQuota("C1", 1L, "customer_count");
        assertTrue(result.isAllowed());
        assertEquals(-1, result.getRemaining());
    }

    @Test
    void getAllQuotas_shouldReturnAll() {
        BillingSubscriptionQuota q1 = new BillingSubscriptionQuota();
        q1.setDimension("customer_count"); q1.setDimensionLabel("客户数");
        q1.setQuotaLimit(50L); q1.setQuotaUsed(30L); q1.setCustomerId("C1");

        BillingSubscriptionQuota q2 = new BillingSubscriptionQuota();
        q2.setDimension("delivery_count"); q2.setDimensionLabel("提货次数");
        q2.setQuotaLimit(200L); q2.setQuotaUsed(150L); q2.setCustomerId("C1");

        when(quotaRepository.findBySubscriptionId(1L)).thenReturn(List.of(q1, q2));

        var results = service.getAllQuotas(1L);
        assertEquals(2, results.size());
        assertEquals("customer_count", results.get(0).getDimension());
        assertTrue(results.get(0).isAllowed());
        assertEquals(20, results.get(0).getRemaining());
        assertEquals("delivery_count", results.get(1).getDimension());
        assertTrue(results.get(1).isAllowed());
        assertEquals(50, results.get(1).getRemaining());
    }

    @Test
    void initQuotas_shouldCreateRecords() {
        String config = "{\"quotas\":[{\"dimension\":\"customer_count\",\"label\":\"客户数\",\"limit\":50}," +
                "{\"dimension\":\"delivery_count\",\"label\":\"提货次数\",\"limit\":200}]}";
        BillingSubscription sub = createSub(1L, config);

        service.initQuotasForSubscription(sub);

        verify(quotaRepository, times(2)).save(any(BillingSubscriptionQuota.class));
    }

    @Test
    void initQuotas_nonQuotaPlan_shouldSkip() {
        BillingSubscription sub = createSub(1L, null);
        sub.setPricingModel("SUBSCRIPTION");

        service.initQuotasForSubscription(sub);

        verify(quotaRepository, never()).save(any());
    }
}
