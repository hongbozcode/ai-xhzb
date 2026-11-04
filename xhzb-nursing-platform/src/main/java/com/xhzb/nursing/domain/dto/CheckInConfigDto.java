package com.xhzb.nursing.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

/** 入住及费用配置参数。 */
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
    public Long getBedId() { return bedId; }
    public void setBedId(Long bedId) { this.bedId = bedId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public Long getFloorId() { return floorId; }
    public void setFloorId(Long floorId) { this.floorId = floorId; }
    public Long getNursingLevelId() { return nursingLevelId; }
    public void setNursingLevelId(Long nursingLevelId) { this.nursingLevelId = nursingLevelId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getFloorName() { return floorName; }
    public void setFloorName(String floorName) { this.floorName = floorName; }
    public String getNursingLevelName() { return nursingLevelName; }
    public void setNursingLevelName(String nursingLevelName) { this.nursingLevelName = nursingLevelName; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public LocalDateTime getFeeStartDate() { return feeStartDate; }
    public void setFeeStartDate(LocalDateTime feeStartDate) { this.feeStartDate = feeStartDate; }
    public LocalDateTime getFeeEndDate() { return feeEndDate; }
    public void setFeeEndDate(LocalDateTime feeEndDate) { this.feeEndDate = feeEndDate; }
    public BigDecimal getDeposit() { return deposit; }
    public void setDeposit(BigDecimal deposit) { this.deposit = deposit; }
    public BigDecimal getBedFee() { return bedFee; }
    public void setBedFee(BigDecimal bedFee) { this.bedFee = bedFee; }
    public BigDecimal getNursingFee() { return nursingFee; }
    public void setNursingFee(BigDecimal nursingFee) { this.nursingFee = nursingFee; }
    public BigDecimal getInsurancePayment() { return insurancePayment; }
    public void setInsurancePayment(BigDecimal insurancePayment) { this.insurancePayment = insurancePayment; }
    public BigDecimal getGovernmentSubsidy() { return governmentSubsidy; }
    public void setGovernmentSubsidy(BigDecimal governmentSubsidy) { this.governmentSubsidy = governmentSubsidy; }
    public BigDecimal getOtherFees() { return otherFees; }
    public void setOtherFees(BigDecimal otherFees) { this.otherFees = otherFees; }
}
