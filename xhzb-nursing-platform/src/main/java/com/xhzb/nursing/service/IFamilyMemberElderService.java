package com.xhzb.nursing.service;

import com.xhzb.nursing.domain.dto.FamilyMemberElderBindDto;
import com.xhzb.nursing.domain.vo.FamilyMemberElderDetailVo;
import com.xhzb.nursing.domain.vo.FamilyMemberElderVo;
import java.util.List;

/**
 * 小程序家人绑定服务。
 */
public interface IFamilyMemberElderService
{
    /** 根据姓名与身份证号为当前家属绑定老人。 */
    void bindFamily(FamilyMemberElderBindDto bindDto);

    /** 查询当前家属绑定的所有老人。 */
    List<FamilyMemberElderVo> listMyFamily();

    /** 按页查询当前家属绑定的老人详情。 */
    List<FamilyMemberElderDetailVo> listMyFamilyByPage(int pageNum, int pageSize);
}
