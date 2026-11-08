package com.xhzb.nursing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhzb.nursing.domain.FamilyMemberElder;
import com.xhzb.nursing.domain.vo.FamilyMemberElderDetailVo;
import com.xhzb.nursing.domain.vo.FamilyMemberElderVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 家属与老人的绑定关系 Mapper。
 */
@Mapper
public interface FamilyMemberElderMapper extends BaseMapper<FamilyMemberElder>
{
    /** 查询指定家属的绑定列表。 */
    List<FamilyMemberElderVo> selectMyFamily(@Param("familyMemberId") Long familyMemberId);

    /** 查询指定家属绑定老人的床位、房型和随身手表。 */
    List<FamilyMemberElderDetailVo> selectMyFamilyDetails(@Param("familyMemberId") Long familyMemberId);
}
