package com.xhzb.nursing.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/** 楼层设备查询返回的上报记录。 */
@Data
public class DeviceDataVo
{
    /** 设备数据主键。 */
    private Long id;

    /** 入库时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;

    /** 更新时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date updateTime;

    /** 数据备注。 */
    private String remark;

    /** IoTDA 设备 ID。 */
    private String iotId;

    /** 设备名称。 */
    private String deviceName;

    /** 设备别名，当前数据表未提供时为空。 */
    private String nickname;

    /** 所属产品标识。 */
    private String productKey;

    /** 所属产品名称。 */
    private String productName;

    /** 上报属性名。 */
    private String functionId;

    /** 设备绑定位置。 */
    private String accessLocation;

    /** 设备位置类型。 */
    private Integer locationType;

    /** 固定设备的物理位置类型。 */
    private Integer physicalLocationType;

    /** 设备位置备注。 */
    private String deviceDescription;

    /** 上报属性值。 */
    private String dataValue;

    /** 转换为北京时间后的上报时间。 */
    private LocalDateTime alarmTime;
}
