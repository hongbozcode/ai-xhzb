package com.xhzb.nursing.controller.member;

import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.nursing.domain.dto.FamilyMemberElderBindDto;
import com.xhzb.nursing.domain.dto.MemberLoginDto;
import com.xhzb.nursing.service.IFamilyMemberElderService;
import com.xhzb.nursing.service.IFamilyMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 家属小程序用户接口，登录入口由现有 /member/** 匿名规则放行。
 */
@RestController
@RequestMapping("/member/user")
@Tag(name = "家属小程序用户")
public class MemberUserController
{
    @Autowired
    private IFamilyMemberService familyMemberService;

    @Autowired
    private IFamilyMemberElderService familyMemberElderService;

    /**
     * 使用微信登录凭证及手机号授权凭证登录。
     *
     * @param loginDto 登录参数
     * @return 令牌与家属昵称
     */
    @PostMapping("/login")
    @Operation(summary = "微信小程序登录")
    public AjaxResult login(@Valid @RequestBody MemberLoginDto loginDto)
    {
        return AjaxResult.success(familyMemberService.login(loginDto)).put("operationTime", LocalDateTime.now());
    }

    /** 为当前家属绑定老人。 */
    @PostMapping("/add")
    @Operation(summary = "绑定家人")
    public AjaxResult add(@Valid @RequestBody FamilyMemberElderBindDto bindDto)
    {
        familyMemberElderService.bindFamily(bindDto);
        return AjaxResult.success();
    }

    /** 查询当前家属绑定的全部老人。 */
    @GetMapping("/my")
    @Operation(summary = "查询家人列表")
    public AjaxResult my()
    {
        return AjaxResult.success(familyMemberElderService.listMyFamily());
    }

    /** 分页查询当前家属绑定的老人详情。 */
    @GetMapping("/list-by-page")
    @Operation(summary = "分页查询家人列表")
    public AjaxResult listByPage(@RequestParam(defaultValue = "1") int pageNum,
                                @RequestParam(defaultValue = "10") int pageSize)
    {
        return AjaxResult.success(familyMemberElderService.listMyFamilyByPage(pageNum, pageSize));
    }
}
