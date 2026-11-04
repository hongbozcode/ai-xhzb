package com.xhzb.nursing.domain.vo;

import lombok.Data;

/** 入住房间及价格。 */
@Data
public class CheckInRoomVo {
    /** 楼层名称 */
    private String floorName;
    /** 楼层ID */
    private String floorId;
    /** 房间ID */
    private String roomId;
    /** 房间编号 */
    private String code;
    /** 床位价格 */
    private String price;
}
