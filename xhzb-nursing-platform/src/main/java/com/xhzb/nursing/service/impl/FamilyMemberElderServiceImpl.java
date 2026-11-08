package com.xhzb.nursing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xhzb.common.constant.HttpStatus;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.common.utils.PageUtils;
import com.xhzb.common.utils.UserThreadLocal;
import com.xhzb.nursing.domain.Elder;
import com.xhzb.nursing.domain.FamilyMemberElder;
import com.xhzb.nursing.domain.dto.FamilyMemberElderBindDto;
import com.xhzb.nursing.domain.vo.FamilyMemberElderDetailVo;
import com.xhzb.nursing.domain.vo.FamilyMemberElderVo;
import com.xhzb.nursing.mapper.ElderMapper;
import com.xhzb.nursing.mapper.FamilyMemberElderMapper;
import com.xhzb.nursing.mapper.FamilyMemberMapper;
import com.xhzb.nursing.service.IFamilyMemberElderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 小程序家人绑定及查询。
 */
@Service
@RequiredArgsConstructor
public class FamilyMemberElderServiceImpl implements IFamilyMemberElderService
{
    private final FamilyMemberElderMapper familyMemberElderMapper;
    private final FamilyMemberMapper familyMemberMapper;
    private final ElderMapper elderMapper;

    @Override
    public void bindFamily(FamilyMemberElderBindDto bindDto)
    {
        Long familyMemberId = currentMemberId();
        if (familyMemberMapper.selectById(familyMemberId) == null)
        {
            throw new ServiceException("家属账号不存在，请重新登录", HttpStatus.UNAUTHORIZED);
        }
        Elder elder = elderMapper.selectOne(new LambdaQueryWrapper<Elder>()
                .select(Elder::getId)
                .eq(Elder::getName, bindDto.getName().strip())
                .eq(Elder::getIdCardNo, bindDto.getIdCard().strip()));
        if (elder == null)
        {
            throw new ServiceException("未找到匹配的老人，请核对姓名和身份证号");
        }

        FamilyMemberElder binding = new FamilyMemberElder();
        binding.setFamilyMemberId(familyMemberId);
        binding.setElderId(elder.getId());
        binding.setRemark(bindDto.getRemark());
        binding.setCreateTime(DateUtils.getNowDate());
        binding.setCreateBy(familyMemberId.toString());
        try
        {
            if (familyMemberElderMapper.insert(binding) != 1)
            {
                throw new ServiceException("绑定家人失败，请重试");
            }
        }
        catch (DuplicateKeyException e)
        {
            // 由家属和老人联合唯一键处理重复提交，保留首次保存的称呼。
            return;
        }
    }

    @Override
    public List<FamilyMemberElderVo> listMyFamily()
    {
        return familyMemberElderMapper.selectMyFamily(currentMemberId());
    }

    @Override
    public List<FamilyMemberElderDetailVo> listMyFamilyByPage(int pageNum, int pageSize)
    {
        Long familyMemberId = currentMemberId();
        if (pageNum < 1 || pageSize < 1)
        {
            throw new ServiceException("页码和每页条数必须大于0", HttpStatus.BAD_REQUEST);
        }
        // 接口仅返回当前页的 data 数组，因此无需查询总数。
        PageUtils.startPage(pageNum, pageSize, false).setReasonable(false).setPageSizeZero(false);
        try
        {
            return familyMemberElderMapper.selectMyFamilyDetails(familyMemberId);
        }
        finally
        {
            PageUtils.clearPage();
        }
    }

    /** 从现有小程序拦截器设置的上下文读取家属身份。 */
    private Long currentMemberId()
    {
        Long familyMemberId = UserThreadLocal.getUserId();
        if (familyMemberId == null)
        {
            throw new ServiceException("请先登录", HttpStatus.UNAUTHORIZED);
        }
        return familyMemberId;
    }
}
