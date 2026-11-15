package com.xhzb.nursing.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xhzb.common.annotation.Log;
import com.xhzb.common.core.controller.BaseController;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.enums.BusinessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.xhzb.nursing.domain.NursingTask;
import com.xhzb.nursing.service.INursingTaskService;
import com.xhzb.common.utils.poi.ExcelUtil;
import com.xhzb.common.core.page.TableDataInfo;

/**
 * 护理任务Controller
 * 
 * @author hongbozcode
 * @date 2026-11-09
 */
@RestController
@RequestMapping("/nursing/nursingTask")
@Tag(name = "护理任务相关接口")
public class NursingTaskController extends BaseController
{
    @Autowired
    private INursingTaskService nursingTaskService;

    /**
     * 查询护理任务列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:nursingTask:list')")
    @GetMapping("/list")
    @Operation(summary = "查询护理任务列表")
    public TableDataInfo list(NursingTask nursingTask)
    {
        startPage();
        List<NursingTask> list = nursingTaskService.selectNursingTaskList(nursingTask);
        return getDataTable(list);
    }

    /**
     * 导出护理任务列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:nursingTask:export')")
    @Log(title = "护理任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @Operation(summary = "导出护理任务列表")
    public void export(HttpServletResponse response, NursingTask nursingTask)
    {
        List<NursingTask> list = nursingTaskService.selectNursingTaskList(nursingTask);
        ExcelUtil<NursingTask> util = new ExcelUtil<NursingTask>(NursingTask.class);
        util.exportExcel(response, list, "护理任务数据");
    }

    /**
     * 获取护理任务详细信息
     */
    @PreAuthorize("@ss.hasPermi('nursing:nursingTask:query')")
    @GetMapping(value = "/{id}")
    @Operation(summary = "获取护理任务详细信息")
    public AjaxResult getInfo(@Schema(name = "护理任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
            @PathVariable("id") Long id)
    {
        return success(nursingTaskService.selectNursingTaskById(id));
    }

    /**
     * 新增护理任务
     */
    @PreAuthorize("@ss.hasPermi('nursing:nursingTask:add')")
    @Log(title = "护理任务", businessType = BusinessType.INSERT)
    @PostMapping
    @Operation(summary = "新增护理任务")
    public AjaxResult add(@RequestBody NursingTask nursingTask)
    {
        return toAjax(nursingTaskService.insertNursingTask(nursingTask));
    }

    /**
     * 修改护理任务
     */
    @PreAuthorize("@ss.hasPermi('nursing:nursingTask:edit')")
    @Log(title = "护理任务", businessType = BusinessType.UPDATE)
    @PutMapping
    @Operation(summary = "修改护理任务")
    public AjaxResult edit(@RequestBody NursingTask nursingTask)
    {
        return toAjax(nursingTaskService.updateNursingTask(nursingTask));
    }

    /**
     * 删除护理任务
     */
    @PreAuthorize("@ss.hasPermi('nursing:nursingTask:remove')")
    @Log(title = "护理任务", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    @Operation(summary = "删除护理任务")
    public AjaxResult remove(@Schema(name = "护理任务ID", requiredMode = Schema.RequiredMode.REQUIRED) @PathVariable Long[] ids)
    {
        return toAjax(nursingTaskService.deleteNursingTaskByIds(ids));
    }
}
