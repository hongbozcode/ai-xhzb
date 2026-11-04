package com.xhzb.nursing.component;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xhzb.common.core.redis.RedisCache;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.common.utils.StringUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 微信小程序服务端接口，统一处理超时、错误响应及调用凭据缓存。
 */
@Component
public class WeChatMiniProgramClient
{
    private static final String API_BASE = "https://api.weixin.qq.com";
    private static final int TIMEOUT_MILLIS = 10000;

    @Value("${wechat.mini-program.appid}")
    private String appid;

    @Value("${wechat.mini-program.secret}")
    private String secret;

    @Autowired
    private RedisCache redisCache;

    /**
     * 校验登录凭证，获取当前小程序内的用户标识。
     *
     * @param code wx.login 返回的临时凭证
     * @return 微信 OpenID
     */
    public String getOpenId(String code)
    {
        Map<String, Object> parameters = credentialParameters();
        parameters.put("js_code", code);
        parameters.put("grant_type", "authorization_code");
        JSONObject result = execute(HttpUtil.createGet(API_BASE + "/sns/jscode2session")
                .form(parameters), "微信登录凭证校验");
        checkResult(result, "微信登录凭证校验");
        return requiredString(result, "openid", "微信登录结果缺少用户标识");
    }

    /**
     * 消费手机号授权凭证；仅在调用凭据失效时刷新并重试一次。
     *
     * @param phoneCode 微信手机号授权凭证
     * @return 不含国家区号的手机号
     */
    public String getPhoneNumber(String phoneCode)
    {
        String accessToken = getAccessToken();
        JSONObject result = requestPhoneNumber(accessToken, phoneCode);
        int errorCode = result.getInt("errcode", 0);
        if (errorCode == 40001 || errorCode == 40014 || errorCode == 42001)
        {
            redisCache.deleteObject(accessTokenKey());
            result = requestPhoneNumber(getAccessToken(), phoneCode);
        }
        checkResult(result, "微信手机号获取");
        JSONObject phoneInfo = result.getJSONObject("phone_info");
        String phone = phoneInfo == null ? null : phoneInfo.getStr("purePhoneNumber");
        if (phone == null || !phone.matches("[0-9]{4,20}"))
        {
            throw new ServiceException("微信返回的手机号无效，请重新授权");
        }
        return phone;
    }

    /** 获取并缓存后台接口调用凭据，提前60秒过期以避免临界时间失效。 */
    private synchronized String getAccessToken()
    {
        String token = redisCache.getCacheObject(accessTokenKey());
        if (StringUtils.isNotBlank(token))
        {
            return token;
        }
        Map<String, Object> parameters = credentialParameters();
        parameters.put("grant_type", "client_credential");
        JSONObject result = execute(HttpUtil.createGet(API_BASE + "/cgi-bin/token")
                .form(parameters), "微信调用凭据获取");
        checkResult(result, "微信调用凭据获取");
        token = requiredString(result, "access_token", "微信调用凭据获取失败");
        Integer expiresIn = result.getInt("expires_in");
        if (expiresIn == null || expiresIn <= 0)
        {
            throw new ServiceException("微信调用凭据有效期异常");
        }
        redisCache.setCacheObject(accessTokenKey(), token, Math.max(1, expiresIn - 60), TimeUnit.SECONDS);
        return token;
    }

    /** 以 JSON 请求体换取手机号，登录 code 不可代替手机号 code。 */
    private JSONObject requestPhoneNumber(String accessToken, String phoneCode)
    {
        return execute(HttpUtil.createPost(API_BASE + "/wxa/business/getuserphonenumber?access_token="
                + cn.hutool.core.net.URLEncoder.DEFAULT.encode(accessToken, java.nio.charset.StandardCharsets.UTF_8))
                .body(JSONUtil.createObj().set("code", phoneCode).toString(), "application/json"),
                "微信手机号获取");
    }

    /** 构造仅供服务端使用的应用凭据。 */
    private Map<String, Object> credentialParameters()
    {
        if (StringUtils.isBlank(appid) || StringUtils.isBlank(secret))
        {
            throw new ServiceException("微信小程序登录尚未配置");
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("appid", appid);
        parameters.put("secret", secret);
        return parameters;
    }

    /** 按应用隔离缓存，避免不同小程序共用调用凭据。 */
    private String accessTokenKey()
    {
        return "wechat:mini-program:access-token:" + appid;
    }

    /** 不透传网络异常及原始响应，避免泄露包含 secret、code、token 的 URL。 */
    private JSONObject execute(HttpRequest request, String operation)
    {
        try (HttpResponse response = request.timeout(TIMEOUT_MILLIS).execute())
        {
            if (!response.isOk())
            {
                throw new ServiceException(operation + "失败，请稍后重试");
            }
            return JSONUtil.parseObj(response.body());
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException(operation + "异常，请稍后重试");
        }
    }

    /** 将微信业务错误转为项目统一异常，仅返回错误码。 */
    private void checkResult(JSONObject result, String operation)
    {
        int errorCode = result.getInt("errcode", 0);
        if (errorCode != 0)
        {
            throw new ServiceException(operation + "失败（错误码：" + errorCode + "），请重新授权后重试");
        }
    }

    /** 校验微信成功响应中的必需字段。 */
    private String requiredString(JSONObject result, String field, String message)
    {
        String value = result.getStr(field);
        if (StringUtils.isBlank(value))
        {
            throw new ServiceException(message);
        }
        return value;
    }
}
