package com.xhzb.nursing.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.common.utils.SecurityUtils;
import com.xhzb.nursing.domain.AlertData;
import com.xhzb.nursing.domain.AlertRule;
import com.xhzb.nursing.domain.DeviceData;
import com.xhzb.nursing.domain.dto.HandleAlertDataDto;
import com.xhzb.nursing.mapper.AlertDataMapper;
import com.xhzb.nursing.service.IAlertDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.time.ZoneId;

/**
 * 报警数据Service业务层处理
 * 
 * @author hongbozcode
 * @date 2026-11-09
 */
@Service
@Slf4j
public class AlertDataServiceImpl extends ServiceImpl<AlertDataMapper, AlertData> implements IAlertDataService
{
    private static final int ELDER_ALERT = 0;
    private static final int DEVICE_ALERT = 1;
    private static final int WEARABLE_DEVICE = 0;
    private static final int FIXED_DEVICE = 1;
    private static final String ADMINISTRATION_ROLE = "行政";
    private static final String SUPER_ADMIN_ROLE = "超级管理员";
    private static final ZoneId ALERT_ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private AlertDataMapper alertDataMapper;

    /** 只更新待处理记录，避免重复提交覆盖已登记的结果。 */
    @Override
    public void handleAlertData(HandleAlertDataDto request)
    {
        var operator = SecurityUtils.getLoginUser();
        String processorName = operator.getUser().getNickName();
        boolean updated = lambdaUpdate()
                .eq(AlertData::getId, request.getId())
                .eq(AlertData::getUserId, operator.getUserId())
                .eq(AlertData::getStatus, 0)
                .set(AlertData::getProcessingResult, request.getProcessingResult())
                .set(AlertData::getProcessingTime, request.getProcessingTime().atZoneSameInstant(ALERT_ZONE).toLocalDateTime())
                .set(AlertData::getProcessorId, operator.getUserId())
                .set(AlertData::getProcessorName, StringUtils.hasText(processorName) ? processorName : operator.getUsername())
                .set(AlertData::getUpdateBy, operator.getUserId().toString())
                .set(AlertData::getStatus, 1)
                .update();
        if (!updated)
        {
            throw new ServiceException("报警数据不存在或已处理");
        }
    }

    /** 在一次独立事务中查询接收人并批量生成待处理的报警记录。 */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public List<Long> saveTriggeredAlerts(AlertRule rule, DeviceData report)
    {
        Integer type = rule.getAlertDataType();
        Set<Long> userIds = new LinkedHashSet<>();
        if (Integer.valueOf(DEVICE_ALERT).equals(type))
        {
            userIds.addAll(alertDataMapper.selectUserIdsByRoleName(ADMINISTRATION_ROLE));
        }
        else if (Integer.valueOf(ELDER_ALERT).equals(type))
        {
            if (Integer.valueOf(WEARABLE_DEVICE).equals(report.getLocationType()))
            {
                userIds.addAll(alertDataMapper.selectWearableNursingUserIds(report.getIotId()));
            }
            else if (Integer.valueOf(FIXED_DEVICE).equals(report.getLocationType()))
            {
                userIds.addAll(alertDataMapper.selectBedNursingUserIds(report.getIotId()));
            }
        }
        else
        {
            log.error("报警规则类型无效，跳过报警保存，ruleId={}, type={}", rule.getId(), type);
            return List.of();
        }
        userIds.addAll(alertDataMapper.selectUserIdsByRoleName(SUPER_ADMIN_ROLE));
        if (userIds.isEmpty())
        {
            log.error("未找到报警接收人，跳过报警保存，ruleId={}, iotId={}", rule.getId(), report.getIotId());
            return List.of();
        }

        String functionName = StringUtils.hasText(rule.getFunctionName()) ? rule.getFunctionName() : report.getFunctionId();
        String reason = "%s %s %s，持续 %d 个周期（每周期 1 分钟）"
                .formatted(functionName, rule.getOperator(), rule.getValue(), rule.getDuration());
        Date createdAt = DateUtils.getNowDate();
        List<AlertData> alerts = userIds.stream()
                .map(userId -> buildAlert(rule, report, userId, reason, createdAt)).toList();
        if (!saveBatch(alerts) || alerts.stream().anyMatch(alert -> alert.getId() == null))
        {
            throw new ServiceException("批量保存报警数据失败或未返回完整主键");
        }
        return alerts.stream().map(AlertData::getId).toList();
    }

