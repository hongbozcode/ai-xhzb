package com.xhzb.nursing.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.framework.web.service.TokenService;
import com.xhzb.nursing.component.WeChatMiniProgramClient;
import com.xhzb.nursing.domain.FamilyMember;
import com.xhzb.nursing.domain.dto.MemberLoginDto;
import com.xhzb.nursing.domain.vo.MemberLoginVo;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 家属登录的注册、手机号更新及失败分支验证。 */
@ExtendWith(MockitoExtension.class)
class FamilyMemberLoginTest
{
    @Mock
    private WeChatMiniProgramClient client;
    @Mock
    private TokenService tokenService;
    @Spy
    @InjectMocks
    private FamilyMemberServiceImpl service;
    private MemberLoginDto request;

    @BeforeEach
    void setUp()
    {
        request = new MemberLoginDto();
        request.setCode("login-code");
        request.setPhoneCode("phone-code");
        request.setNickName("不能覆盖随机昵称");
        ReflectionTestUtils.setField(service, "memberTokenExpireMinutes", 30);
        when(client.getOpenId("login-code")).thenReturn("openid");
    }

    @Test
    void registersWithPhoneSuffixAndSignsPersistedIdentity()
    {
        doReturn(null).when(service).getOne(any(Wrapper.class));
        when(client.getPhoneNumber("phone-code")).thenReturn("13800138915");
        doAnswer(call -> {
            FamilyMember member = call.getArgument(0);
            member.setId(42L);
            return true;
        }).when(service).save(any(FamilyMember.class));
        when(tokenService.createToken(anyMap())).thenReturn("signed-token");
        MemberLoginVo result = service.login(request);
        assertEquals("signed-token", result.getToken());
        assertTrue(result.getNickName().matches("[\\p{IsHan}]{4}8915"));
        ArgumentCaptor<FamilyMember> memberCaptor = ArgumentCaptor.forClass(FamilyMember.class);
        verify(service).save(memberCaptor.capture());
        assertEquals("openid", memberCaptor.getValue().getOpenId());
        assertEquals("13800138915", memberCaptor.getValue().getPhone());
        ArgumentCaptor<Map<String, Object>> claims = ArgumentCaptor.forClass(Map.class);
        verify(tokenService).createToken(claims.capture());
        assertEquals(42L, claims.getValue().get("userId"));
        assertEquals(result.getNickName(), claims.getValue().get("nickName"));
        assertTrue(((java.util.Date) claims.getValue().get("exp")).after(new java.util.Date()));
    }

    @Test
    void updatesChangedPhoneAndPreservesNickname()
    {
        FamilyMember member = existingMember();
        doReturn(member).when(service).getOne(any(Wrapper.class));
        when(client.getPhoneNumber("phone-code")).thenReturn("13900130000");
        doReturn(true).when(service).updateById(any(FamilyMember.class));
        MemberLoginVo result = service.login(request);
        assertEquals("温暖相伴8915", result.getNickName());
        ArgumentCaptor<FamilyMember> update = ArgumentCaptor.forClass(FamilyMember.class);
        verify(service).updateById(update.capture());
        assertEquals(42L, update.getValue().getId());
        assertEquals("13900130000", update.getValue().getPhone());
        assertNull(update.getValue().getName());
        verify(service, never()).save(any(FamilyMember.class));
    }

    @Test
    void samePhoneDoesNotWrite()
    {
        doReturn(existingMember()).when(service).getOne(any(Wrapper.class));
        when(client.getPhoneNumber("phone-code")).thenReturn("13800138915");
        service.login(request);
        verify(service, never()).save(any(FamilyMember.class));
        verify(service, never()).updateById(any(FamilyMember.class));
    }

    @Test
    void phoneAuthorizationFailureDoesNotCreateMemberOrToken()
    {
        doReturn(null).when(service).getOne(any(Wrapper.class));
        when(client.getPhoneNumber("phone-code")).thenThrow(new ServiceException("授权失败"));
        assertThrows(ServiceException.class, () -> service.login(request));
        verify(service, never()).save(any(FamilyMember.class));
        verifyNoInteractions(tokenService);
    }

    @Test
    void failedInsertDoesNotIssueToken()
    {
        doReturn(null).when(service).getOne(any(Wrapper.class));
        when(client.getPhoneNumber("phone-code")).thenReturn("13800138915");
        doReturn(false).when(service).save(any(FamilyMember.class));
        assertThrows(ServiceException.class, () -> service.login(request));
        verifyNoInteractions(tokenService);
    }

    private FamilyMember existingMember()
    {
        FamilyMember member = new FamilyMember();
        member.setId(42L);
        member.setPhone("13800138915");
        member.setName("温暖相伴8915");
        return member;
    }
}
