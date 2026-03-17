package com.cobasesys.module.system.service;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.common.exception.ErrorCode;
import com.cobasesys.module.system.dto.TenantDTO;
import com.cobasesys.module.system.entity.Tenant;
import com.cobasesys.module.system.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional
    public TenantDTO.Response create(TenantDTO.CreateRequest request) {
        if (tenantRepository.existsByTenantCode(request.getTenantCode())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "租户编码已存在");
        }
        Tenant tenant = new Tenant();
        BeanUtils.copyProperties(request, tenant);
        tenantRepository.save(tenant);
        return toResponse(tenant);
    }

    @Transactional
    public TenantDTO.Response update(Long id, TenantDTO.UpdateRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.TENANT_NOT_FOUND));
        if (request.getTenantName() != null) tenant.setTenantName(request.getTenantName());
        if (request.getContactName() != null) tenant.setContactName(request.getContactName());
        if (request.getContactPhone() != null) tenant.setContactPhone(request.getContactPhone());
        if (request.getContactEmail() != null) tenant.setContactEmail(request.getContactEmail());
        if (request.getStatus() != null) tenant.setStatus(request.getStatus());
        tenantRepository.save(tenant);
        return toResponse(tenant);
    }

    public Page<TenantDTO.Response> list(Pageable pageable) {
        return tenantRepository.findAll(pageable).map(this::toResponse);
    }

    public TenantDTO.Response getById(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.TENANT_NOT_FOUND));
        return toResponse(tenant);
    }

    @Transactional
    public void delete(Long id) {
        tenantRepository.deleteById(id);
    }

    private TenantDTO.Response toResponse(Tenant tenant) {
        TenantDTO.Response resp = new TenantDTO.Response();
        BeanUtils.copyProperties(tenant, resp);
        return resp;
    }
}
