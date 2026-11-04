package com.xhzb.nursing.domain.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

/** 入住签约参数。 */
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
    public String getContractName() { return contractName; }
    public void setContractName(String contractName) { this.contractName = contractName; }
    public String getAgreementPath() { return agreementPath; }
    public void setAgreementPath(String agreementPath) { this.agreementPath = agreementPath; }
    public String getThirdPartyName() { return thirdPartyName; }
    public void setThirdPartyName(String thirdPartyName) { this.thirdPartyName = thirdPartyName; }
    public String getThirdPartyPhone() { return thirdPartyPhone; }
    public void setThirdPartyPhone(String thirdPartyPhone) { this.thirdPartyPhone = thirdPartyPhone; }
    public LocalDateTime getSignDate() { return signDate; }
    public void setSignDate(LocalDateTime signDate) { this.signDate = signDate; }
}
