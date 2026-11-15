package com.xhzb.nursing.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xhzb.common.annotation.Excel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.xhzb.common.core.domain.BaseEntity;

/**
 * 护理任务对象 nursing_task
 * 
 * @author hongbozcode
 * @date 2026-11-09
 */
@Data
@Schema(description = "护理任务对象")
public class NursingTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** id */
    @Schema(title = "id")
    private Long id;

    /** 护理员id */
    @Schema(title = "护理员id")
    @Excel(name = "护理员id")
    private String nursingId;

    /** 项目id */
    @Schema(title = "项目id")
    @Excel(name = "项目id")
    private Integer projectId;

    /** 护理项目名称 */
    @Schema(title = "护理项目名称")
    @Excel(name = "护理项目名称")
    private String projectName;

    /** 老人id */
    @Schema(title = "老人id")
    @Excel(name = "老人id")
    private Long elderId;

    /** 老人姓名 */
    @Schema(title = "老人姓名")
    @Excel(name = "老人姓名")
    private String elderName;

    /** 床位编号 */
    @Schema(title = "床位编号")
    @Excel(name = "床位编号")
    private String bedNumber;

    /** 预计服务时间 */
    @Schema(title = "预计服务时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "预计服务时间", width = 30, dateFormat = "yyyy-MM-dd")
    private LocalDateTime estimatedServerTime;

    /** 实际服务时间 */
    @Schema(title = "实际服务时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "实际服务时间", width = 30, dateFormat = "yyyy-MM-dd")
    private LocalDateTime realServerTime;

    /** 执行记录 */
    @Schema(title = "执行记录")
    @Excel(name = "执行记录")
    private String mark;

    /** 取消原因 */
    @Schema(title = "取消原因")
    @Excel(name = "取消原因")
    private String cancelReason;

    /** 状态  1待执行 2已执行 3已关闭  */
    @Schema(title = "状态  1待执行 2已执行 3已关闭 ")
    @Excel(name = "状态  1待执行 2已执行 3已关闭 ")
    private Integer status;

    /** 执行图片 */
    @Schema(title = "执行图片")
    @Excel(name = "执行图片")
    private String taskImage;

}
