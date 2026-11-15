package com.xhzb.nursing.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhzb.nursing.domain.AlertData;

/**
 * 报警数据Mapper接口
 * 
 * @author hongbozcode
 * @date 2026-11-09
 */
@Mapper
public interface AlertDataMapper extends BaseMapper<AlertData>
{
    /** 查询指定角色的用户 ID。 */
    List<Long> selectUserIdsByRoleName(@Param("roleName") String roleName);

    /** 通过随身设备绑定的老人查询护理员用户 ID。 */
    List<Long> selectWearableNursingUserIds(@Param("iotId") String iotId);

    /** 通过固定设备绑定的床位查询护理员用户 ID。 */
    List<Long> selectBedNursingUserIds(@Param("iotId") String iotId);

    /**
     * 查询报警数据
     * 
     * @param id 报警数据主键
     * @return 报警数据
     */
    public AlertData selectAlertDataById(Long id);

    /**
     * 查询报警数据列表
     * 
     * @param alertData 报警数据
     * @return 报警数据集合
     */
    public List<AlertData> selectAlertDataList(AlertData alertData);

    /** 批量解析当前页报警的接入位置名称。 */
    List<AlertData> selectAccessLocationNames(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    /**
     * 新增报警数据
     * 
     * @param alertData 报警数据
     * @return 结果
     */
    public int insertAlertData(AlertData alertData);

    /**
     * 修改报警数据
     * 
     * @param alertData 报警数据
     * @return 结果
     */
    public int updateAlertData(AlertData alertData);

    /**
     * 删除报警数据
     * 
     * @param id 报警数据主键
     * @return 结果
     */
    public int deleteAlertDataById(Long id);

    /**
     * 批量删除报警数据
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAlertDataByIds(Long[] ids);
}
