package com.xhzb.nursing.domain.dto;

import java.util.List;
import lombok.Data;

/** 一个老人及其本次选中的护理员。 */
@Data
public class NursingElderAssignmentDto
{
    /** 老人编号。 */
    private Long elderId;

    /** 负责该老人的护理员用户编号。 */
    private List<Long> nursingIds;
}
