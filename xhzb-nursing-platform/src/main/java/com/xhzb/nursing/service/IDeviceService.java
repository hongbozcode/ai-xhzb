package com.xhzb.nursing.service;

import java.util.List;
import com.xhzb.nursing.domain.Device;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xhzb.nursing.domain.vo.ProductVo;
import com.xhzb.nursing.domain.vo.DeviceDetailVo;
import com.xhzb.nursing.domain.vo.DeviceServicePropertyVo;
import com.xhzb.nursing.domain.dto.DeviceRegisterDto;
import com.xhzb.nursing.domain.dto.DeviceUpdateDto;

/**
 * 设备Service接口
 * 
 * @author hongbozcode
 * @date 2026-11-06
 */
public interface IDeviceService extends IService<Device>
{
    /**
     * 根据物联网设备 ID 先删除本地记录，再删除华为云设备。
     *
     * @param iotId 华为云设备 ID
     */
    void deleteDeviceByIotId(String iotId);

    /**
     * 更新云端设备信息后保存本地设备名称及绑定位置。
     *
     * @param dto 设备修改参数
     */
    void modifyDevice(DeviceUpdateDto dto);

    /**
     * 查询设备影子中的上报属性，并转换上报时间为北京时间。
     *
     * @param iotId 华为云设备 ID
     * @return 设备上报属性列表
     */
    List<DeviceServicePropertyVo> queryServiceProperties(String iotId);

    /**
     * 查询设备详情，合并本地资料与云端状态、激活时间。
     *
     * @param iotId 华为云设备 ID
     * @return 设备详情
     */
    DeviceDetailVo selectDeviceDetail(String iotId);

    /**
     * 校验设备唯一性，注册华为云设备并保存到本地。
     *
     * @param dto 设备注册参数
     */
    void register(DeviceRegisterDto dto);

    /**
     * 查询设备
     * 
     * @param id 设备主键
     * @return 设备
     */
    public Device selectDeviceById(Long id);

    /**
     * 查询设备列表
     * 
     * @param device 设备
     * @return 设备集合
     */
    public List<Device> selectDeviceList(Device device);

    /**
     * 新增设备
     * 
     * @param device 设备
     * @return 结果
     */
    public int insertDevice(Device device);

    /**
     * 修改设备
     * 
     * @param device 设备
     * @return 结果
     */
    public int updateDevice(Device device);

    /**
     * 批量删除设备
     * 
     * @param ids 需要删除的设备主键集合
     * @return 结果
     */
    public int deleteDeviceByIds(Long[] ids);

    /**
     * 删除设备信息
     * 
     * @param id 设备主键
     * @return 结果
     */
    public int deleteDeviceById(Long id);

    /**
     * 从物联网平台同步产品列表
     */
    void syncProductList();

    /**
     * 查询全部产品列表
     * @return
     */
    List<ProductVo> allProduct();

}
