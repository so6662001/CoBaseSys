package com.cobasesys.module.billing.service;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.common.exception.ErrorCode;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.common.util.IdGenerator;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.entity.*;
import com.cobasesys.module.billing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingTrialService {

    private final BillingTrialRepository trialRepository;
    private final BillingSubscriptionRepository subscriptionRepository;
    private final BillingProductRepository productRepository;
    private final BillingPackageRepository packageRepository;
    private final BillingTrialExtendApprovalRepository approvalRepository;

    @Transactional("billingTransactionManager")
    public BillingDTO.TrialResp applyTrial(BillingDTO.TrialApplyReq req) {
        Long tenantId = TenantContext.requireTenantId();

        Optional<BillingTrial> existing = trialRepository
                .findByTenantIdAndCustomerIdAndSourceTypeAndSourceId(
                        tenantId, req.getCustomerId(), req.getSourceType(), req.getSourceId());
        if (existing.isPresent() && "ACTIVE".equals(existing.get().getStatus())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "该产品/套餐已有活跃试用");
        }

        String sourceName;
        int trialDays;
        if ("PRODUCT".equals(req.getSourceType())) {
            BillingProduct product = productRepository.findById(req.getSourceId())
                    .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
            if (product.getTrialEnabled() != 1) throw new BizException(ErrorCode.PARAM_INVALID, "该产品不支持试用");
            sourceName = product.getProductName();
            trialDays = product.getTrialDays();
        } else {
            BillingPackage pkg = packageRepository.findById(req.getSourceId())
                    .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
            if (pkg.getTrialEnabled() != 1) throw new BizException(ErrorCode.PARAM_INVALID, "该套餐不支持试用");
            sourceName = pkg.getPackageName();
            trialDays = pkg.getTrialDays();
        }

        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(trialDays);

        BillingSubscription sub = new BillingSubscription();
        sub.setTenantId(tenantId);
        sub.setCustomerId(req.getCustomerId());
        sub.setSubscriptionNo(IdGenerator.generate("SUB"));
        sub.setSourceType(req.getSourceType());
        sub.setSourceId(req.getSourceId());
        sub.setSourceName(sourceName);
        sub.setPricingModel("TRIAL");
        sub.setQuantity(1);
        sub.setStatus("TRIAL");
        sub.setIsTrial(1);
        sub.setStartDate(start);
        sub.setEndDate(end);
        sub.setTotalDays(trialDays);
        subscriptionRepository.save(sub);

        BillingTrial trial = existing.orElseGet(BillingTrial::new);
        trial.setTenantId(tenantId);
        trial.setCustomerId(req.getCustomerId());
        trial.setSourceType(req.getSourceType());
        trial.setSourceId(req.getSourceId());
        trial.setSourceName(sourceName);
        trial.setSubscriptionId(sub.getId());
        trial.setTrialDays(trialDays);
        trial.setStartDate(start);
        trial.setEndDate(end);
        trial.setStatus("ACTIVE");
        trial.setExtendCount(0);
        trial.setTotalExtendDays(0);
        trialRepository.save(trial);

        return toResp(trial);
    }

    @Transactional("billingTransactionManager")
    public BillingDTO.TrialExtendApprovalResp submitExtendApply(Long trialId, BillingDTO.TrialExtendReq req) {
        BillingTrial trial = trialRepository.findById(trialId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        int maxExtendDays;
        boolean extendEnabled;
        if ("PRODUCT".equals(trial.getSourceType())) {
            BillingProduct p = productRepository.findById(trial.getSourceId()).orElseThrow();
            extendEnabled = p.getTrialExtendEnabled() == 1;
            maxExtendDays = p.getTrialMaxExtendDays();
        } else {
            BillingPackage p = packageRepository.findById(trial.getSourceId()).orElseThrow();
            extendEnabled = p.getTrialExtendEnabled() == 1;
            maxExtendDays = p.getTrialMaxExtendDays();
        }

        if (!extendEnabled) throw new BizException(ErrorCode.PARAM_INVALID, "该产品/套餐不支持延长试用");
        if (trial.getTotalExtendDays() + req.getExtendDays() > maxExtendDays) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "超过最大延长天数限制(" + maxExtendDays + "天)，已延长" + trial.getTotalExtendDays() + "天");
        }

        BillingTrialExtendApproval approval = new BillingTrialExtendApproval();
        approval.setTenantId(trial.getTenantId());
        approval.setTrialId(trial.getId());
        approval.setSubscriptionId(trial.getSubscriptionId());
        approval.setCustomerId(trial.getCustomerId());
        approval.setSourceName(trial.getSourceName());
        approval.setExtendDays(req.getExtendDays());
        approval.setApplyReason(req.getApplyReason());
        approval.setApplicantId(req.getApplicantId());
        approval.setApplicantName(req.getApplicantName());
        approval.setApplyTime(LocalDateTime.now());
        approval.setStatus("PENDING");
        approvalRepository.save(approval);

        return toApprovalResp(approval);
    }

    @Transactional("billingTransactionManager")
    public BillingDTO.TrialExtendApprovalResp approve(Long approvalId, String approverId,
                                                        String approverName, String remark) {
        BillingTrialExtendApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        if (!"PENDING".equals(approval.getStatus())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "该申请已处理");
        }

        approval.setStatus("APPROVED");
        approval.setApproverId(approverId);
        approval.setApproverName(approverName);
        approval.setApproveTime(LocalDateTime.now());
        approval.setApproveRemark(remark);
        approvalRepository.save(approval);

        BillingTrial trial = trialRepository.findById(approval.getTrialId()).orElseThrow();
        trial.setEndDate(trial.getEndDate().plusDays(approval.getExtendDays()));
        trial.setExtendCount(trial.getExtendCount() + 1);
        trial.setTotalExtendDays(trial.getTotalExtendDays() + approval.getExtendDays());
        trialRepository.save(trial);

        BillingSubscription sub = subscriptionRepository.findById(trial.getSubscriptionId()).orElseThrow();
        sub.setEndDate(trial.getEndDate());
        sub.setTotalDays(sub.getTotalDays() + approval.getExtendDays());
        subscriptionRepository.save(sub);

        log.info("Trial {} extended by {} days (approved by {})", trial.getId(),
                approval.getExtendDays(), approverName);
        return toApprovalResp(approval);
    }

    @Transactional("billingTransactionManager")
    public BillingDTO.TrialExtendApprovalResp reject(Long approvalId, String approverId,
                                                       String approverName, String remark) {
        BillingTrialExtendApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        if (!"PENDING".equals(approval.getStatus())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "该申请已处理");
        }
        approval.setStatus("REJECTED");
        approval.setApproverId(approverId);
        approval.setApproverName(approverName);
        approval.setApproveTime(LocalDateTime.now());
        approval.setApproveRemark(remark);
        approvalRepository.save(approval);
        return toApprovalResp(approval);
    }

    public PageResult<BillingDTO.TrialResp> listTrials(String status, Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        var page = status != null
                ? trialRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                : trialRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        return PageResult.from(page.map(this::toResp));
    }

    public PageResult<BillingDTO.TrialExtendApprovalResp> listApprovals(String status, Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        var page = status != null
                ? approvalRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                : approvalRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        return PageResult.from(page.map(this::toApprovalResp));
    }

    public List<BillingDTO.TrialResp> listByCustomer(Long tenantId, String customerId) {
        return trialRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .stream().map(this::toResp).toList();
    }

    private BillingDTO.TrialResp toResp(BillingTrial t) {
        BillingDTO.TrialResp resp = new BillingDTO.TrialResp();
        BeanUtils.copyProperties(t, resp);
        return resp;
    }

    private BillingDTO.TrialExtendApprovalResp toApprovalResp(BillingTrialExtendApproval a) {
        BillingDTO.TrialExtendApprovalResp resp = new BillingDTO.TrialExtendApprovalResp();
        BeanUtils.copyProperties(a, resp);
        return resp;
    }
}
