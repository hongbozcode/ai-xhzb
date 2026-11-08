package com.xhzb.nursing.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

/**
 * 小程序绑定家人参数。
 */
@Data
public class FamilyMemberElderBindDto
{
    /** 老人身份证号。 */
    @NotBlank(message = "身份证号不能为空")
    @Size(max = 20, message = "身份证号不能超过20个字符")
    @ToString.Exclude
    private String idCard;

    /** 老人姓名。 */
    @NotBlank(message = "老人姓名不能为空")
    @Size(max = 255, message = "老人姓名不能超过255个字符")
    private String name;

    /** 家属对老人的称呼。 */
    @Size(max = 255, message = "称呼不能超过255个字符")
    private String remark;
}
