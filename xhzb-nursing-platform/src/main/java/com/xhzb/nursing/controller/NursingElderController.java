package com.xhzb.nursing.controller;

import java.util.List;
import com.xhzb.common.annotation.Log;
import com.xhzb.common.core.controller.BaseController;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.enums.BusinessType;
import com.xhzb.nursing.domain.dto.NursingElderAssignmentDto;
import com.xhzb.nursing.service.INursingElderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 护理员与老人的分配接口。 */
@RestController
@RequestMapping("/elder/nursingElder")
@RequiredArgsConstructor
public class NursingElderController extends BaseController
{
    private final INursingElderService nursingElderService;

    /** 批量设置负责每位老人的护理员。 */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "负责老人", businessType = BusinessType.UPDATE)
    @PostMapping("/setNursing")
    @Operation(summary = "设置负责老人的护理员")
    public AjaxResult setNursing(@RequestBody List<NursingElderAssignmentDto> assignments)
    {
        nursingElderService.setNursing(assignments);
        return success();
    }
}
