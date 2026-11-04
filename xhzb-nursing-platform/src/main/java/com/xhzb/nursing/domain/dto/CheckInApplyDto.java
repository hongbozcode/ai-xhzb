package com.xhzb.nursing.domain.dto;

import lombok.Data;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/** 入住申请参数。 */
@Data
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
}
