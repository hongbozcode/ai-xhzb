package com.xhzb.nursing.controller.member;

import com.xhzb.nursing.domain.vo.MemberLoginVo;
import com.xhzb.nursing.service.IFamilyMemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 验证固定请求路径、响应契约及必要的登录凭证校验。 */
class MemberUserControllerTest
{
    private MockMvc mvc;
    private IFamilyMemberService service;

    @BeforeEach
    void setUp()
    {
        service = mock(IFamilyMemberService.class);
        MemberUserController controller = new MemberUserController();
        ReflectionTestUtils.setField(controller, "familyMemberService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void returnsDocumentedLoginResponse() throws Exception
    {
        MemberLoginVo result = new MemberLoginVo();
        result.setToken("signed-token");
        result.setNickName("温暖相伴8915");
        when(service.login(any())).thenReturn(result);
        mvc.perform(post("/member/user/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"login-code\",\"nickName\":\"微信用户\",\"phoneCode\":\"phone-code\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"token\":\"signed-token\",\"nickName\":\"温暖相伴8915\"},\"operationTime\":null}", true));
    }

    @Test
    void rejectsMissingOrBlankCredentialsBeforeCallingService() throws Exception
    {
        for (String body : new String[]{"{}", "{\"code\":\" \",\"phoneCode\":\"phone-code\"}",
                "{\"code\":\"login-code\",\"phoneCode\":\"\"}"})
        {
            mvc.perform(post("/member/user/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
}
