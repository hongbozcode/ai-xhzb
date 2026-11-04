package com.xhzb.nursing.domain.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 家属小程序登录参数。
 */
@Data
public class MemberLoginDto
{
    /** 微信登录临时凭证，与手机号授权凭证不同。 */
    @NotBlank(message = "登录凭证不能为空")
    private String code;

    /** 前端微信昵称；注册时按业务规则生成昵称，不使用该值。 */
    @Size(max = 100, message = "微信昵称不能超过100个字符")
    private String nickName;

    /** 微信手机号授权临时凭证。 */
    @NotBlank(message = "手机号授权凭证不能为空")
    private String phoneCode;

}
