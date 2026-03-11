package com.cobasesys.module.billing.event;

public class BillingEvents {

    public record OrderPaidEvent(Object source, Long tenantId, String customerId,
                                  String orderNo, long actualAmount) {}

    public record SubscriptionActivatedEvent(Object source, Long tenantId, String customerId,
                                               String subscriptionNo, String sourceName) {}

    public record SubscriptionExpiredEvent(Object source, Long tenantId, String customerId,
                                             String subscriptionNo, String sourceName) {}

    public record TrialStartedEvent(Object source, Long tenantId, String customerId,
                                      String sourceName, int trialDays) {}

    public record TrialExpiredEvent(Object source, Long tenantId, String customerId,
                                      String sourceName) {}
}
