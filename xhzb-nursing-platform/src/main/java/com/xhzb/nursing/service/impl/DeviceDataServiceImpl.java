package com.xhzb.nursing.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.nursing.domain.Device;
import com.xhzb.nursing.domain.DeviceData;
import com.xhzb.nursing.domain.vo.IotDaDeviceReportVo;
import com.xhzb.nursing.mapper.DeviceDataMapper;
import com.xhzb.nursing.mapper.DeviceMapper;
import com.xhzb.nursing.service.IDeviceDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 设备数据Service业务层处理
 * 
 * @author hongbozcode
 * @date 2026-11-06
 */
@Service
@RequiredArgsConstructor
public class DeviceDataServiceImpl extends ServiceImpl<DeviceDataMapper, DeviceData> implements IDeviceDataService
{
    private static final DateTimeFormatter IOTDA_EVENT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX");
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

    private final DeviceDataMapper deviceDataMapper;
    private final DeviceMapper deviceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDeviceReport(IotDaDeviceReportVo report)
    {
        IotDaDeviceReportVo.NotifyData notifyData = report.getNotifyData();
        IotDaDeviceReportVo.Header header = notifyData == null ? null : notifyData.getHeader();
        String iotId = header == null ? null : header.getDeviceId();
        if (!StringUtils.hasText(iotId))
        {
            iotId = report.getDeviceId();
        }
        if (!StringUtils.hasText(iotId))
        {
            return;
        }

        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getIotId, iotId));
        if (device == null)
        {
            return;
        }

        Date createTime = DateUtils.getNowDate();
        List<DeviceData> records = new ArrayList<>();
        IotDaDeviceReportVo.Body body = notifyData == null ? null : notifyData.getBody();
        if (body != null && body.getServices() != null)
        {
            for (IotDaDeviceReportVo.Service service : body.getServices())
            {
                if (service != null)
                {
                    String eventTime = StringUtils.hasText(service.getEventTime())
                            ? service.getEventTime() : report.getEventTime();
                    addProperties(records, device, service.getProperties(), eventTime, createTime);
                }
            }
        }
        if (!records.isEmpty() && !saveBatch(records))
        {
            throw new ServiceException("批量保存设备上报数据失败");
        }
    }

    /** 将上报属性拆成独立记录，并保留入库时的设备和位置字段。 */
    private void addProperties(List<DeviceData> records, Device device, Map<String, Object> properties,
                               String eventTime, Date createTime)
    {
        if (properties == null || properties.isEmpty())
        {
            return;
        }

        LocalDateTime alarmTime = StringUtils.hasText(eventTime)
                ? OffsetDateTime.parse(eventTime, IOTDA_EVENT_TIME_FORMAT)
                        .atZoneSameInstant(BEIJING_ZONE).toLocalDateTime()
                : null;
        properties.forEach((functionId, value) -> {
            DeviceData record = new DeviceData();
            record.setIotId(device.getIotId());
            record.setDeviceName(device.getDeviceName());
            record.setProductKey(device.getProductKey());
            record.setProductName(device.getProductName());
            record.setAccessLocation(device.getBindingLocation());
            record.setLocationType(device.getLocationType());
            record.setPhysicalLocationType(device.getPhysicalLocationType());
            record.setDeviceDescription(device.getDeviceDescription());
            record.setFunctionId(functionId);
            record.setDataValue(value == null || value instanceof String ? (String) value : JSON.toJSONString(value));
            record.setAlarmTime(alarmTime);
            record.setCreateTime(createTime);
            records.add(record);
        });
    }

    /**
     * 查询设备数据
     * 
     * @param id 设备数据主键
     * @return 设备数据
     */
    @Override
    public DeviceData selectDeviceDataById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询设备数据列表
     * 
     * @param deviceData 设备数据
     * @return 设备数据
     */
    @Override
    public List<DeviceData> selectDeviceDataList(DeviceData deviceData)
    {
        return deviceDataMapper.selectDeviceDataList(deviceData);
    }

    /**
     * 新增设备数据
     * 
     * @param deviceData 设备数据
     * @return 结果
     */
    @Override
    public int insertDeviceData(DeviceData deviceData)
    {
        return save(deviceData)? 1 : 0;
    }

    /**
     * 修改设备数据
     * 
     * @param deviceData 设备数据
     * @return 结果
     */
    @Override
    public int updateDeviceData(DeviceData deviceData)
    {
        return updateById(deviceData)? 1 : 0;
    }

    /**
     * 批量删除设备数据
     * 
     * @param ids 需要删除的设备数据主键
     * @return 结果
     */
    @Override
    public int deleteDeviceDataByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids))? 1 : 0;
    }

    /**
     * 删除设备数据信息
     * 
     * @param id 设备数据主键
     * @return 结果
     */
    @Override
    public int deleteDeviceDataById(Long id)
    {
        return removeById(id)? 1 : 0;
    }
}
