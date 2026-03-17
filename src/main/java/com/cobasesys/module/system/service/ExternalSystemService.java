package com.cobasesys.module.system.service;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.common.exception.ErrorCode;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.common.util.IdGenerator;
import com.cobasesys.common.util.SignatureUtil;
import com.cobasesys.module.system.dto.ExternalSystemDTO;
import com.cobasesys.module.system.entity.ExternalSystem;
import com.cobasesys.module.system.repository.ExternalSystemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExternalSystemService {

    private final ExternalSystemRepository systemRepository;

    @Transactional
    public ExternalSystemDTO.DetailResponse create(ExternalSystemDTO.CreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        if (systemRepository.existsByTenantIdAndSystemCode(tenantId, request.getSystemCode())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "系统编码已存在");
        }

        ExternalSystem system = new ExternalSystem();
        BeanUtils.copyProperties(request, system);
        system.setTenantId(tenantId);
        system.setAppKey(IdGenerator.appKey());
        system.setAppSecret(SignatureUtil.generateAppSecret());
        if (request.getRateLimit() == null) {
            system.setRateLimit(1000);
        }
        systemRepository.save(system);
        return toDetailResponse(system);
    }

    @Transactional
    public ExternalSystemDTO.Response update(Long id, ExternalSystemDTO.UpdateRequest request) {
        ExternalSystem system = findById(id);
        if (request.getSystemName() != null) system.setSystemName(request.getSystemName());
        if (request.getDescription() != null) system.setDescription(request.getDescription());
        if (request.getCallbackUrl() != null) system.setCallbackUrl(request.getCallbackUrl());
        if (request.getIpWhitelist() != null) system.setIpWhitelist(request.getIpWhitelist());
        if (request.getRateLimit() != null) system.setRateLimit(request.getRateLimit());
        if (request.getStatus() != null) system.setStatus(request.getStatus());
        systemRepository.save(system);
        return toResponse(system);
    }

    public Page<ExternalSystemDTO.Response> list(Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        return systemRepository.findByTenantId(tenantId, pageable).map(this::toResponse);
    }

    public ExternalSystemDTO.Response getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public ExternalSystemDTO.DetailResponse resetSecret(Long id) {
        ExternalSystem system = findById(id);
        system.setAppSecret(SignatureUtil.generateAppSecret());
        systemRepository.save(system);
        return toDetailResponse(system);
    }

    @Transactional
    public void delete(Long id) {
        systemRepository.deleteById(id);
    }

    private ExternalSystem findById(Long id) {
        return systemRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.SYSTEM_NOT_FOUND));
    }

    private ExternalSystemDTO.Response toResponse(ExternalSystem system) {
        ExternalSystemDTO.Response resp = new ExternalSystemDTO.Response();
        BeanUtils.copyProperties(system, resp);
        return resp;
    }

    private ExternalSystemDTO.DetailResponse toDetailResponse(ExternalSystem system) {
        ExternalSystemDTO.DetailResponse resp = new ExternalSystemDTO.DetailResponse();
        BeanUtils.copyProperties(system, resp);
        return resp;
    }
}
