package com.xhzb.nursing.domain.vo;

import lombok.Data;

/**
 * 家属小程序登录结果。
 */
@Data
public class MemberLoginVo
{
    /** 家属登录令牌。 */
    private String token;

    /** 家属昵称。 */
    private String nickName;

}
