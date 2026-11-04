package com.xhzb.nursing.domain;

import com.xhzb.common.annotation.Excel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.xhzb.common.core.domain.BaseEntity;

/**
 * 老人对象 elder
 * 
 * @author hongbozcode
 * @date 2026-11-03
 */
@Data
@Schema(description = "老人对象")
public class Elder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** id */
    @Schema(title = "id")
    private Long id;

    /** 名称 */
    @Schema(title = "名称")
    @Excel(name = "名称")
    private String name;

    /** 头像 */
    @Schema(title = "头像")
    @Excel(name = "头像")
    private String image;

    /** 身份证号 */
    @Schema(title = "身份证号")
    @Excel(name = "身份证号")
    private String idCardNo;

    /** 性别（0:女  1:男） */
    @Schema(title = "性别（0:女  1:男）")
    @Excel(name = "性别", readConverterExp = "0=:女,1=:男")
    private Integer sex;

    /** 状态（0:禁用，1:已入住 2:请假 3:已退住） */
    @Schema(title = "状态（0:禁用，1:已入住 2:请假 3:已退住）")
    @Excel(name = "状态", readConverterExp = "0=:禁用，1:已入住,2=:请假,3=:已退住")
    private Integer status;

    /** 手机号 */
    @Schema(title = "手机号")
    @Excel(name = "手机号")
    private String phone;

    /** 出生日期 */
    @Schema(title = "出生日期")
    @Excel(name = "出生日期")
    private String birthday;

    /** 家庭住址 */
    @Schema(title = "家庭住址")
    @Excel(name = "家庭住址")
    private String address;

    /** 身份证国徽面 */
    @Schema(title = "身份证国徽面")
    @Excel(name = "身份证国徽面")
    private String idCardNationalEmblemImg;

    /** 身份证人像面 */
    @Schema(title = "身份证人像面")
    @Excel(name = "身份证人像面")
    private String idCardPortraitImg;

    /** 床位编号 */
    @Schema(title = "床位编号")
    @Excel(name = "床位编号")
    private String bedNumber;

    /** 床位id */
    @Schema(title = "床位id")
    @Excel(name = "床位id")
    private Long bedId;

    /** 民族 */
    @Schema(title = "民族")
    @Excel(name = "民族")
    private String nation;

    /** 文化程度 */
    @Schema(title = "文化程度")
    @Excel(name = "文化程度")
    private String educationLevel;

    /** 社保卡号 */
    @Schema(title = "社保卡号")
    @Excel(name = "社保卡号")
    private String socialSecurityCard;

    /** 居住情况 */
    @Schema(title = "居住情况")
    @Excel(name = "居住情况")
    private String livingSituation;

    /** 宗教信仰 */
    @Schema(title = "宗教信仰")
    @Excel(name = "宗教信仰")
    private String religiousBelief;

    /** 经济来源 */
    @Schema(title = "经济来源")
    @Excel(name = "经济来源")
    private String economicSource;

    /** 婚姻状况 */
    @Schema(title = "婚姻状况")
    @Excel(name = "婚姻状况")
    private String maritalStatus;

    /** 医疗费用支付方式 */
    @Schema(title = "医疗费用支付方式")
    @Excel(name = "医疗费用支付方式")
    private String medicalPaymentMethod;

    /** 核心建议 */
    @Schema(title = "核心建议")
    @Excel(name = "核心建议")
    private String coreSuggestion;


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getIdCardNo() { return idCardNo; }
    public void setIdCardNo(String idCardNo) { this.idCardNo = idCardNo; }
    public Integer getSex() { return sex; }
    public void setSex(Integer sex) { this.sex = sex; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getIdCardNationalEmblemImg() { return idCardNationalEmblemImg; }
    public void setIdCardNationalEmblemImg(String idCardNationalEmblemImg) { this.idCardNationalEmblemImg = idCardNationalEmblemImg; }
    public String getIdCardPortraitImg() { return idCardPortraitImg; }
    public void setIdCardPortraitImg(String idCardPortraitImg) { this.idCardPortraitImg = idCardPortraitImg; }
    public String getBedNumber() { return bedNumber; }
    public void setBedNumber(String bedNumber) { this.bedNumber = bedNumber; }
    public Long getBedId() { return bedId; }
    public void setBedId(Long bedId) { this.bedId = bedId; }
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
