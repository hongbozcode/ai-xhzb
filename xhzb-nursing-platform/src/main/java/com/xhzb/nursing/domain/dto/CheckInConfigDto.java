package com.xhzb.nursing.domain.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

/** 入住及费用配置参数。 */
@Data
public class CheckInConfigDto {
    /** 床位ID */
    @NotNull
    @Positive
    private Long bedId;
    /** 房间ID */
    @NotNull
    @Positive
    private Long roomId;
    /** 楼层ID */
    @NotNull
    @Positive
    private Long floorId;
    /** 护理等级ID */
    @NotNull
    @Positive
    private Long nursingLevelId;
    /** 房间编号 */
    @Size(max = 50)
    private String code;
    /** 楼层名称 */
    @Size(max = 50)
    private String floorName;
    /** 护理等级名称 */
    @Size(max = 50)
    private String nursingLevelName;
    /** 入住开始日期 */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDate;
    /** 入住结束日期 */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;
    /** 计费开始日期 */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime feeStartDate;
    /** 计费结束日期 */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime feeEndDate;
    /** 押金 */
    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal deposit;
    /** 床位费 */
    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal bedFee;
    /** 护理费 */
    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal nursingFee;
    /** 医保支付 */
    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal insurancePayment;
    /** 政府补贴 */
    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal governmentSubsidy;
    /** 其他费用 */
    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal otherFees;
}
