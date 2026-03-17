package com.cobasesys.module.member.service;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.common.exception.ErrorCode;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.member.dto.MemberDTO;
import com.cobasesys.module.member.entity.MemberLevel;
import com.cobasesys.module.member.entity.UserMember;
import com.cobasesys.module.member.repository.MemberLevelRepository;
import com.cobasesys.module.member.repository.UserMemberRepository;
import com.cobasesys.module.points.service.PointService;
import com.cobasesys.module.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberLevelRepository levelRepository;
    private final UserMemberRepository userMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public MemberDTO.LevelResponse createLevel(MemberDTO.LevelCreateRequest request) {
        MemberLevel level = new MemberLevel();
        BeanUtils.copyProperties(request, level);
        level.setTenantId(TenantContext.requireTenantId());
        levelRepository.save(level);
        return toLevelResponse(level);
    }

    @Transactional
    public MemberDTO.LevelResponse updateLevel(Long id, MemberDTO.LevelUpdateRequest request) {
        MemberLevel level = levelRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.MEMBER_LEVEL_NOT_FOUND));
        if (request.getLevelName() != null) level.setLevelName(request.getLevelName());
        if (request.getLevelRank() != null) level.setLevelRank(request.getLevelRank());
        if (request.getMinPoints() != null) level.setMinPoints(request.getMinPoints());
        if (request.getMinConsumption() != null) level.setMinConsumption(request.getMinConsumption());
        if (request.getPointMultiplier() != null) level.setPointMultiplier(request.getPointMultiplier());
        if (request.getDiscountRate() != null) level.setDiscountRate(request.getDiscountRate());
        if (request.getIconUrl() != null) level.setIconUrl(request.getIconUrl());
        if (request.getDescription() != null) level.setDescription(request.getDescription());
        if (request.getStatus() != null) level.setStatus(request.getStatus());
        levelRepository.save(level);
        return toLevelResponse(level);
    }

    public PageResult<MemberDTO.LevelResponse> listLevels(Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        Page<MemberLevel> page = levelRepository.findByTenantIdOrderByLevelRankAsc(tenantId, pageable);
        return PageResult.from(page.map(this::toLevelResponse));
    }

    @Transactional
    public void deleteLevel(Long id) {
        levelRepository.deleteById(id);
    }

    public MemberDTO.UserMemberResponse getUserMember(Long tenantId, String userId) {
        UserMember userMember = userMemberRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElse(null);
        List<MemberLevel> levels = levelRepository.findByTenantIdAndStatusOrderByLevelRankAsc(tenantId, 1);

        if (levels.isEmpty()) {
            MemberDTO.UserMemberResponse resp = new MemberDTO.UserMemberResponse();
            resp.setUserId(userId);
            resp.setLevelName("无等级");
            return resp;
        }

        MemberLevel currentLevel = levels.get(0);
        if (userMember != null) {
            currentLevel = levels.stream()
                    .filter(l -> l.getId().equals(userMember.getLevelId()))
                    .findFirst().orElse(levels.get(0));
        }

        MemberDTO.UserMemberResponse resp = new MemberDTO.UserMemberResponse();
        resp.setUserId(userId);
        resp.setLevelCode(currentLevel.getLevelCode());
        resp.setLevelName(currentLevel.getLevelName());
        resp.setLevelRank(currentLevel.getLevelRank());
        resp.setPointMultiplier(currentLevel.getPointMultiplier());
        resp.setDiscountRate(currentLevel.getDiscountRate());
        if (userMember != null) {
            resp.setTotalPointsEarned(userMember.getTotalPointsEarned());
            resp.setTotalConsumption(userMember.getTotalConsumption());
            resp.setLevelUpdatedAt(userMember.getLevelUpdatedAt());
        }
        return resp;
    }

    public PageResult<MemberDTO.UserMemberResponse> listUserMembers(Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        Page<UserMember> page = userMemberRepository.findByTenantId(tenantId, pageable);
        List<MemberLevel> levels = levelRepository.findByTenantIdAndStatusOrderByLevelRankAsc(tenantId, 1);

        return PageResult.from(page.map(um -> {
            MemberLevel level = levels.stream()
                    .filter(l -> l.getId().equals(um.getLevelId()))
                    .findFirst().orElse(null);
            MemberDTO.UserMemberResponse resp = new MemberDTO.UserMemberResponse();
            resp.setUserId(um.getUserId());
            resp.setTotalPointsEarned(um.getTotalPointsEarned());
            resp.setTotalConsumption(um.getTotalConsumption());
            resp.setLevelUpdatedAt(um.getLevelUpdatedAt());
            if (level != null) {
                resp.setLevelCode(level.getLevelCode());
                resp.setLevelName(level.getLevelName());
                resp.setLevelRank(level.getLevelRank());
                resp.setPointMultiplier(level.getPointMultiplier());
                resp.setDiscountRate(level.getDiscountRate());
            }
            return resp;
        }));
    }

    @Async
    @EventListener
    @Transactional
    public void onPointChange(PointService.PointChangeEvent event) {
        if (event.direction() != 1) return;
        evaluateAndUpgrade(event.tenantId(), event.userId(), event.points(), 0);
    }

    @Async
    @EventListener
    @Transactional
    public void onWalletChange(WalletService.WalletChangeEvent event) {
        if (event.direction() != -1) return;
        evaluateAndUpgrade(event.tenantId(), event.userId(), 0, event.amount());
    }

    private void evaluateAndUpgrade(Long tenantId, String userId, long pointsDelta, long consumptionDelta) {
        List<MemberLevel> levels = levelRepository.findByTenantIdAndStatusOrderByLevelRankAsc(tenantId, 1);
        if (levels.isEmpty()) return;

        UserMember userMember = userMemberRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseGet(() -> {
                    UserMember um = new UserMember();
                    um.setTenantId(tenantId);
                    um.setUserId(userId);
                    um.setLevelId(levels.get(0).getId());
                    um.setTotalPointsEarned(0L);
                    um.setTotalConsumption(0L);
                    return userMemberRepository.save(um);
                });

        userMember.setTotalPointsEarned(userMember.getTotalPointsEarned() + pointsDelta);
        userMember.setTotalConsumption(userMember.getTotalConsumption() + consumptionDelta);

        MemberLevel bestLevel = levels.get(0);
        for (MemberLevel level : levels) {
            if (userMember.getTotalPointsEarned() >= level.getMinPoints()
                    && userMember.getTotalConsumption() >= level.getMinConsumption()) {
                bestLevel = level;
            }
        }

        if (!bestLevel.getId().equals(userMember.getLevelId())) {
            log.info("User {} upgraded to level {} ({})", userId, bestLevel.getLevelCode(), bestLevel.getLevelName());
            userMember.setLevelId(bestLevel.getId());
            userMember.setLevelUpdatedAt(LocalDateTime.now());
            eventPublisher.publishEvent(new MemberUpgradeEvent(this, tenantId, userId,
                    bestLevel.getLevelCode(), bestLevel.getLevelName(), bestLevel.getLevelRank()));
        }

        userMemberRepository.save(userMember);
    }

    public record MemberUpgradeEvent(Object source, Long tenantId, String userId,
                                      String levelCode, String levelName, int levelRank) {}

    private MemberDTO.LevelResponse toLevelResponse(MemberLevel level) {
        MemberDTO.LevelResponse resp = new MemberDTO.LevelResponse();
        BeanUtils.copyProperties(level, resp);
        return resp;
    }
}
