package com.xhzb.nursing.domain.vo;

import java.util.List;
import com.xhzb.nursing.domain.*;

/** 入住完整详情。 */
public class CheckInDetailVo {
    /** 老人信息 */
    private CheckInElderVo checkInElderVo;
    /** 家属信息 */
    private List<com.xhzb.nursing.domain.dto.ElderFamilyDto> elderFamilyVoList;
    /** 入住配置 */
    private CheckInConfigVo checkInConfigVo;
    /** 本次入住合同 */
    private Contract contract;
    public CheckInElderVo getCheckInElderVo() { return checkInElderVo; }
    public void setCheckInElderVo(CheckInElderVo checkInElderVo) { this.checkInElderVo = checkInElderVo; }
    public List<com.xhzb.nursing.domain.dto.ElderFamilyDto> getElderFamilyVoList() { return elderFamilyVoList; }
    public void setElderFamilyVoList(List<com.xhzb.nursing.domain.dto.ElderFamilyDto> elderFamilyVoList) { this.elderFamilyVoList = elderFamilyVoList; }
    public CheckInConfigVo getCheckInConfigVo() { return checkInConfigVo; }
    public void setCheckInConfigVo(CheckInConfigVo checkInConfigVo) { this.checkInConfigVo = checkInConfigVo; }
    public Contract getContract() { return contract; }
    public void setContract(Contract contract) { this.contract = contract; }
}
