package com.xhzb.nursing.domain.vo;


/** 入住房间及价格。 */
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
    public String getFloorName() { return floorName; }
    public void setFloorName(String floorName) { this.floorName = floorName; }
    public String getFloorId() { return floorId; }
    public void setFloorId(String floorId) { this.floorId = floorId; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
}
