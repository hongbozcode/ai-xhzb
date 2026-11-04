package com.xhzb.nursing.domain.vo;

import lombok.Data;
import com.xhzb.nursing.domain.*;

/** 入住老人详情。 */
@Data
public class CheckInElderVo extends Elder {
    /** 按出生日期计算的年龄 */
    private Integer age;
}
