package com.cobasesys.module.billing;

import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.entity.*;
import com.cobasesys.module.billing.repository.*;
import com.cobasesys.module.billing.service.PriceCalculator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceCalculatorTest {

    @Mock private BillingProductRepository productRepository;
    @Mock private BillingPackageRepository packageRepository;
    @Mock private BillingPricingPlanRepository pricingPlanRepository;
    @Mock private BillingDiscountRuleRepository discountRuleRepository;
    @Mock private BillingGiftRuleRepository giftRuleRepository;

    @InjectMocks private PriceCalculator calculator;

    @BeforeEach
    void setUp() { TenantContext.setTenantId(1L); }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    private BillingProduct createProduct(Long id, String name, String model) {
        BillingProduct p = new BillingProduct();
        p.setId(id); p.setProductName(name); p.setPricingModel(model);
        p.setPointsPayable(0); p.setMaxPointsRatio(BigDecimal.ZERO);
        p.setPointsExchangeRate(BigDecimal.ZERO);
        return p;
    }

    private BillingPricingPlan createPlan(String model, long price) {
        BillingPricingPlan plan = new BillingPricingPlan();
        plan.setPricingModel(model);
        plan.setPeriodPrice(price); plan.setUnitPrice(price);
        plan.setRentalPrice(price); plan.setSpaceUnitPrice(price);
        plan.setOneTimePrice(price);
        plan.setSoftwareFee(price); plan.setAnnualServiceFee(price / 2);
        return plan;
    }

    @Test
    void calculateSubscription_basicPrice() {
        BillingProduct product = createProduct(1L, "产品A", "SUBSCRIPTION");
        BillingPricingPlan plan = createPlan("SUBSCRIPTION", 10000);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(pricingPlanRepository.findActivePlans(eq("PRODUCT"), eq(1L), any())).thenReturn(List.of(plan));
        when(discountRuleRepository.findActiveRules(eq(1L), any())).thenReturn(List.of());
        when(giftRuleRepository.findActiveRules(eq(1L), any())).thenReturn(List.of());

        BillingDTO.PriceCalculateReq req = new BillingDTO.PriceCalculateReq();
        req.setCustomerId("C1");
        BillingDTO.OrderItemReq item = new BillingDTO.OrderItemReq();
        item.setItemType("PRODUCT"); item.setItemId(1L); item.setQuantity(2); item.setPeriodCount(1);
        req.setItems(List.of(item));
        req.setUsePoints(false);

        BillingDTO.PriceCalculateResp resp = calculator.calculate(req);
        assertEquals(20000, resp.getTotalAmount());
        assertEquals(0, resp.getDiscountAmount());
        assertEquals(20000, resp.getActualAmount());
    }

    @Test
    void calculateWithProductDiscount() {
        BillingProduct product = createProduct(1L, "产品A", "SUBSCRIPTION");
        BillingPricingPlan plan = createPlan("SUBSCRIPTION", 10000);

        BillingDiscountRule discount = new BillingDiscountRule();
        discount.setDiscountType("PRODUCT_DISCOUNT");
        discount.setTargetType("PRODUCT"); discount.setTargetId(1L);
        discount.setMinQuantity(1); discount.setDiscountRate(BigDecimal.valueOf(0.90));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(pricingPlanRepository.findActivePlans(eq("PRODUCT"), eq(1L), any())).thenReturn(List.of(plan));
        when(discountRuleRepository.findActiveRules(eq(1L), any())).thenReturn(List.of(discount));
        when(giftRuleRepository.findActiveRules(eq(1L), any())).thenReturn(List.of());

        BillingDTO.PriceCalculateReq req = new BillingDTO.PriceCalculateReq();
        req.setCustomerId("C1");
        BillingDTO.OrderItemReq item = new BillingDTO.OrderItemReq();
        item.setItemType("PRODUCT"); item.setItemId(1L); item.setQuantity(1); item.setPeriodCount(1);
        req.setItems(List.of(item));
        req.setUsePoints(false);

        BillingDTO.PriceCalculateResp resp = calculator.calculate(req);
        assertEquals(10000, resp.getTotalAmount());
        assertEquals(1000, resp.getDiscountAmount());
        assertEquals(9000, resp.getActualAmount());
    }

    @Test
    void calculateWithAmountDiscount() {
        BillingProduct product = createProduct(1L, "产品A", "ONE_TIME");
        BillingPricingPlan plan = createPlan("ONE_TIME", 50000);

        BillingDiscountRule amountDiscount = new BillingDiscountRule();
        amountDiscount.setDiscountType("AMOUNT_DISCOUNT");
        amountDiscount.setThresholdAmount(40000L);
        amountDiscount.setDiscountRate(BigDecimal.valueOf(0.95));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(pricingPlanRepository.findActivePlans(eq("PRODUCT"), eq(1L), any())).thenReturn(List.of(plan));
        when(discountRuleRepository.findActiveRules(eq(1L), any())).thenReturn(List.of(amountDiscount));
        when(giftRuleRepository.findActiveRules(eq(1L), any())).thenReturn(List.of());

        BillingDTO.PriceCalculateReq req = new BillingDTO.PriceCalculateReq();
        req.setCustomerId("C1");
        BillingDTO.OrderItemReq item = new BillingDTO.OrderItemReq();
        item.setItemType("PRODUCT"); item.setItemId(1L); item.setQuantity(1); item.setPeriodCount(1);
        req.setItems(List.of(item));
        req.setUsePoints(false);

        BillingDTO.PriceCalculateResp resp = calculator.calculate(req);
        assertEquals(50000, resp.getTotalAmount());
        assertTrue(resp.getDiscountAmount() > 0);
        assertTrue(resp.getActualAmount() < 50000);
    }

    @Test
    void calculateWithGift() {
        BillingProduct product = createProduct(1L, "企业版用户", "SUBSCRIPTION");
        BillingPricingPlan plan = createPlan("SUBSCRIPTION", 2000);

        BillingGiftRule giftRule = new BillingGiftRule();
        giftRule.setConditionType("BUY_PRODUCT");
        giftRule.setConditionTargetType("PRODUCT"); giftRule.setConditionTargetId(1L);
        giftRule.setConditionQuantity(10);
        giftRule.setGiftType("PRODUCT"); giftRule.setGiftTargetId(1L);
        giftRule.setGiftQuantity(1); giftRule.setGiftPoints(0L);
        giftRule.setRuleName("买10送1");

        BillingProduct giftProduct = createProduct(1L, "企业版用户", "SUBSCRIPTION");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(pricingPlanRepository.findActivePlans(eq("PRODUCT"), eq(1L), any())).thenReturn(List.of(plan));
        when(discountRuleRepository.findActiveRules(eq(1L), any())).thenReturn(List.of());
        when(giftRuleRepository.findActiveRules(eq(1L), any())).thenReturn(List.of(giftRule));

        BillingDTO.PriceCalculateReq req = new BillingDTO.PriceCalculateReq();
        req.setCustomerId("C1");
        BillingDTO.OrderItemReq item = new BillingDTO.OrderItemReq();
        item.setItemType("PRODUCT"); item.setItemId(1L); item.setQuantity(10); item.setPeriodCount(1);
        req.setItems(List.of(item));
        req.setUsePoints(false);

        BillingDTO.PriceCalculateResp resp = calculator.calculate(req);
        assertEquals(20000, resp.getTotalAmount());
        assertFalse(resp.getGifts().isEmpty());
        assertEquals("买10送1", resp.getGifts().get(0).getRuleName());
    }

    @Test
    void calculateWithPointsDeduction() {
        BillingProduct product = createProduct(1L, "产品A", "SUBSCRIPTION");
        product.setPointsPayable(1);
        product.setMaxPointsRatio(BigDecimal.valueOf(0.50));
        product.setPointsExchangeRate(BigDecimal.valueOf(100));
        BillingPricingPlan plan = createPlan("SUBSCRIPTION", 20000);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(pricingPlanRepository.findActivePlans(eq("PRODUCT"), eq(1L), any())).thenReturn(List.of(plan));
        when(discountRuleRepository.findActiveRules(eq(1L), any())).thenReturn(List.of());
        when(giftRuleRepository.findActiveRules(eq(1L), any())).thenReturn(List.of());

        BillingDTO.PriceCalculateReq req = new BillingDTO.PriceCalculateReq();
        req.setCustomerId("C1");
        BillingDTO.OrderItemReq item = new BillingDTO.OrderItemReq();
        item.setItemType("PRODUCT"); item.setItemId(1L); item.setQuantity(1); item.setPeriodCount(1);
        req.setItems(List.of(item));
        req.setUsePoints(true);

        BillingDTO.PriceCalculateResp resp = calculator.calculate(req);
        assertEquals(20000, resp.getTotalAmount());
        assertEquals(10000, resp.getPointsDeductAmount());
        assertTrue(resp.getPointsNeeded() > 0);
        assertEquals(10000, resp.getActualAmount());
    }
}
