package com.xhzb.nursing.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.Data;

/** 提交设备报警处理结果。 */
@Data
public class HandleAlertDataDto
{
    /** 报警数据编号。 */
    @NotNull(message = "报警数据 ID 不能为空")
    @Positive(message = "报警数据 ID 必须大于 0")
    private Long id;

    /** 处理结果，允许空字符串。 */
    @NotNull(message = "请提供处理结果")
    @Size(max = 500, message = "处理结果不能超过 500 个字符")
    private String processingResult;

    /** 含时区偏移的处理时间。 */
    @NotNull(message = "处理时间不能为空")
    private OffsetDateTime processingTime;
}
