package com.xhzb.nursing.domain.vo;

import com.xhzb.nursing.domain.*;

/** 入住老人详情。 */
public class CheckInElderVo extends Elder {
    /** 按出生日期计算的年龄 */
    private Integer age;
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
}
