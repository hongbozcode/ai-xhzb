package com.xhzb.nursing.domain.vo;

import lombok.Data;

import java.util.List;

/** 房间或床位绑定的设备及其最新数据。 */
@Data
public class RoomDeviceVo
{
    /** 设备主键。 */
    private Long id;

    /** IoTDA 设备 ID。 */
    private String iotId;

    /** 设备名称。 */
    private String deviceName;

    /** 所属产品标识。 */
    private String productKey;

    /** 所属产品名称。 */
    private String productName;

    /** 设备最新一批上报数据。 */
    private List<DeviceDataVo> deviceDataVos = List.of();
}
