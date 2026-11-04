package com.xhzb.nursing.domain.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/** 入住申请参数。 */
public class CheckInApplyDto {
    /** 评估ID */
    @NotNull
    @Positive
    private Long healthAssessmentId;
    /** 老人信息 */
    @NotNull
    @Valid
    private CheckInElderDto checkInElderDto;
    /** 家属信息 */
    @NotNull
    private List<@NotNull @Valid ElderFamilyDto> elderFamilyDtoList;
    /** 入住配置 */
    @NotNull
    @Valid
    private CheckInConfigDto checkInConfigDto;
    /** 合同信息 */
    @NotNull
    @Valid
    private CheckInContractDto checkInContractDto;
    public Long getHealthAssessmentId() { return healthAssessmentId; }
    public void setHealthAssessmentId(Long healthAssessmentId) { this.healthAssessmentId = healthAssessmentId; }
    public CheckInElderDto getCheckInElderDto() { return checkInElderDto; }
    public void setCheckInElderDto(CheckInElderDto checkInElderDto) { this.checkInElderDto = checkInElderDto; }
    public List<@NotNull @Valid ElderFamilyDto> getElderFamilyDtoList() { return elderFamilyDtoList; }
    public void setElderFamilyDtoList(List<@NotNull @Valid ElderFamilyDto> elderFamilyDtoList) { this.elderFamilyDtoList = elderFamilyDtoList; }
    public CheckInConfigDto getCheckInConfigDto() { return checkInConfigDto; }
    public void setCheckInConfigDto(CheckInConfigDto checkInConfigDto) { this.checkInConfigDto = checkInConfigDto; }
    public CheckInContractDto getCheckInContractDto() { return checkInContractDto; }
    public void setCheckInContractDto(CheckInContractDto checkInContractDto) { this.checkInContractDto = checkInContractDto; }
}
