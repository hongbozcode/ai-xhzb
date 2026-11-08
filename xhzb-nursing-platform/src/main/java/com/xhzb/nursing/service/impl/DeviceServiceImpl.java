package com.xhzb.nursing.service.impl;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.huaweicloud.sdk.iotda.v5.model.AddDevice;
import com.huaweicloud.sdk.iotda.v5.model.AddDeviceRequest;
import com.huaweicloud.sdk.iotda.v5.model.AddDeviceResponse;
import com.huaweicloud.sdk.iotda.v5.model.AuthInfo;
import com.huaweicloud.sdk.iotda.v5.model.DeleteDeviceRequest;
import com.huaweicloud.sdk.iotda.v5.model.DeleteDeviceResponse;
import com.huaweicloud.sdk.core.exception.SdkException;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.huaweicloud.sdk.iotda.v5.model.ShowDeviceRequest;
import com.huaweicloud.sdk.iotda.v5.model.ShowDeviceResponse;
import com.huaweicloud.sdk.iotda.v5.model.ShowDeviceShadowRequest;
import com.huaweicloud.sdk.iotda.v5.model.ShowDeviceShadowResponse;
import com.huaweicloud.sdk.iotda.v5.model.UpdateDevice;
import com.huaweicloud.sdk.iotda.v5.model.UpdateDeviceRequest;
import com.huaweicloud.sdk.iotda.v5.model.UpdateDeviceResponse;
import com.huaweicloud.sdk.iotda.v5.model.DeviceShadowData;
import com.huaweicloud.sdk.iotda.v5.model.DeviceShadowProperties;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.nursing.domain.dto.DeviceRegisterDto;
import com.xhzb.nursing.domain.dto.DeviceUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import cn.hutool.json.JSONUtil;
import com.huaweicloud.sdk.iotda.v5.IoTDAClient;
import com.huaweicloud.sdk.iotda.v5.model.ListProductsRequest;
import com.huaweicloud.sdk.iotda.v5.model.ListProductsResponse;
import com.huaweicloud.sdk.iotda.v5.model.ProductSummary;
import com.xhzb.common.constant.CacheConstants;
import com.xhzb.common.exception.base.BaseException;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.nursing.domain.vo.ProductVo;
import com.xhzb.nursing.domain.vo.DeviceDetailVo;
import com.xhzb.nursing.domain.vo.DeviceServicePropertyVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xhzb.nursing.mapper.DeviceMapper;
import com.xhzb.nursing.domain.Device;
import com.xhzb.nursing.service.IDeviceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.Arrays;

/**
 * 设备Service业务层处理
 * 
 * @author hongbozcode
 * @date 2026-11-06
 */
