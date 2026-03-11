package com.cobasesys.module.invoice.service;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.common.exception.ErrorCode;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.common.util.IdGenerator;
import com.cobasesys.module.billing.entity.BillingOrder;
import com.cobasesys.module.billing.repository.BillingOrderRepository;
import com.cobasesys.module.invoice.dto.InvoiceDTO;
import com.cobasesys.module.invoice.entity.InvoiceApplication;
import com.cobasesys.module.invoice.entity.InvoiceApplicationOrder;
import com.cobasesys.module.invoice.repository.InvoiceApplicationOrderRepository;
import com.cobasesys.module.invoice.repository.InvoiceApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceApplicationRepository applicationRepository;
    private final InvoiceApplicationOrderRepository appOrderRepository;
    private final BillingOrderRepository orderRepository;
    private final InvoiceApiClient apiClient;
    private final JavaMailSender mailSender;

    @Value("${cobasesys.invoice.rate-limit.max-per-day:5}")
    private int maxPerDay;

    @Value("${cobasesys.invoice.rate-limit.min-interval-minutes:30}")
    private int minIntervalMinutes;

    @Value("${spring.mail.username:noreply@example.com}")
    private String mailFrom;

    @Transactional("billingTransactionManager")
    public InvoiceDTO.ApplicationResponse apply(InvoiceDTO.ApplyRequest req) {
        Long tenantId = TenantContext.requireTenantId();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayCount = applicationRepository.countByTenantIdAndCustomerIdAndCreatedAtAfter(
                tenantId, req.getCustomerId(), todayStart);
        if (todayCount >= maxPerDay) {
            throw new BizException(ErrorCode.RATE_LIMITED, "每天最多申请" + maxPerDay + "次开票");
        }

        LocalDateTime lastApply = applicationRepository.findLastApplyTime(tenantId, req.getCustomerId());
        if (lastApply != null && lastApply.plusMinutes(minIntervalMinutes).isAfter(LocalDateTime.now())) {
            throw new BizException(ErrorCode.RATE_LIMITED, "两次开票申请间隔至少" + minIntervalMinutes + "分钟");
        }

        if ("SPECIAL".equals(req.getInvoiceType())) {
            if (req.getBankName() == null || req.getBankAccount() == null
                    || req.getCompanyAddress() == null || req.getCompanyPhone() == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "专用发票必须填写开户银行、账号、公司地址和电话");
            }
        }

        long totalAmount = 0;
        for (Long orderId : req.getOrderIds()) {
            BillingOrder order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订单不存在: " + orderId));
            if (order.getPaymentStatus() != 1) {
                throw new BizException(ErrorCode.PARAM_INVALID, "订单未付款: " + order.getOrderNo());
            }
            if (order.getInvoiceStatus() != null && order.getInvoiceStatus() != 0) {
                throw new BizException(ErrorCode.PARAM_INVALID, "订单已申请开票: " + order.getOrderNo());
            }
            totalAmount += order.getActualAmount();
        }

        BigDecimal rate = new BigDecimal("0.06");
        long amountWithoutTax = BigDecimal.valueOf(totalAmount)
                .divide(rate.add(BigDecimal.ONE), 0, RoundingMode.FLOOR).longValue();
        long taxAmount = totalAmount - amountWithoutTax;

        InvoiceApplication app = new InvoiceApplication();
        BeanUtils.copyProperties(req, app);
        app.setTenantId(tenantId);
        app.setApplicationNo(IdGenerator.generate("INV"));
        app.setTotalAmount(totalAmount);
        app.setTaxRate(rate);
        app.setTaxAmount(taxAmount);
        app.setAmountWithoutTax(amountWithoutTax);
        app.setItemMode("DEFAULT");
        app.setStatus("PENDING");
        applicationRepository.save(app);

        for (Long orderId : req.getOrderIds()) {
            BillingOrder order = orderRepository.findById(orderId).orElseThrow();
            InvoiceApplicationOrder appOrder = new InvoiceApplicationOrder();
            appOrder.setApplicationId(app.getId());
            appOrder.setOrderId(orderId);
            appOrder.setOrderNo(order.getOrderNo());
            appOrder.setOrderAmount(order.getActualAmount());
            appOrderRepository.save(appOrder);
            order.setInvoiceStatus(1);
            orderRepository.save(order);
        }

        return toResp(app);
    }

    @Transactional("billingTransactionManager")
    public InvoiceDTO.ApplicationResponse approve(Long id, InvoiceDTO.ApproveRequest req) {
        InvoiceApplication app = findAndVerify(id);
        if (!"PENDING".equals(app.getStatus())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "当前状态不允许审核");
        }

        app.setReviewerId(req.getReviewerId());
        app.setReviewerName(req.getReviewerName());
        app.setReviewTime(LocalDateTime.now());
        if (req.getItemMode() != null) app.setItemMode(req.getItemMode());
        app.setStatus("APPROVED");

        InvoiceApiClient.IssueRequest issueReq = new InvoiceApiClient.IssueRequest();
        issueReq.setBuyerName(app.getTitleName());
        issueReq.setBuyerTaxNo(app.getTaxNo());
        issueReq.setBuyerAddress(app.getCompanyAddress());
        issueReq.setBuyerPhone(app.getCompanyPhone());
        issueReq.setBuyerBankName(app.getBankName());
        issueReq.setBuyerBankAccount(app.getBankAccount());
        issueReq.setInvoiceType(app.getInvoiceType());
        issueReq.setTotalAmount(app.getTotalAmount());
        issueReq.setItemName("DEFAULT".equals(app.getItemMode()) ? "*信息技术服务*技术服务费" : "详见订单明细");
        issueReq.setTaxRate(app.getTaxRate().doubleValue());

        InvoiceApiClient.IssueResult result = apiClient.issueInvoice(issueReq);

        if (result.isSuccess()) {
            app.setStatus("ISSUED");
            app.setInvoiceCode(result.getInvoiceCode());
            app.setInvoiceNumber(result.getInvoiceNumber());
            app.setInvoiceDate(result.getInvoiceDate());
            app.setPdfUrl(result.getPdfUrl());
            app.setApiRequestId(result.getRequestId());
            app.setApiResponse(result.getRawResponse());

            List<InvoiceApplicationOrder> orders = appOrderRepository.findByApplicationId(id);
            for (InvoiceApplicationOrder ao : orders) {
                orderRepository.findById(ao.getOrderId()).ifPresent(o -> {
                    o.setInvoiceStatus(2);
                    orderRepository.save(o);
                });
            }

            applicationRepository.save(app);
            sendEmailNotification(app);
        } else {
            app.setApiResponse(result.getRawResponse());
            applicationRepository.save(app);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "开票失败: " + result.getErrorMessage());
        }

        return toResp(app);
    }

    @Transactional("billingTransactionManager")
    public InvoiceDTO.ApplicationResponse reject(Long id, InvoiceDTO.RejectRequest req) {
        InvoiceApplication app = findAndVerify(id);
        if (!"PENDING".equals(app.getStatus())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "当前状态不允许驳回");
        }
        app.setStatus("REJECTED");
        app.setReviewerId(req.getReviewerId());
        app.setReviewerName(req.getReviewerName());
        app.setReviewTime(LocalDateTime.now());
        app.setRejectReason(req.getRejectReason());
        applicationRepository.save(app);

        List<InvoiceApplicationOrder> orders = appOrderRepository.findByApplicationId(id);
        for (InvoiceApplicationOrder ao : orders) {
            orderRepository.findById(ao.getOrderId()).ifPresent(o -> {
                o.setInvoiceStatus(0);
                orderRepository.save(o);
            });
        }
        return toResp(app);
    }

    @Transactional("billingTransactionManager")
    public InvoiceDTO.ApplicationResponse voidInvoice(Long id, InvoiceDTO.VoidRequest req) {
        InvoiceApplication app = findAndVerify(id);
        if (!"ISSUED".equals(app.getStatus())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "只有已开具的发票才能红冲");
        }

        boolean success = apiClient.voidInvoice(app.getInvoiceCode(), app.getInvoiceNumber(), req.getVoidReason());
        if (!success) throw new BizException(ErrorCode.SYSTEM_ERROR, "红冲API调用失败");

        app.setStatus("VOIDED");
        app.setVoidReason(req.getVoidReason());
        app.setVoidTime(LocalDateTime.now());
        applicationRepository.save(app);

        List<InvoiceApplicationOrder> orders = appOrderRepository.findByApplicationId(id);
        for (InvoiceApplicationOrder ao : orders) {
            orderRepository.findById(ao.getOrderId()).ifPresent(o -> {
                o.setInvoiceStatus(0);
                orderRepository.save(o);
            });
        }
        return toResp(app);
    }

    public PageResult<InvoiceDTO.ApplicationResponse> list(String customerId, String status, Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        Page<InvoiceApplication> page;
        if (customerId != null) {
            page = applicationRepository.findByTenantIdAndCustomerIdOrderByCreatedAtDesc(tenantId, customerId, pageable);
        } else if (status != null) {
            page = applicationRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status, pageable);
        } else {
            page = applicationRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        }
        return PageResult.from(page.map(this::toResp));
    }

    public InvoiceDTO.ApplicationResponse getById(Long id) {
        return toResp(findAndVerify(id));
    }

    public InvoiceDTO.Statistics getStatistics() {
        Long tenantId = TenantContext.requireTenantId();
        InvoiceDTO.Statistics stats = new InvoiceDTO.Statistics();
        stats.setTotalIssued(applicationRepository.countByTenantIdAndStatus(tenantId, "ISSUED"));
        stats.setTotalPending(applicationRepository.countByTenantIdAndStatus(tenantId, "PENDING"));
        stats.setTotalRejected(applicationRepository.countByTenantIdAndStatus(tenantId, "REJECTED"));
        stats.setTotalVoided(applicationRepository.countByTenantIdAndStatus(tenantId, "VOIDED"));
        stats.setTotalIssuedAmount(applicationRepository.sumIssuedAmount(tenantId));
        if (stats.getTotalIssuedAmount() != null) {
            stats.setTotalIssuedAmountDisplay(InvoiceDTO.formatAmount(stats.getTotalIssuedAmount()));
        }
        return stats;
    }

    public void resendEmail(Long id) {
        InvoiceApplication app = findAndVerify(id);
        if (!"ISSUED".equals(app.getStatus())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "只有已开具的发票才能发送邮件");
        }
        sendEmailNotification(app);
    }

    @Async
    void sendEmailNotification(InvoiceApplication app) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(mailFrom);
            msg.setTo(app.getReceiverEmail());
            msg.setSubject("您的电子发票已开具 - " + app.getInvoiceNumber());
            msg.setText("尊敬的 " + app.getCustomerName() + ":\n\n"
                    + "您申请的电子发票已成功开具:\n"
                    + "  发票类型: " + InvoiceDTO.invoiceTypeText(app.getInvoiceType()) + "\n"
                    + "  发票号码: " + app.getInvoiceNumber() + "\n"
                    + "  开票日期: " + app.getInvoiceDate() + "\n"
                    + "  发票金额: " + InvoiceDTO.formatAmount(app.getTotalAmount()) + "\n\n"
                    + "发票PDF下载: " + app.getPdfUrl() + "\n\n"
                    + "CoBaseSys 运营团队");
            mailSender.send(msg);
            app.setEmailSent(1);
            app.setEmailSentAt(LocalDateTime.now());
            applicationRepository.save(app);
        } catch (Exception e) {
            log.error("Failed to send invoice email to {}: {}", app.getReceiverEmail(), e.getMessage());
        }
    }

    private InvoiceApplication findAndVerify(Long id) {
        InvoiceApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(app.getTenantId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问");
        }
        return app;
    }

    private InvoiceDTO.ApplicationResponse toResp(InvoiceApplication app) {
        InvoiceDTO.ApplicationResponse resp = new InvoiceDTO.ApplicationResponse();
        BeanUtils.copyProperties(app, resp);
        resp.setStatusText(InvoiceDTO.statusText(app.getStatus()));
        resp.setInvoiceTypeText(InvoiceDTO.invoiceTypeText(app.getInvoiceType()));
        resp.setTotalAmountDisplay(InvoiceDTO.formatAmount(app.getTotalAmount()));
        List<InvoiceApplicationOrder> orders = appOrderRepository.findByApplicationId(app.getId());
        resp.setOrders(orders.stream().map(o -> {
            InvoiceDTO.OrderItem item = new InvoiceDTO.OrderItem();
            item.setOrderId(o.getOrderId());
            item.setOrderNo(o.getOrderNo());
            item.setOrderAmount(o.getOrderAmount());
            item.setOrderAmountDisplay(InvoiceDTO.formatAmount(o.getOrderAmount()));
            return item;
        }).toList());
        return resp;
    }
}
