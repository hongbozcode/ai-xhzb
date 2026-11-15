package com.xhzb.nursing.service;

import java.util.List;
import com.xhzb.nursing.domain.AlertData;
import com.xhzb.nursing.domain.dto.HandleAlertDataDto;
import com.xhzb.nursing.domain.AlertRule;
import com.xhzb.nursing.domain.DeviceData;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 报警数据Service接口
 * 
 * @author hongbozcode
 * @date 2026-11-09
 */
public interface IAlertDataService extends IService<AlertData>
{
    /** 将待处理的报警标记为已处理。 */
    void handleAlertData(HandleAlertDataDto request);

    /** 按接收人批量保存同一次报警，返回已提交的报警记录 ID。 */
    List<Long> saveTriggeredAlerts(AlertRule rule, DeviceData report);

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
     * 批量删除报警数据
     * 
     * @param ids 需要删除的报警数据主键集合
     * @return 结果
     */
    public int deleteAlertDataByIds(Long[] ids);

    /**
     * 删除报警数据信息
     * 
     * @param id 报警数据主键
     * @return 结果
     */
    public int deleteAlertDataById(Long id);
}
