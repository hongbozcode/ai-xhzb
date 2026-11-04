package com.xhzb.nursing.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xhzb.nursing.domain.*;

/** 入住配置详情。 */
@Data
public class CheckInConfigVo extends CheckInConfig {
    /** 入住开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDate;
    /** 入住结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;
    /** 入住床位编号 */
    private String bedNumber;
}
