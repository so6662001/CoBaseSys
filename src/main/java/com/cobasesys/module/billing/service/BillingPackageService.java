package com.cobasesys.module.billing.service;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.common.exception.ErrorCode;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.entity.BillingPackage;
import com.cobasesys.module.billing.entity.BillingPackageItem;
import com.cobasesys.module.billing.entity.BillingProduct;
import com.cobasesys.module.billing.repository.BillingPackageItemRepository;
import com.cobasesys.module.billing.repository.BillingPackageRepository;
import com.cobasesys.module.billing.repository.BillingProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingPackageService {

    private final BillingPackageRepository packageRepository;
    private final BillingPackageItemRepository itemRepository;
    private final BillingProductRepository productRepository;

    @Transactional("billingTransactionManager")
    public BillingDTO.PackageResp create(BillingDTO.PackageCreateReq req) {
        Long tenantId = TenantContext.requireTenantId();
        BillingPackage pkg = new BillingPackage();
        BeanUtils.copyProperties(req, pkg);
        pkg.setTenantId(tenantId);
        packageRepository.save(pkg);

        if (req.getItems() != null) {
            for (BillingDTO.PackageItemReq itemReq : req.getItems()) {
                BillingPackageItem item = new BillingPackageItem();
                item.setPackageId(pkg.getId());
                item.setProductId(itemReq.getProductId());
                item.setQuantity(itemReq.getQuantity());
                item.setSortOrder(itemReq.getSortOrder() != null ? itemReq.getSortOrder() : 0);
                itemRepository.save(item);
            }
        }
        return toResp(pkg);
    }

    @Transactional("billingTransactionManager")
    public BillingDTO.PackageResp update(Long id, BillingDTO.PackageCreateReq req) {
        BillingPackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        if (req.getPackageName() != null) pkg.setPackageName(req.getPackageName());
        if (req.getDescription() != null) pkg.setDescription(req.getDescription());
        if (req.getPricingModel() != null) pkg.setPricingModel(req.getPricingModel());
        if (req.getTrialEnabled() != null) pkg.setTrialEnabled(req.getTrialEnabled());
        if (req.getTrialDays() != null) pkg.setTrialDays(req.getTrialDays());
        if (req.getPointsPayable() != null) pkg.setPointsPayable(req.getPointsPayable());
        if (req.getMaxPointsRatio() != null) pkg.setMaxPointsRatio(req.getMaxPointsRatio());
        if (req.getPointsExchangeRate() != null) pkg.setPointsExchangeRate(req.getPointsExchangeRate());
        packageRepository.save(pkg);
        return toResp(pkg);
    }

    public PageResult<BillingDTO.PackageResp> list(Integer status, Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        Page<BillingPackage> page = status != null
                ? packageRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                : packageRepository.findByTenantId(tenantId, pageable);
        return PageResult.from(page.map(this::toResp));
    }

    public BillingDTO.PackageResp getById(Long id) {
        return toResp(packageRepository.findById(id).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND)));
    }

    @Transactional("billingTransactionManager")
    public void delete(Long id) {
        itemRepository.deleteByPackageId(id);
        packageRepository.deleteById(id);
    }

    @Transactional("billingTransactionManager")
    public void addItem(Long packageId, BillingDTO.PackageItemReq req) {
        BillingPackageItem item = new BillingPackageItem();
        item.setPackageId(packageId);
        item.setProductId(req.getProductId());
        item.setQuantity(req.getQuantity());
        item.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        itemRepository.save(item);
    }

    @Transactional("billingTransactionManager")
    public void removeItem(Long itemId) {
        itemRepository.deleteById(itemId);
    }

    private BillingDTO.PackageResp toResp(BillingPackage pkg) {
        BillingDTO.PackageResp resp = new BillingDTO.PackageResp();
        BeanUtils.copyProperties(pkg, resp);
        List<BillingPackageItem> items = itemRepository.findByPackageIdOrderBySortOrderAsc(pkg.getId());
        resp.setItems(items.stream().map(item -> {
            BillingDTO.PackageItemResp ir = new BillingDTO.PackageItemResp();
            ir.setId(item.getId());
            ir.setProductId(item.getProductId());
            ir.setQuantity(item.getQuantity());
            productRepository.findById(item.getProductId())
                    .ifPresent(p -> ir.setProductName(p.getProductName()));
            return ir;
        }).toList());
        return resp;
    }
}
