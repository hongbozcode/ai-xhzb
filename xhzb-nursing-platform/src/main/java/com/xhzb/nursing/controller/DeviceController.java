package com.xhzb.nursing.controller;

import java.util.List;

import com.xhzb.nursing.domain.vo.ProductVo;
import com.xhzb.nursing.domain.vo.DeviceDetailVo;
import com.xhzb.nursing.domain.vo.DeviceServicePropertyVo;
import com.xhzb.nursing.domain.dto.DeviceRegisterDto;
import com.xhzb.nursing.domain.dto.DeviceUpdateDto;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xhzb.common.annotation.Log;
import com.xhzb.common.core.controller.BaseController;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.enums.BusinessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.xhzb.nursing.domain.Device;
import com.xhzb.nursing.service.IDeviceService;
import com.xhzb.common.utils.poi.ExcelUtil;
import com.xhzb.common.core.page.TableDataInfo;

/**
 * 设备Controller
 * 
 * @author hongbozcode
 * @date 2026-11-06
 */
@RestController
@RequestMapping("/nursing/device")
@Tag(name = "设备相关接口")
public class DeviceController extends BaseController
{
    @Autowired
    private IDeviceService deviceService;

    /**
     * 删除本地设备及其在华为云上的记录。
     *
     * @param iotId 华为云设备 ID
     * @return 操作结果
     */
    @DeleteMapping("/{iotId}")
    @Operation(summary = "删除设备")
    public AjaxResult remove(@PathVariable("iotId") String iotId)
    {
        deviceService.deleteDeviceByIotId(iotId);
        return success();
    }

    /**
     * 修改设备名称、位置类型和接入位置。
     *
     * @param dto 设备修改参数
     * @return 操作结果
     */
    @PutMapping
    @Operation(summary = "修改设备")
    public AjaxResult edit(@Valid @RequestBody DeviceUpdateDto dto)
    {
        deviceService.modifyDevice(dto);
        return success();
    }

    /**
     * 查询设备最近一次上报的属性数据。
     *
     * @param iotId 华为云设备 ID
     * @return 设备上报属性列表
     */
    @GetMapping("/queryServiceProperties/{iotId}")
    @Operation(summary = "查看设备上报的数据")
    public AjaxResult queryServiceProperties(@PathVariable("iotId") String iotId)
    {
        List<DeviceServicePropertyVo> properties = deviceService.queryServiceProperties(iotId);
        return success(properties);
    }

    /**
     * 根据物联网设备 ID 查询本地资料及云端运行信息。
     *
     * @param iotId 华为云设备 ID
     * @return 设备详情
     */
    @GetMapping("/{iotId}")
    @Operation(summary = "查询设备详细信息")
    public AjaxResult getInfo(@PathVariable("iotId") String iotId)
    {
        DeviceDetailVo detail = deviceService.selectDeviceDetail(iotId);
        return success(detail);
    }

    /**
     * 注册设备，云端创建成功后保存本地设备信息。
     */
    @PostMapping("/register")
    @Operation(summary = "注册设备")
    public AjaxResult register(@Valid @RequestBody DeviceRegisterDto dto)
    {
        deviceService.register(dto);
        return success();
    }

    /**
     * 查询设备列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:device:list')")
    @GetMapping("/list")
    @Operation(summary = "查询设备列表")
    public TableDataInfo list(Device device)
    {
        startPage();
        List<Device> list = deviceService.selectDeviceList(device);
        return getDataTable(list);
    }
    /**
    * 同步产品列表
     */

    @PostMapping("/syncProductList")
    public AjaxResult syncProductList(){
        deviceService.syncProductList();
        return success();

    }
    /**
     *查询全部产品列表
     */
    @GetMapping("/allProduct")
    public AjaxResult allProduct(){
        List<ProductVo> productVoList=deviceService.allProduct();
        return success(productVoList);
    }

}
