package com.xhzb.nursing.domain;

import com.xhzb.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 家属与老人的绑定关系。
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class FamilyMemberElder extends BaseEntity
{
    /** 绑定关系主键。 */
    private Long id;

    /** 家属主键。 */
    private Long familyMemberId;

    /** 老人主键。 */
    private Long elderId;
}
