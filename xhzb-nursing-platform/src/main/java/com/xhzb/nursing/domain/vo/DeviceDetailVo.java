package com.xhzb.nursing.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;

/**
 * 本地设备资料与华为云运行信息合并后的设备详情。
 */
@Data
@Schema(description = "设备详细信息")
public class DeviceDetailVo
{
    /** 本地设备主键 */
    private Long id;

    /** 华为云设备 ID */
    private String iotId;

    /** 设备名称 */
    private String deviceName;

    /** 设备节点标识符 */
    private String nodeId;

    /** 本地保存的设备密钥 */
    private String secret;

    /** 产品 ID */
    private String productKey;

    /** 产品名称 */
    private String productName;

    /** 位置类型，取值来自数据字典 */
    private Integer locationType;

    /** 绑定的老人或物理位置 ID */
    private Long bindingLocation;

    /** 老人姓名或位置描述 */
    private String remark;

    /** 华为云设备状态 */
    private String deviceStatus;

    /** 设备激活时间，北京时间；未激活时为空 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime activeTime;

    /** 本地设备创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;

    /** 创建人 ID */
    private Long createBy;

    /** 创建人名称，未提供时为空 */
    private String creator;
}
