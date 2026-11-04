package com.xhzb.nursing.service.impl;

import java.util.List;
import com.xhzb.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.FamilyMemberMapper;
import com.xhzb.nursing.domain.FamilyMember;
import com.xhzb.nursing.service.IFamilyMemberService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.framework.web.service.TokenService;
import com.xhzb.nursing.component.WeChatMiniProgramClient;
import com.xhzb.nursing.domain.dto.MemberLoginDto;
import com.xhzb.nursing.domain.vo.MemberLoginVo;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

/**
 * 老人家属Service业务层处理
 * 
 * @author hongbozcode
 * @date 2026-11-04
 */
@Service
public class FamilyMemberServiceImpl extends ServiceImpl<FamilyMemberMapper, FamilyMember> implements IFamilyMemberService
{
    @Autowired
    private FamilyMemberMapper familyMemberMapper;

    @Autowired
    private WeChatMiniProgramClient weChatMiniProgramClient;

    @Autowired
    private TokenService tokenService;

    @Value("${token.expireTime}")
    private int memberTokenExpireMinutes;

    /** 适合养老关怀场景的四字昵称前缀。 */
    private static final List<String> NICKNAME_PREFIXES = Arrays.asList(
            "阖家安康", "温暖相伴", "幸福守望", "岁岁平安", "福寿安宁", "亲情常在", "和乐融融", "暖心家人");

    /**
     * 微信登录：先校验身份及手机号，再注册或更新家属并签发令牌。
     *
     * @param loginDto 微信登录参数
     * @return 登录令牌与持久化昵称
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberLoginVo login(MemberLoginDto loginDto)
    {
        String openId = weChatMiniProgramClient.getOpenId(loginDto.getCode());
        FamilyMember member = getOne(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getOpenId, openId));
        String phone = weChatMiniProgramClient.getPhoneNumber(loginDto.getPhoneCode());
        if (member == null)
        {
            member = new FamilyMember();
            member.setOpenId(openId);
            member.setPhone(phone);
            String prefix = NICKNAME_PREFIXES.get(ThreadLocalRandom.current().nextInt(NICKNAME_PREFIXES.size()));
            member.setName(prefix + phone.substring(phone.length() - 4));
            member.setCreateTime(DateUtils.getNowDate());
            if (!save(member))
            {
                throw new ServiceException("家属注册失败，请重试");
            }
        }
        else if (!phone.equals(member.getPhone()))
        {
            // 只更新手机号，避免覆盖其他请求修改的头像、昵称等信息。
            FamilyMember update = new FamilyMember();
            update.setId(member.getId());
            update.setPhone(phone);
            update.setUpdateTime(DateUtils.getNowDate());
            if (!updateById(update))
            {
                throw new ServiceException("家属手机号更新失败，请重试");
            }
            member.setPhone(phone);
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", member.getId());
        claims.put("nickName", member.getName());
        // Map 令牌与管理端 Redis 会话分离，并设置与项目配置一致的有效期。
        claims.put("exp", new Date(System.currentTimeMillis() + memberTokenExpireMinutes * 60000L));
        MemberLoginVo result = new MemberLoginVo();
        result.setToken(tokenService.createToken(claims));
        result.setNickName(member.getName());
        return result;
    }


    /**
     * 查询老人家属
     * 
     * @param id 老人家属主键
     * @return 老人家属
     */
    @Override
    public FamilyMember selectFamilyMemberById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询老人家属列表
     * 
     * @param familyMember 老人家属
     * @return 老人家属
     */
    @Override
    public List<FamilyMember> selectFamilyMemberList(FamilyMember familyMember)
    {
        return familyMemberMapper.selectFamilyMemberList(familyMember);
    }

    /**
     * 新增老人家属
     * 
     * @param familyMember 老人家属
     * @return 结果
     */
    @Override
    public int insertFamilyMember(FamilyMember familyMember)
    {
        return save(familyMember)? 1 : 0;
    }

    /**
     * 修改老人家属
     * 
     * @param familyMember 老人家属
     * @return 结果
     */
    @Override
    public int updateFamilyMember(FamilyMember familyMember)
    {
        return updateById(familyMember)? 1 : 0;
    }

    /**
     * 批量删除老人家属
     * 
     * @param ids 需要删除的老人家属主键
     * @return 结果
     */
    @Override
    public int deleteFamilyMemberByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids))? 1 : 0;
    }

    /**
     * 删除老人家属信息
     * 
     * @param id 老人家属主键
     * @return 结果
     */
    @Override
    public int deleteFamilyMemberById(Long id)
    {
        return removeById(id)? 1 : 0;
    }
}