    /** 为一个接收人生成待处理记录，并保留触发报警时的设备信息。 */
    private AlertData buildAlert(AlertRule rule, DeviceData report, Long userId, String reason, Date createdAt)
    {
        AlertData alert = new AlertData();
        alert.setIotId(report.getIotId());
        alert.setDeviceName(report.getDeviceName());
        alert.setProductKey(report.getProductKey());
        alert.setProductName(report.getProductName());
        alert.setFunctionId(report.getFunctionId());
        alert.setAccessLocation(report.getAccessLocation());
        alert.setLocationType(report.getLocationType());
        alert.setPhysicalLocationType(report.getPhysicalLocationType());
        alert.setDeviceDescription(report.getDeviceDescription());
        alert.setDataValue(report.getDataValue());
        alert.setAlertRuleId(rule.getId());
        alert.setAlertReason(reason);
        alert.setType(rule.getAlertDataType());
        alert.setStatus(0);
        alert.setUserId(userId);
        alert.setCreateTime(createdAt);
        return alert;
    }

    /**
     * 查询报警数据
     * 
     * @param id 报警数据主键
     * @return 报警数据
     */
    @Override
    public AlertData selectAlertDataById(Long id)
    {
        AlertData alert = lambdaQuery().eq(AlertData::getId, id)
                .eq(AlertData::getUserId, SecurityUtils.getUserId()).one();
        if (alert == null)
        {
            throw new ServiceException("报警数据不存在或无权访问");
        }
        return alert;
    }

    /**
     * 查询报警数据列表
     * 
     * @param alertData 报警数据
     * @return 报警数据
     */
    @Override
    public List<AlertData> selectAlertDataList(AlertData alertData)
    {
        Long userId = SecurityUtils.getUserId();
        alertData.setUserId(userId);
        List<AlertData> alerts = alertDataMapper.selectAlertDataList(alertData);
        if (!alerts.isEmpty())
        {
            Map<Long, String> namesById = new HashMap<>();
            // 保留分页查询返回的原始列表及其总数，只填充当前页的显示名称。
            List<Long> ids = alerts.stream().map(AlertData::getId).toList();
            for (AlertData location : alertDataMapper.selectAccessLocationNames(ids, userId))
            {
                namesById.put(location.getId(), location.getAccessLocationName());
            }
            alerts.forEach(alert -> alert.setAccessLocationName(namesById.get(alert.getId())));
        }
        return alerts;
    }

    /**
     * 新增报警数据
     * 
     * @param alertData 报警数据
     * @return 结果
     */
    @Override
    public int insertAlertData(AlertData alertData)
    {
        return save(alertData)? 1 : 0;
    }

    /**
     * 修改报警数据
     * 
     * @param alertData 报警数据
     * @return 结果
     */
    @Override
    public int updateAlertData(AlertData alertData)
    {
        return updateById(alertData)? 1 : 0;
    }

    /**
     * 批量删除报警数据
     * 
     * @param ids 需要删除的报警数据主键
     * @return 结果
     */
    @Override
    public int deleteAlertDataByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids))? 1 : 0;
    }

    /**
     * 删除报警数据信息
     * 
     * @param id 报警数据主键
     * @return 结果
     */
    @Override
    public int deleteAlertDataById(Long id)
    {
        return removeById(id)? 1 : 0;
    }
}
