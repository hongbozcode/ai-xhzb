package com.xhzb.nursing.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhzb.common.constant.UserConstants;
import com.xhzb.common.core.domain.entity.SysUser;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.nursing.domain.Elder;
import com.xhzb.nursing.domain.NursingElder;
import com.xhzb.nursing.domain.dto.NursingElderAssignmentDto;
import com.xhzb.nursing.mapper.ElderMapper;
import com.xhzb.nursing.mapper.NursingElderMapper;
import com.xhzb.nursing.service.INursingElderService;
import com.xhzb.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 护理员老人关联Service业务层处理
 * 
 * @author hongbozcode
 * @date 2026-11-09
 */
@Service
@RequiredArgsConstructor
public class NursingElderServiceImpl extends ServiceImpl<NursingElderMapper, NursingElder> implements INursingElderService
{
    /** “负责老人”页面用于查询护理员选项的部门编号。 */
    private static final long NURSING_DEPT_ID = 201L;

    private final ElderMapper elderMapper;
    private final ISysUserService userService;

    /** 校验并保存整批分配，保留仍在本次选择中的现有关系。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setNursing(List<NursingElderAssignmentDto> assignments)
    {
        if (assignments == null || assignments.isEmpty())
        {
            throw new ServiceException("请至少选择一位老人");
        }

        Map<Long, Set<Long>> pendingByElder = new LinkedHashMap<>();
        Set<Long> requestedNursingIds = new HashSet<>();
        for (NursingElderAssignmentDto assignment : assignments)
        {
            if (assignment == null || assignment.getElderId() == null || assignment.getElderId() <= 0)
            {
                throw new ServiceException("老人编号无效");
            }
            List<Long> nursingIds = assignment.getNursingIds();
            if (nursingIds == null || nursingIds.isEmpty()
                    || nursingIds.stream().anyMatch(id -> id == null || id <= 0))
            {
                throw new ServiceException("请选择有效的护理员");
            }
            if (pendingByElder.putIfAbsent(assignment.getElderId(), new LinkedHashSet<>(nursingIds)) != null)
            {
                throw new ServiceException("同一老人只能提交一组护理员");
            }
            requestedNursingIds.addAll(nursingIds);
        }

        // 同一老人的并发设置按老人主键加锁，包括尚无分配记录的老人。
        List<Elder> elders = elderMapper.selectList(Wrappers.<Elder>lambdaQuery()
                .select(Elder::getId).in(Elder::getId, pendingByElder.keySet())
                .orderByAsc(Elder::getId).last("FOR UPDATE"));
        if (elders.size() != pendingByElder.size())
        {
            throw new ServiceException("选中的老人已不存在，请刷新后重试");
        }
        validateNursingUsers(requestedNursingIds);

        List<NursingElder> currentAssignments = lambdaQuery()
                .in(NursingElder::getElderId, pendingByElder.keySet()).list();
        List<Long> removedIds = new ArrayList<>();
        for (NursingElder current : currentAssignments)
        {
            if (!pendingByElder.get(current.getElderId()).remove(current.getNursingId()))
            {
                removedIds.add(current.getId());
            }
        }
        List<NursingElder> newAssignments = new ArrayList<>();
        pendingByElder.forEach((elderId, nursingIds) -> nursingIds.forEach(nursingId -> {
            NursingElder relation = new NursingElder();
            relation.setElderId(elderId);
            relation.setNursingId(nursingId);
            newAssignments.add(relation);
        }));
        if (!removedIds.isEmpty() && !removeByIds(removedIds))
        {
            throw new ServiceException("移除原护理员关联失败");
        }
        if (!newAssignments.isEmpty() && !saveBatch(newAssignments))
        {
            throw new ServiceException("保存护理员关联失败");
        }
    }

    /** 复用页面护理员选项的部门、账号状态和数据权限。 */
    private void validateNursingUsers(Set<Long> nursingIds)
    {
        SysUser query = new SysUser();
        query.setDeptId(NURSING_DEPT_ID);
        query.setStatus(UserConstants.NORMAL);
        Set<Long> selectableIds = userService.selectUserList(query).stream()
                .map(SysUser::getUserId).collect(Collectors.toSet());
        if (!selectableIds.containsAll(nursingIds))
        {
            throw new ServiceException("有护理员已停用或不在可选范围内，请刷新后重试");
        }
    }

    /**
     * 查询护理员老人关联
     * 
     * @param id 护理员老人关联主键
     * @return 护理员老人关联
     */
    @Override
    public NursingElder selectNursingElderById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询护理员老人关联列表
     * 
     * @param nursingElder 护理员老人关联
     * @return 护理员老人关联
     */
    @Override
    public List<NursingElder> selectNursingElderList(NursingElder nursingElder)
    {
        return baseMapper.selectNursingElderList(nursingElder);
    }

    /**
     * 新增护理员老人关联
     * 
     * @param nursingElder 护理员老人关联
     * @return 结果
     */
    @Override
    public int insertNursingElder(NursingElder nursingElder)
    {
        return save(nursingElder)? 1 : 0;
    }

    /**
     * 修改护理员老人关联
     * 
     * @param nursingElder 护理员老人关联
     * @return 结果
     */
    @Override
    public int updateNursingElder(NursingElder nursingElder)
    {
        return updateById(nursingElder)? 1 : 0;
    }

    /**
     * 批量删除护理员老人关联
     * 
     * @param ids 需要删除的护理员老人关联主键
     * @return 结果
     */
    @Override
    public int deleteNursingElderByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids))? 1 : 0;
    }

    /**
     * 删除护理员老人关联信息
     * 
     * @param id 护理员老人关联主键
     * @return 结果
     */
    @Override
    public int deleteNursingElderById(Long id)
    {
        return removeById(id)? 1 : 0;
    }
}
