package com.cobasesys.module.billing.service;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.common.exception.ErrorCode;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.entity.BillingProduct;
import com.cobasesys.module.billing.repository.BillingProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingProductService {

    private final BillingProductRepository productRepository;

    @Transactional("billingTransactionManager")
    public BillingDTO.ProductResp create(BillingDTO.ProductCreateReq req) {
        Long tenantId = TenantContext.requireTenantId();
        if (productRepository.existsByTenantIdAndProductCode(tenantId, req.getProductCode())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "产品编码已存在");
        }
        BillingProduct product = new BillingProduct();
        BeanUtils.copyProperties(req, product);
        product.setTenantId(tenantId);
        productRepository.save(product);
        return toResp(product);
    }

    @Transactional("billingTransactionManager")
    public BillingDTO.ProductResp update(Long id, BillingDTO.ProductCreateReq req) {
        BillingProduct product = productRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        if (req.getProductName() != null) product.setProductName(req.getProductName());
        if (req.getCategory() != null) product.setCategory(req.getCategory());
        if (req.getDescription() != null) product.setDescription(req.getDescription());
        if (req.getIconUrl() != null) product.setIconUrl(req.getIconUrl());
        if (req.getPricingModel() != null) product.setPricingModel(req.getPricingModel());
        if (req.getTrialEnabled() != null) product.setTrialEnabled(req.getTrialEnabled());
        if (req.getTrialDays() != null) product.setTrialDays(req.getTrialDays());
        if (req.getTrialExtendEnabled() != null) product.setTrialExtendEnabled(req.getTrialExtendEnabled());
        if (req.getTrialMaxExtendDays() != null) product.setTrialMaxExtendDays(req.getTrialMaxExtendDays());
        if (req.getPointsPayable() != null) product.setPointsPayable(req.getPointsPayable());
        if (req.getMaxPointsRatio() != null) product.setMaxPointsRatio(req.getMaxPointsRatio());
        if (req.getPointsExchangeRate() != null) product.setPointsExchangeRate(req.getPointsExchangeRate());
        if (req.getSortOrder() != null) product.setSortOrder(req.getSortOrder());
        productRepository.save(product);
        return toResp(product);
    }

    public PageResult<BillingDTO.ProductResp> list(Integer status, Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        Page<BillingProduct> page = status != null
                ? productRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                : productRepository.findByTenantId(tenantId, pageable);
        return PageResult.from(page.map(this::toResp));
    }

    public BillingDTO.ProductResp getById(Long id) {
        return toResp(productRepository.findById(id).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND)));
    }

    @Transactional("billingTransactionManager")
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    private BillingDTO.ProductResp toResp(BillingProduct p) {
        BillingDTO.ProductResp resp = new BillingDTO.ProductResp();
        BeanUtils.copyProperties(p, resp);
        return resp;
    }
}
