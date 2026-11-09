package com.xhzb.nursing.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "房间VO")
public class RoomVo {

    @Schema(title = "房间ID")
    private Long id;

    @Schema(title = "楼层名称")
    private String floorName;

    @Schema(title = "楼层ID")
    private String floorId;

    @Schema(title = "房间ID")
    private String roomId;

    @Schema(title = "房间编号")
    private String code;

    @Schema(title = "房间价格")
    private String price;

    @Schema(title = "床位列表")
    private List<BedVo> bedVoList;

    /** 绑定在房间上的固定设备。 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<RoomDeviceVo> deviceVos;

}
