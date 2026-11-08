package com.xhzb.nursing.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备修改参数，兼容前端提交包含只读字段的表单。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceUpdateDto
{
    /** 本地设备主键 */
    @NotNull(message = "设备主键不能为空")
    private Long id;

    /** 设备名称 */
    @NotBlank(message = "设备名称不能为空")
    @Size(max = 100, message = "设备名称不能超过100个字符")
    private String deviceName;

    /** 绑定的老人或物理位置 ID */
    @NotBlank(message = "绑定位置不能为空")
    @Size(max = 50, message = "绑定位置不能超过50个字符")
    private String bindingLocation;

    /** 位置类型，取值来自数据字典 */
    @NotNull(message = "位置类型不能为空")
    private Integer locationType;

    /** 物理位置类型，随身设备不使用此字段 */
    private Integer physicalLocationType;

    /** 随接入位置更新的老人姓名或位置描述 */
    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

    /** 用于接入位置回显的层级 ID */
    @Size(max = 255, message = "设备描述不能超过255个字符")
    private String deviceDescription;
}
