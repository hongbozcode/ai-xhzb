package com.xhzb.nursing.domain.vo;

import lombok.Data;

/**
 * 小程序家人绑定列表项。
 */
@Data
public class FamilyMemberElderVo
{
    /** 绑定关系主键。 */
    private Long id;

    /** 家属主键。 */
    private Long familyMemberId;

    /** 老人主键。 */
    private Long elderId;

    /** 老人姓名。 */
    private String elderName;
}
