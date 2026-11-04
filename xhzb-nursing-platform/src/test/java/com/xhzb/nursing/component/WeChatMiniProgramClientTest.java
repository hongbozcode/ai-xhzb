package com.xhzb.nursing.component;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.xhzb.common.core.redis.RedisCache;
import com.xhzb.common.exception.ServiceException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 使用模拟响应验证微信协议和错误处理，不请求真实微信服务。 */
class WeChatMiniProgramClientTest
{
    private WeChatMiniProgramClient client;
    private RedisCache redis;

    @BeforeEach
    void setUp()
    {
        client = new WeChatMiniProgramClient();
        redis = mock(RedisCache.class);
        ReflectionTestUtils.setField(client, "redisCache", redis);
        ReflectionTestUtils.setField(client, "appid", "test-appid");
        ReflectionTestUtils.setField(client, "secret", "test-secret");
    }

    @Test
    void exchangesLoginCodeUsingOfficialParameters()
    {
        HttpRequest request = request("{\"openid\":\"test-openid\"}");
        try (MockedStatic<HttpUtil> http = mockStatic(HttpUtil.class))
        {
            http.when(() -> HttpUtil.createGet("https://api.weixin.qq.com/sns/jscode2session")).thenReturn(request);
            assertEquals("test-openid", client.getOpenId("login-code"));
            verify(request).form(argThat((java.util.Map<String, Object> values) ->
                    "login-code".equals(values.get("js_code"))
                    && "authorization_code".equals(values.get("grant_type"))
                    && "test-appid".equals(values.get("appid"))
                    && "test-secret".equals(values.get("secret"))));
            verify(request).timeout(10000);
        }
    }

    @Test
    void fetchesCachesAndReusesAccessToken()
    {
        HttpRequest token = request("{\"access_token\":\"access-token\",\"expires_in\":7200}");
        HttpRequest phone = request("{\"errcode\":0,\"phone_info\":{\"purePhoneNumber\":\"13800138915\"}}");
        when(redis.getCacheObject(anyString())).thenReturn(null, "access-token");
        try (MockedStatic<HttpUtil> http = mockStatic(HttpUtil.class))
        {
            http.when(() -> HttpUtil.createGet("https://api.weixin.qq.com/cgi-bin/token")).thenReturn(token);
            http.when(() -> HttpUtil.createPost("https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=access-token")).thenReturn(phone);
            assertEquals("13800138915", client.getPhoneNumber("phone-code"));
            assertEquals("13800138915", client.getPhoneNumber("next-phone-code"));
            verify(redis).setCacheObject("wechat:mini-program:access-token:test-appid", "access-token", 7140, TimeUnit.SECONDS);
            verify(token, times(1)).execute();
            verify(phone).body("{\"code\":\"phone-code\"}", "application/json");
        }
    }

    @Test
    void rejectsInvalidCodeAndMissingOpenId()
    {
        HttpRequest request = request("{\"errcode\":40029,\"errmsg\":\"invalid code\"}", "{}");
        try (MockedStatic<HttpUtil> http = mockStatic(HttpUtil.class))
        {
            http.when(() -> HttpUtil.createGet(anyString())).thenReturn(request);
            assertTrue(assertThrows(ServiceException.class, () -> client.getOpenId("bad")).getMessage().contains("40029"));
            assertThrows(ServiceException.class, () -> client.getOpenId("missing"));
        }
    }

    @Test
    void refreshesExpiredAccessTokenOnce()
    {
        when(redis.getCacheObject(anyString())).thenReturn("old-token", (Object) null);
        HttpRequest token = request("{\"access_token\":\"new-token\",\"expires_in\":7200}");
        HttpRequest expired = request("{\"errcode\":42001}");
        HttpRequest phone = request("{\"errcode\":0,\"phone_info\":{\"purePhoneNumber\":\"13800138915\"}}");
        try (MockedStatic<HttpUtil> http = mockStatic(HttpUtil.class))
        {
            http.when(() -> HttpUtil.createGet(anyString())).thenReturn(token);
            http.when(() -> HttpUtil.createPost(contains("old-token"))).thenReturn(expired);
            http.when(() -> HttpUtil.createPost(contains("new-token"))).thenReturn(phone);
            assertEquals("13800138915", client.getPhoneNumber("phone-code"));
            verify(redis).deleteObject("wechat:mini-program:access-token:test-appid");
            verify(phone, times(1)).execute();
        }
    }

    @Test
    void masksTransportErrors()
    {
        HttpRequest request = mock(HttpRequest.class, RETURNS_SELF);
        when(request.execute()).thenThrow(new IllegalStateException("url?secret=test-secret"));
        try (MockedStatic<HttpUtil> http = mockStatic(HttpUtil.class))
        {
            http.when(() -> HttpUtil.createGet(anyString())).thenReturn(request);
            ServiceException error = assertThrows(ServiceException.class, () -> client.getOpenId("code"));
            assertFalse(error.getMessage().contains("test-secret"));
        }
    }

    private HttpRequest request(String... bodies)
    {
        HttpRequest request = mock(HttpRequest.class, RETURNS_SELF);
        HttpResponse response = mock(HttpResponse.class);
        when(request.execute()).thenReturn(response);
        when(response.isOk()).thenReturn(true);
        when(response.body()).thenReturn(bodies[0], java.util.Arrays.copyOfRange(bodies, 1, bodies.length));
        return request;
    }
}
