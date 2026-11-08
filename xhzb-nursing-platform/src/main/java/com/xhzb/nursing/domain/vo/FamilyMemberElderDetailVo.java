package com.xhzb.nursing.domain.vo;

import lombok.Data;

/**
 * 小程序家人分页列表项。
 */
@Data
public class FamilyMemberElderDetailVo
{
    /** 绑定关系主键，按接口约定返回字符串。 */
    private String mid;

    /** 家属对老人的称呼。 */
    private String mremark;

    /** 老人主键，按接口约定返回字符串。 */
    private String elderId;

    /** 老人姓名。 */
    private String name;

    /** 老人头像地址。 */
    private String image;

    /** 床位编号。 */
    private String bedNumber;

    /** 房型名称。 */
    private String typeName;

    /** 随身手表的物联网设备 ID。 */
    private String iotId;

    /** 随身手表的设备名称。 */
    private String deviceName;

    /** 随身手表的产品标识。 */
    private String productKey;
}
