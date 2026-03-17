package com.cobasesys.module.points.service;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.common.exception.ErrorCode;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.common.util.IdGenerator;
import com.cobasesys.module.points.dto.PointDTO;
import com.cobasesys.module.points.entity.PointAccount;
import com.cobasesys.module.points.entity.PointGiftApproval;
import com.cobasesys.module.points.entity.PointTransaction;
import com.cobasesys.module.points.repository.PointAccountRepository;
import com.cobasesys.module.points.repository.PointGiftApprovalRepository;
import com.cobasesys.module.points.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointGiftService {

    private final PointGiftApprovalRepository approvalRepository;
    private final PointAccountRepository accountRepository;
    private final PointTransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PointDTO.GiftApprovalResponse submitGiftApply(PointDTO.GiftApplyRequest req) {
        Long tenantId = TenantContext.requireTenantId();
        PointGiftApproval approval = new PointGiftApproval();
        approval.setTenantId(tenantId);
        approval.setCustomerId(req.getCustomerId());
        approval.setCustomerName(req.getCustomerName());
        approval.setPoints(req.getPoints());
        approval.setGiftReason(req.getGiftReason());
        approval.setSourceType(req.getSourceType() != null ? req.getSourceType() : "GIFT_MANUAL");
        approval.setApplicantId(req.getApplicantId());
        approval.setApplicantName(req.getApplicantName());
        approval.setApplyTime(LocalDateTime.now());
        approval.setStatus("PENDING");
        approvalRepository.save(approval);
        log.info("Point gift application submitted: {} points to customer {} by {}",
                req.getPoints(), req.getCustomerId(), req.getApplicantName());
        return toResp(approval);
    }

    @Transactional
    public PointDTO.GiftApprovalResponse approve(Long approvalId, String approverId,
                                                   String approverName, String remark) {
        PointGiftApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        if (!"PENDING".equals(approval.getStatus())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "该申请已处理");
        }

        approval.setStatus("APPROVED");
        approval.setApproverId(approverId);
        approval.setApproverName(approverName);
        approval.setApproveTime(LocalDateTime.now());
        approval.setApproveRemark(remark);

        Long tenantId = approval.getTenantId();
        PointAccount account = accountRepository.findByTenantIdAndUserId(tenantId, approval.getCustomerId())
                .orElseGet(() -> {
                    PointAccount a = new PointAccount();
                    a.setTenantId(tenantId);
                    a.setUserId(approval.getCustomerId());
                    return accountRepository.save(a);
                });

        int updated = accountRepository.earnPoints(account.getId(), approval.getPoints(), account.getVersion());
        if (updated == 0) {
            account = accountRepository.findById(account.getId()).orElseThrow();
            accountRepository.earnPoints(account.getId(), approval.getPoints(), account.getVersion());
        }

        PointTransaction tx = new PointTransaction();
        tx.setTenantId(tenantId);
        tx.setTransactionNo(IdGenerator.pointTransactionNo());
        tx.setAccountId(account.getId());
        tx.setSystemId(0L);
        tx.setActionId(0L);
        tx.setDirection(1);
        tx.setPoints(approval.getPoints());
        tx.setBalanceBefore(account.getBalance());
        tx.setBalanceAfter(account.getBalance() + approval.getPoints());
        tx.setSourceType(approval.getSourceType());
        tx.setRemark("赠送积分: " + approval.getGiftReason()
                + " (审批人: " + approverName + ")");
        tx.setIdempotentKey("gift_approval:" + approvalId);
        transactionRepository.save(tx);

        approval.setTransactionNo(tx.getTransactionNo());
        approvalRepository.save(approval);

        log.info("Point gift approved: {} points to customer {} (approval #{}, approved by {})",
                approval.getPoints(), approval.getCustomerId(), approvalId, approverName);

        eventPublisher.publishEvent(new PointService.PointChangeEvent(this,
                tenantId, approval.getCustomerId(), approval.getPoints(), 1,
                account.getBalance() + approval.getPoints()));

        return toResp(approval);
    }

    @Transactional
    public PointDTO.GiftApprovalResponse reject(Long approvalId, String approverId,
                                                  String approverName, String remark) {
        PointGiftApproval approval = approvalRepository.findById(approvalId)
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
        return toResp(approval);
    }

    public PageResult<PointDTO.GiftApprovalResponse> list(String status, Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        Page<PointGiftApproval> page = status != null
                ? approvalRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status, pageable)
                : approvalRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        return PageResult.from(page.map(this::toResp));
    }

    public PointDTO.GiftSummary getSummary() {
        Long tenantId = TenantContext.requireTenantId();
        PointDTO.GiftSummary summary = new PointDTO.GiftSummary();
        summary.setTotalApproved(approvalRepository.countByTenantIdAndStatus(tenantId, "APPROVED"));
        summary.setTotalPending(approvalRepository.countByTenantIdAndStatus(tenantId, "PENDING"));
        summary.setTotalGiftedPoints(approvalRepository.sumApprovedPoints(tenantId));
        summary.setManualGiftedPoints(approvalRepository.sumApprovedPointsBySourceType(tenantId, "GIFT_MANUAL"));
        summary.setOrderGiftedPoints(approvalRepository.sumApprovedPointsBySourceType(tenantId, "GIFT_ORDER"));
        summary.setActivityGiftedPoints(approvalRepository.sumApprovedPointsBySourceType(tenantId, "GIFT_ACTIVITY"));
        return summary;
    }

    public PageResult<PointDTO.TransactionResponse> listGiftTransactions(String sourceType, Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        String st = sourceType != null ? sourceType : "GIFT_MANUAL";
        Page<PointTransaction> page = transactionRepository
                .findByTenantIdAndSourceTypeOrderByCreatedAtDesc(tenantId, st, pageable);
        return PageResult.from(page.map(tx -> {
            PointDTO.TransactionResponse resp = new PointDTO.TransactionResponse();
            resp.setTransactionNo(tx.getTransactionNo());
            resp.setDirection(tx.getDirection());
            resp.setDirectionText(tx.getDirection() == 1 ? "收入" : "支出");
            resp.setPoints(tx.getPoints());
            resp.setBalanceAfter(tx.getBalanceAfter());
            resp.setSourceType(tx.getSourceType());
            resp.setSourceTypeText(PointDTO.sourceTypeText(tx.getSourceType()));
            resp.setRemark(tx.getRemark());
            resp.setCreatedAt(tx.getCreatedAt());
            return resp;
        }));
    }

    private PointDTO.GiftApprovalResponse toResp(PointGiftApproval a) {
        PointDTO.GiftApprovalResponse resp = new PointDTO.GiftApprovalResponse();
        BeanUtils.copyProperties(a, resp);
        resp.setStatusText(switch (a.getStatus()) {
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已驳回";
            default -> a.getStatus();
        });
        return resp;
    }
}
