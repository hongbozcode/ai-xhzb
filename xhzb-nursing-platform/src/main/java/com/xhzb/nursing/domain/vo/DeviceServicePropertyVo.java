package com.xhzb.nursing.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 设备最近一次上报的属性数据。
 */
@Data
@Schema(description = "设备上报属性")
public class DeviceServicePropertyVo
{
    /** 产品模型中的属性标识符 */
    private String functionId;

    /** 属性上报时间，已转换为北京时间 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime eventTime;

    /** 上报属性值，保留云端返回的数值、布尔或字符串等类型 */
    private Object value;
}
