package com.xhzb.nursing.domain.vo;

import lombok.Data;
import java.util.List;

/** 楼层房间床位级联节点。 */
@Data
public class CascaderVo {
    /** 节点ID */
    private String value;
    /** 节点名称 */
    private String label;
    /** 子节点 */
    private List<CascaderVo> children;
}
