package com.xhzb.nursing.domain;

import com.xhzb.common.annotation.Excel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.xhzb.common.core.domain.BaseEntity;

/**
 * 护理员老人关联对象 nursing_elder
 * 
 * @author hongbozcode
 * @date 2026-11-09
 */
@Data
@Schema(description = "护理员老人关联对象")
public class NursingElder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** id */
    @Schema(title = "id")
    private Long id;

    /** 护理员id */
    @Schema(title = "护理员id")
    @Excel(name = "护理员id")
    private Long nursingId;

    /** 老人id */
    @Schema(title = "老人id")
    @Excel(name = "老人id")
    private Long elderId;

}
