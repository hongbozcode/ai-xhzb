package com.xhzb.nursing.service;

import com.xhzb.nursing.domain.dto.CheckInApplyDto;
import com.xhzb.nursing.domain.vo.CheckInDetailVo;

import java.util.List;
import com.xhzb.nursing.domain.CheckIn;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 入住信息Service接口
 * 
 * @author hongbozcode
 * @date 2026-11-03
 */
public interface ICheckInService extends IService<CheckIn>
{
    /**
     * 查询入住信息
     * 
     * @param id 入住信息主键
     * @return 入住信息
     */
    public CheckIn selectCheckInById(Long id);

    /**
     * 查询入住信息列表
     * 
     * @param checkIn 入住信息
     * @return 入住信息集合
     */
    public List<CheckIn> selectCheckInList(CheckIn checkIn);

    /**
     * 新增入住信息
     * 
     * @param checkIn 入住信息
     * @return 结果
     */
    public int insertCheckIn(CheckIn checkIn);

    /**
     * 修改入住信息
     * 
     * @param checkIn 入住信息
     * @return 结果
     */
    public int updateCheckIn(CheckIn checkIn);

    /**
     * 批量删除入住信息
     * 
     * @param ids 需要删除的入住信息主键集合
     * @return 结果
     */
    public int deleteCheckInByIds(Long[] ids);

    /**
     * 删除入住信息信息
     * 
     * @param id 入住信息主键
     * @return 结果
     */
    public int deleteCheckInById(Long id);
    /** 申请入住，原子完成床位、老人、合同及评估更新。 */
    void apply(CheckInApplyDto dto);
    /** 根据入住ID查询完整详情。 */
    CheckInDetailVo detail(Long id);

}
