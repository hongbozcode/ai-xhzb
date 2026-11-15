package com.xhzb.nursing.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/** 一次报警已为各接收人提交记录后发出的通知事件。 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlertCreatedEvent {

    /** 同一次报警生成的待处理记录 ID。 */
    private List<Long> alertDataIds;
}
