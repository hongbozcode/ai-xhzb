package com.xhzb.nursing.domain.dto;

import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

/** 入住签约参数。 */
@Data
public class CheckInContractDto {
    /** 合同名称 */
    @NotBlank
    @Size(max = 100)
    private String contractName;
    /** 协议地址 */
    @NotBlank
    @Size(max = 255)
    private String agreementPath;
    /** 丙方姓名 */
    @NotBlank
    @Size(max = 50)
    private String thirdPartyName;
    /** 丙方电话 */
    @NotBlank
    @Size(max = 20)
    private String thirdPartyPhone;
    /** 签约日期 */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime signDate;
}
