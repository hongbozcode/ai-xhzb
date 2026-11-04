package com.xhzb.nursing.domain.vo;

import lombok.Data;
import java.util.List;
import com.xhzb.nursing.domain.*;

/** 入住完整详情。 */
@Data
public class CheckInDetailVo {
    /** 老人信息 */
    private CheckInElderVo checkInElderVo;
    /** 家属信息 */
    private List<com.xhzb.nursing.domain.dto.ElderFamilyDto> elderFamilyVoList;
    /** 入住配置 */
    private CheckInConfigVo checkInConfigVo;
    /** 本次入住合同 */
    private Contract contract;
}
