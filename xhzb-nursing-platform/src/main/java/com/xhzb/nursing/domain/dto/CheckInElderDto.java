package com.xhzb.nursing.domain.dto;

import jakarta.validation.constraints.*;

/** 入住老人资料。 */
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
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIdCardNo() { return idCardNo; }
    public void setIdCardNo(String idCardNo) { this.idCardNo = idCardNo; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getSex() { return sex; }
    public void setSex(Integer sex) { this.sex = sex; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getIdCardNationalEmblemImg() { return idCardNationalEmblemImg; }
    public void setIdCardNationalEmblemImg(String idCardNationalEmblemImg) { this.idCardNationalEmblemImg = idCardNationalEmblemImg; }
    public String getIdCardPortraitImg() { return idCardPortraitImg; }
    public void setIdCardPortraitImg(String idCardPortraitImg) { this.idCardPortraitImg = idCardPortraitImg; }
    public String getNation() { return nation; }
    public void setNation(String nation) { this.nation = nation; }
    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }
    public String getSocialSecurityCard() { return socialSecurityCard; }
    public void setSocialSecurityCard(String socialSecurityCard) { this.socialSecurityCard = socialSecurityCard; }
    public String getLivingSituation() { return livingSituation; }
    public void setLivingSituation(String livingSituation) { this.livingSituation = livingSituation; }
    public String getReligiousBelief() { return religiousBelief; }
    public void setReligiousBelief(String religiousBelief) { this.religiousBelief = religiousBelief; }
    public String getEconomicSource() { return economicSource; }
    public void setEconomicSource(String economicSource) { this.economicSource = economicSource; }
    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }
    public String getMedicalPaymentMethod() { return medicalPaymentMethod; }
    public void setMedicalPaymentMethod(String medicalPaymentMethod) { this.medicalPaymentMethod = medicalPaymentMethod; }
    public String getCoreSuggestion() { return coreSuggestion; }
    public void setCoreSuggestion(String coreSuggestion) { this.coreSuggestion = coreSuggestion; }
}
