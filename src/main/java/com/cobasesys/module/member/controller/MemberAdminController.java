package com.cobasesys.module.member.controller;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.member.dto.MemberDTO;
import com.cobasesys.module.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "会员等级管理")
@RestController
@RequestMapping("/admin/members")
@RequiredArgsConstructor
public class MemberAdminController {

    private final MemberService memberService;

    @Operation(summary = "创建会员等级")
    @PostMapping("/levels")
    public ApiResponse<MemberDTO.LevelResponse> createLevel(
            @Valid @RequestBody MemberDTO.LevelCreateRequest request) {
        return ApiResponse.ok(memberService.createLevel(request));
    }

    @Operation(summary = "更新会员等级")
    @PutMapping("/levels/{id}")
    public ApiResponse<MemberDTO.LevelResponse> updateLevel(
            @PathVariable Long id, @Valid @RequestBody MemberDTO.LevelUpdateRequest request) {
        return ApiResponse.ok(memberService.updateLevel(id, request));
    }

    @Operation(summary = "会员等级列表")
    @GetMapping("/levels")
    public ApiResponse<PageResult<MemberDTO.LevelResponse>> listLevels(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(memberService.listLevels(PageRequest.of(page - 1, pageSize)));
    }

    @Operation(summary = "查询用户会员信息")
    @GetMapping("/users/{userId}")
    public ApiResponse<MemberDTO.UserMemberResponse> getUserMember(@PathVariable String userId) {
        return ApiResponse.ok(memberService.getUserMember(TenantContext.requireTenantId(), userId));
    }

    @Operation(summary = "会员用户列表")
    @GetMapping("/users")
    public ApiResponse<PageResult<MemberDTO.UserMemberResponse>> listUserMembers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(memberService.listUserMembers(PageRequest.of(page - 1, pageSize)));
    }
}
