package com.xhzb.nursing.domain.vo;

import java.util.List;

/** 楼层房间床位级联节点。 */
public class CascaderVo {
    /** 节点ID */
    private String value;
    /** 节点名称 */
    private String label;
    /** 子节点 */
    private List<CascaderVo> children;
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public List<CascaderVo> getChildren() { return children; }
    public void setChildren(List<CascaderVo> children) { this.children = children; }
}
