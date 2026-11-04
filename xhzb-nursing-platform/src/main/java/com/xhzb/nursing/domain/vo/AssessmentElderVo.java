package com.xhzb.nursing.domain.vo;

import lombok.Data;

/** 评估采集信息转入住字段。 */
@Data
public class AssessmentElderVo {
    /** 联系电话 */
    private String phone;
    /** 医疗费用支付方式 */
    private String medicalPaymentMethod;
    /** 民族 */
    private String nation;
    /** 文化程度 */
    private String educationLevel;
    /** 身份证号码 */
    private String idCardNo;
    /** 老人姓名 */
    private String name;
    /** 社保卡号 */
    private String socialSecurityCard;
    /** 居住情况 */
    private String livingSituation;
    /** 宗教信仰 */
    private String religiousBelief;
    /** 经济来源 */
    private String economicSource;
    /** 婚姻状况 */
    private String maritalStatus;
    /** 核心建议 */
    private Integer coreSuggestion;
}
