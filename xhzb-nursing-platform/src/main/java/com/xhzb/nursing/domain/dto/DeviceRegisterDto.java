package com.xhzb.nursing.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备注册参数，仅接收业务字段，设备 ID 和密钥由服务端生成。
 */
@Data
public class DeviceRegisterDto
{
    /** 设备名称 */
    @NotBlank(message = "设备名称不能为空")
    @Size(max = 100, message = "设备名称不能超过100个字符")
    private String deviceName;

    /** 绑定的老人、楼层、房间或床位 ID */
    @NotBlank(message = "绑定位置不能为空")
    @Size(max = 50, message = "绑定位置不能超过50个字符")
    private String bindingLocation;

    /** 位置类型：0 随身设备，1 固定设备 */
    @NotNull(message = "位置类型不能为空")
    private Integer locationType;

    /** 设备节点标识符 */
    @NotBlank(message = "设备标识符不能为空")
    @Size(max = 50, message = "设备标识符不能超过50个字符")
    @Pattern(regexp = "[a-zA-Z0-9_-]+", message = "设备标识符只能包含字母、数字、下划线和连接符")
    private String nodeId;

    /** 物理位置类型：随身设备为 null；固定设备按数据字典传入 */
    private Integer physicalLocationType;

    /** 华为云产品 ID */
    @NotBlank(message = "产品ID不能为空")
    @Size(max = 36, message = "产品ID不能超过36个字符")
    @Pattern(regexp = "[a-zA-Z0-9_-]+", message = "产品ID只能包含字母、数字、下划线和连接符")
    private String productKey;

    /** 产品名称 */
    @NotBlank(message = "产品名称不能为空")
    @Size(max = 100, message = "产品名称不能超过100个字符")
    private String productName;

    /** 老人姓名或位置的文字描述 */
    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

    /** 用于回显的老人 ID 或位置层级 ID 列表，按请求原样保存 */
    @Size(max = 255, message = "设备描述不能超过255个字符")
    private String deviceDescription;

}
