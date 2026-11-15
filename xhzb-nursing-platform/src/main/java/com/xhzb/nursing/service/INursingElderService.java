package com.xhzb.nursing.service;

import java.util.List;
import com.xhzb.nursing.domain.NursingElder;
import com.xhzb.nursing.domain.dto.NursingElderAssignmentDto;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 护理员老人关联Service接口
 * 
 * @author hongbozcode
 * @date 2026-11-09
 */
public interface INursingElderService extends IService<NursingElder>
{
    /** 用提交的护理员列表更新每位指定老人的关联。 */
    void setNursing(List<NursingElderAssignmentDto> assignments);

    /**
     * 查询护理员老人关联
     * 
     * @param id 护理员老人关联主键
     * @return 护理员老人关联
     */
    public NursingElder selectNursingElderById(Long id);

    /**
     * 查询护理员老人关联列表
     * 
     * @param nursingElder 护理员老人关联
     * @return 护理员老人关联集合
     */
    public List<NursingElder> selectNursingElderList(NursingElder nursingElder);

    /**
     * 新增护理员老人关联
     * 
     * @param nursingElder 护理员老人关联
     * @return 结果
     */
    public int insertNursingElder(NursingElder nursingElder);

    /**
     * 修改护理员老人关联
     * 
     * @param nursingElder 护理员老人关联
     * @return 结果
     */
    public int updateNursingElder(NursingElder nursingElder);

    /**
     * 批量删除护理员老人关联
     * 
     * @param ids 需要删除的护理员老人关联主键集合
     * @return 结果
     */
    public int deleteNursingElderByIds(Long[] ids);

    /**
     * 删除护理员老人关联信息
     * 
     * @param id 护理员老人关联主键
     * @return 结果
     */
    public int deleteNursingElderById(Long id);
}
