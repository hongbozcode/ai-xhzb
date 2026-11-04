package com.xhzb.nursing.domain.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

/** 入住老人资料。 */
@Data
public class CheckInElderDto {
    /** 老人姓名 */
    @NotBlank
    @Size(max = 50)
    private String name;
    /** 身份证号码 */
    @NotBlank
    @Pattern(regexp = "[0-9]{17}[0-9Xx]")
    private String idCardNo;
    /** 联系电话 */
    @NotBlank
    @Size(max = 50)
    private String phone;
    /** 性别，0女1男 */
    @Min(0)
    @Max(1)
    private Integer sex;
    /** 年龄 */
    @Min(0)
    @Max(150)
    private Integer age;
    /** 家庭住址 */
    @Size(max = 255)
    private String address;
    /** 出生日期 */
    @Size(max = 50)
    @Pattern(regexp = "[0-9]{4}-[0-9]{2}-[0-9]{2}")
    private String birthday;
    /** 头像 */
    @Size(max = 255)
    private String image;
    /** 身份证国徽面 */
    @Size(max = 255)
    private String idCardNationalEmblemImg;
    /** 身份证人像面 */
    @Size(max = 255)
    private String idCardPortraitImg;
    /** 民族 */
    @Size(max = 50)
    private String nation;
    /** 文化程度 */
    @Size(max = 50)
    private String educationLevel;
    /** 社保卡号 */
    @Size(max = 50)
    private String socialSecurityCard;
    /** 居住情况 */
    @Size(max = 50)
    private String livingSituation;
    /** 宗教信仰 */
    @Size(max = 50)
    private String religiousBelief;
    /** 经济来源 */
    @Size(max = 50)
    private String economicSource;
    /** 婚姻状况 */
    @Size(max = 50)
    private String maritalStatus;
    /** 医疗费用支付方式 */
    @Size(max = 50)
    private String medicalPaymentMethod;
    /** 核心建议 */
    @Size(max = 50)
    private String coreSuggestion;
}