@Service
@Slf4j
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements IDeviceService
{
    @Autowired
    private DeviceMapper deviceMapper;
    @Autowired
    private IoTDAClient client;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    /**
     * 删除本地设备后通知华为云，云端失败时回滚本地删除。
     *
     * @param iotId 华为云设备 ID
     */
    @Override
    @Transactional
    public void deleteDeviceByIotId(String iotId)
    {
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getIotId, iotId).last("FOR UPDATE"));
        if (device == null)
        {
            throw new ServiceException("设备不存在");
        }
        if (deviceMapper.deleteById(device.getId()) != 1)
        {
            throw new ServiceException("本地设备删除失败");
        }

        DeleteDeviceResponse response;
        try
        {
            response = client.deleteDevice(new DeleteDeviceRequest().withDeviceId(device.getIotId()));
        }
        catch (ServiceResponseException ex)
        {
            // 云端明确确认设备已不存在时，可以完成本地清理。
            if (ex.getHttpStatusCode() == 404 && "IOTDA.014000".equals(ex.getErrorCode()))
            {
                return;
            }
            throw new ServiceException("华为云设备删除失败，请稍后重试");
        }
        catch (SdkException ex)
        {
            throw new ServiceException("华为云设备删除失败，请稍后重试");
        }
        if (response == null || response.getHttpStatusCode() != 204)
        {
            throw new ServiceException("华为云设备删除失败");
        }
    }

    /**
     * 校验修改后的设备名称与绑定位置，先更新华为云，再保存本地资料。
     *
     * @param dto 设备修改参数
     */
    @Override
    @Transactional
    public void modifyDevice(DeviceUpdateDto dto)
    {
        // 同一设备的修改串行执行，避免云端更新及失败补偿互相覆盖。
        Device current = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getId, dto.getId()).last("FOR UPDATE"));
        if (current == null)
        {
            throw new ServiceException("设备不存在");
        }
        if (!StringUtils.hasText(current.getIotId()))
        {
            throw new ServiceException("设备缺少物联网设备ID");
        }
        if (deviceMapper.selectCount(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceName, dto.getDeviceName()).ne(Device::getId, current.getId())) > 0)
        {
            throw new ServiceException("设备名称已存在");
        }

        boolean wearable = Integer.valueOf(0).equals(dto.getLocationType());
        Integer physicalLocationType = wearable ? null : dto.getPhysicalLocationType();
        if (StringUtils.hasText(current.getProductKey()))
        {
            LambdaQueryWrapper<Device> binding = new LambdaQueryWrapper<Device>()
                    .ne(Device::getId, current.getId())
                    .eq(Device::getBindingLocation, dto.getBindingLocation())
                    .eq(Device::getLocationType, dto.getLocationType())
                    .eq(Device::getProductKey, current.getProductKey());
            // 随身设备的位置由老人 ID 标识，也能识别旧记录中的 -1。
            if (!wearable)
            {
                binding.eq(physicalLocationType != null, Device::getPhysicalLocationType, physicalLocationType)
                        .isNull(physicalLocationType == null, Device::getPhysicalLocationType);
            }
            if (deviceMapper.selectCount(binding) > 0)
            {
                throw new ServiceException("该位置已绑定相同产品");
            }
        }

        String description = dto.getDeviceDescription() == null
                ? current.getDeviceDescription() : dto.getDeviceDescription();
        updateCloudDevice(current.getIotId(), dto.getDeviceName(), description);
        try
        {
            // 显式写入可编辑字段，允许固定设备改为随身设备时清除物理位置类型。
            int updated = deviceMapper.update(new Device(), new LambdaUpdateWrapper<Device>()
                    .eq(Device::getId, current.getId())
                    .set(Device::getDeviceName, dto.getDeviceName())
                    .set(Device::getLocationType, dto.getLocationType())
                    .set(Device::getBindingLocation, dto.getBindingLocation())
                    .set(Device::getPhysicalLocationType, physicalLocationType)
                    .set(dto.getRemark() != null, Device::getRemark, dto.getRemark())
                    .set(dto.getDeviceDescription() != null, Device::getDeviceDescription, description));
            if (updated == 0)
            {
                throw new ServiceException("本地设备更新失败");
            }
        }
        catch (RuntimeException ex)
        {
            try
            {
                updateCloudDevice(current.getIotId(), current.getDeviceName(),
                        current.getDeviceDescription() == null ? "" : current.getDeviceDescription());
            }
            catch (ServiceException rollbackError)
            {
                log.error("本地设备更新失败且云端恢复失败，设备ID：{}", current.getIotId());
                throw new ServiceException("本地设备更新失败且云端恢复失败，请联系管理员核查设备");
            }
            throw new ServiceException("本地设备更新失败，云端修改已撤销");
        }
    }

    /**
     * 更新华为云设备名称和描述。
     */
    private void updateCloudDevice(String iotId, String deviceName, String description)
    {
        UpdateDeviceResponse response;
        try
        {
            response = client.updateDevice(new UpdateDeviceRequest().withDeviceId(iotId)
                    .withBody(new UpdateDevice().withDeviceName(deviceName).withDescription(description)));
        }
        catch (Exception ex)
        {
            throw new ServiceException("华为云设备更新失败，请稍后重试");
        }
        if (response == null || response.getHttpStatusCode() != 200)
        {
            throw new ServiceException("华为云设备更新失败");
        }
    }

    /**
     * 展开各服务的 reported 属性，排除应用下发的 desired 数据。
     *
     * @param iotId 华为云设备 ID
     * @return 设备最近一次上报的属性列表
     */
    @Override
    public List<DeviceServicePropertyVo> queryServiceProperties(String iotId)
    {
        ShowDeviceShadowResponse response;
        try
        {
            response = client.showDeviceShadow(new ShowDeviceShadowRequest().withDeviceId(iotId));
        }
        catch (Exception ex)
        {
            throw new ServiceException("查询设备上报数据失败，请稍后重试");
        }
        if (response == null || response.getHttpStatusCode() != 200)
        {
            throw new ServiceException("查询设备上报数据失败");
        }
        List<DeviceServicePropertyVo> result = new ArrayList<>();
        if (CollUtil.isEmpty(response.getShadow()))
        {
            return result;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX");
        for (DeviceShadowData shadow : response.getShadow())
        {
            if (shadow == null || shadow.getReported() == null)
            {
                continue;
            }
            DeviceShadowProperties reported = shadow.getReported();
            if (reported.getProperties() == null)
            {
                continue;
            }
            if (!(reported.getProperties() instanceof Map<?, ?> properties))
            {
                throw new ServiceException("设备上报属性数据格式错误");
            }
            if (properties.isEmpty())
            {
                continue;
            }

            // 每个服务拥有各自的上报时间，不能统一使用第一个服务的时间。
            LocalDateTime eventTime = null;
            if (StringUtils.hasText(reported.getEventTime()))
            {
                try
                {
                    eventTime = OffsetDateTime.parse(reported.getEventTime(), formatter)
                            .atZoneSameInstant(ZoneId.of("Asia/Shanghai")).toLocalDateTime();
                }
                catch (DateTimeParseException ex)
                {
                    throw new ServiceException("设备上报时间格式错误");
                }
            }
            for (Map.Entry<?, ?> property : properties.entrySet())
            {
                DeviceServicePropertyVo item = new DeviceServicePropertyVo();
                item.setFunctionId(String.valueOf(property.getKey()));
                item.setEventTime(eventTime);
                item.setValue(property.getValue());
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 本地资料为主体，云端补充实时状态及激活时间。
     *
     * @param iotId 华为云设备 ID
     * @return 合并后的设备详情
     */
    @Override
    public DeviceDetailVo selectDeviceDetail(String iotId)
    {
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getIotId, iotId));
        if (device == null)
        {
            throw new ServiceException("设备不存在");
        }

        ShowDeviceResponse response;
        try
        {
            response = client.showDevice(new ShowDeviceRequest().withDeviceId(iotId));
        }
        catch (Exception ex)
        {
            throw new ServiceException("查询华为云设备信息失败，请稍后重试");
        }
        if (response == null || response.getHttpStatusCode() != 200)
        {
            throw new ServiceException("查询华为云设备信息失败");
        }

        DeviceDetailVo detail = new DeviceDetailVo();
        BeanUtils.copyProperties(device, detail);
        // 表中绑定位置为字符串，创建人由基类以字符串承载；详情接口统一返回数值 ID。
        detail.setBindingLocation(StringUtils.hasText(device.getBindingLocation())
                ? Long.valueOf(device.getBindingLocation()) : null);
        detail.setCreateBy(StringUtils.hasText(device.getCreateBy())
                ? Long.valueOf(device.getCreateBy()) : null);
        detail.setDeviceStatus(response.getStatus());
        String activeTime = response.getActiveTime();
        // 未激活设备可能没有激活时间，此时保持为空。
        if (StringUtils.hasText(activeTime))
        {
            try
            {
                detail.setActiveTime(OffsetDateTime.parse(activeTime)
                        .atZoneSameInstant(ZoneId.of("Asia/Shanghai")).toLocalDateTime());
            }
            catch (DateTimeParseException ex)
            {
                throw new ServiceException("华为云设备激活时间格式错误");
            }
        }
        return detail;
    }

    /**
     * 注册设备。按表中位置联合唯一键校验，避免不同位置类型的同号 ID 互相冲突。
     *
     * @param dto 设备注册参数
     */
    @Override
    public void register(DeviceRegisterDto dto)
    {
        if (deviceMapper.selectCount(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceName, dto.getDeviceName())) > 0)
        {
            throw new ServiceException("设备名称已存在");
        }
        if (deviceMapper.selectCount(new LambdaQueryWrapper<Device>()
                .eq(Device::getNodeId, dto.getNodeId())) > 0)
        {
            throw new ServiceException("设备标识符已存在");
        }
        if (deviceMapper.selectCount(new LambdaQueryWrapper<Device>()
                .eq(Device::getBindingLocation, dto.getBindingLocation())
                .eq(Device::getLocationType, dto.getLocationType())
                .eq(dto.getPhysicalLocationType() != null,
                        Device::getPhysicalLocationType, dto.getPhysicalLocationType())
                .isNull(dto.getPhysicalLocationType() == null, Device::getPhysicalLocationType)
                .eq(Device::getProductKey, dto.getProductKey())) > 0)
        {
            throw new ServiceException("该位置已绑定相同产品");
        }

        Device device = new Device();
        BeanUtils.copyProperties(dto, device);
        device.setSecret(IdUtil.simpleUUID());
        device.setHaveEntranceGuard(0);
        device.setCreateTime(DateUtils.getNowDate());
        AddDevice body = new AddDevice()
                .withDeviceName(device.getDeviceName())
                .withNodeId(device.getNodeId())
                .withProductId(device.getProductKey())
                .withDescription(device.getDeviceDescription())
                .withAuthInfo(new AuthInfo().withAuthType("SECRET").withSecret(device.getSecret()));
        AddDeviceResponse response;
        try
        {
            response = client.addDevice(new AddDeviceRequest().withBody(body));
        }
        catch (Exception ex)
        {
            // 云端异常内容可能含鉴权数据，不将原始响应暴露给调用方。
            throw new ServiceException("华为云设备注册失败，请稍后重试");
        }
        if (response == null || response.getHttpStatusCode() != 201)
        {
            throw new ServiceException("华为云设备注册失败");
        }
        if (!StringUtils.hasText(response.getDeviceId()))
        {
            throw new ServiceException("华为云设备注册响应缺少设备ID，请核查云端设备状态");
        }
        device.setIotId(response.getDeviceId());

        // 本地只有一次插入；数据库事务不能回滚云端操作，插入失败时补偿删除本次创建的设备。
        try
        {
            if (!save(device))
            {
                throw new ServiceException("本地设备保存失败");
            }
        }
        catch (Exception ex)
        {
            compensateRegistration(device.getIotId());
            throw new ServiceException("本地设备保存失败，云端注册已撤销，请重试");
        }
    }

    /**
     * 撤销本次注册，补偿失败时提示人工核查，不返回成功。
     *
     * @param iotId 本次注册的云端设备 ID
     */
    private void compensateRegistration(String iotId)
    {
        try
        {
            DeleteDeviceResponse response = client.deleteDevice(new DeleteDeviceRequest().withDeviceId(iotId));
            if (response != null && (response.getHttpStatusCode() == 204 || response.getHttpStatusCode() == 404))
            {
                return;
            }
        }
        catch (Exception ex)
        {
            // 下方统一报告补偿失败，不记录可能包含设备密钥的 SDK 异常正文。
        }
        log.error("设备注册本地保存失败且云端撤销失败，需核查云端设备：{}", iotId);
        throw new ServiceException("本地设备保存失败且云端撤销失败，请联系管理员核查云端设备：" + iotId);
    }

    /**
     * 查询设备
     * 
     * @param id 设备主键
     * @return 设备
     */
    @Override
    public Device selectDeviceById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询设备列表
     * 
     * @param device 设备
     * @return 设备
     */
    @Override
    public List<Device> selectDeviceList(Device device)
    {
        return deviceMapper.selectDeviceList(device);
    }

    /**
     * 新增设备
     * 
     * @param device 设备
     * @return 结果
     */
    @Override
    public int insertDevice(Device device)
    {
        return save(device)? 1 : 0;
    }

    /**
     * 修改设备
     * 
     * @param device 设备
     * @return 结果
     */
    @Override
    public int updateDevice(Device device)
    {
        return updateById(device)? 1 : 0;
    }

    /**
     * 批量删除设备
     * 
     * @param ids 需要删除的设备主键
     * @return 结果
     */
    @Override
    public int deleteDeviceByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids))? 1 : 0;
    }

    /**
     * 删除设备信息
     * 
     * @param id 设备主键
     * @return 结果
     */
    @Override
    public int deleteDeviceById(Long id)
    {
        return removeById(id)? 1 : 0;
    }

    /**
     * 从物联网平台同步产品列表,存入redis
     */
    @Override
    public void syncProductList() {
        //1,调用IOTDA平台接口查询产品列表
        ListProductsRequest request = new ListProductsRequest();
        request.setLimit(50);
        ListProductsResponse response = client.listProducts(request);
        if(response.getHttpStatusCode()!=200){
            throw  new BaseException("同步产品列表失败");
        }

        //2,将产品列表转为json字符串
        List<ProductSummary> products = response.getProducts();
        if(CollUtil.isEmpty(products)){
            return;
        }
        String jsonStr = JSONUtil.toJsonStr(products);

        //3,存入redis的String类型
        String key = CacheConstants.IOT_PRODUCT_LIST;
        redisTemplate.opsForValue().set(key,jsonStr);
    }

    /**
     * 查询全部产品列表
     *
     * @return
     */
    @Override
    public List<ProductVo> allProduct() {
        // 根据key从redis查询产品列表的JSON字符串
        String s = redisTemplate.opsForValue().get(CacheConstants.IOT_PRODUCT_LIST);
        //将JSON字符转换为产品列表
        List<ProductVo> list = JSONUtil.toList(s, ProductVo.class);

        //判断是否为空
        if(CollUtil.isEmpty(list)){
            return List.of();
        }

        //返回数据
        return list;
    }
}
