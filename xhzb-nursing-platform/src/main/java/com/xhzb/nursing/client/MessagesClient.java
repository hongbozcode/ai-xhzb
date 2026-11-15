package com.xhzb.nursing.client;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.http.Header;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xhzb.common.exception.ServiceException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/** 通过 SMS8 向绑定的发送设备提交短信。 */
@Component
public class MessagesClient {
    private static final int TIMEOUT_MILLIS = 10_000;

    private final String endpoint;
    private final String apiKey;
    private final boolean enabled;

    public MessagesClient(
            @Value("${xhzb.sms8.endpoint:https://app.sms8.io/api/v1/messages}") String endpoint,
            @Value("${xhzb.sms8.api-key:${SMS8_API_KEY:8dc8149cd37deafdfac003d0b97c83fa5d0b759c}}") String apiKey,
            @Value("${xhzb.sms8.enabled:true}") boolean enabled) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.enabled = enabled;
    }

    /** 返回当前环境是否开启短信发送。 */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 批量提交已渲染的短信，提交成功后由 SMS8 设备负责发送。
     *
     * @param messagesByNumber 国际格式手机号及对应的短信内容
     */
    public void sendMessage(Map<String, String> messagesByNumber) {
        if (!enabled || messagesByNumber.isEmpty()) {
            return;
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new ServiceException("SMS8 未配置 API Key");
        }

        JSONArray messages = JSONUtil.createArray();
        messagesByNumber.forEach((number, message) -> messages.add(JSONUtil.createObj()
                .set("number", number)
                .set("message", message)
                .set("type", "sms")));
        Map<String, Object> form = Map.of("key", apiKey, "messages", messages.toString(), "option", "0");
        try (HttpResponse response = HttpRequest.post(endpoint)
                .header(Header.ACCEPT, "application/json")
                .form(form)
                .timeout(TIMEOUT_MILLIS)
                .execute()) {
            if (!response.isOk()) {
                throw new ServiceException("SMS8 请求失败，HTTP 状态码：" + response.getStatus());
            }
            String body = response.body();
            if (!StringUtils.hasText(body)) {
                throw new ServiceException("SMS8 返回空响应，HTTP 状态码：" + response.getStatus());
            }
            if (!JSONUtil.isTypeJSONObject(body)) {
                throw new ServiceException("SMS8 响应不是 JSON 对象，HTTP 状态码：" + response.getStatus());
            }
            JSONObject result;
            try {
                result = JSONUtil.parseObj(body);
            } catch (JSONException exception) {
                throw new ServiceException("SMS8 响应 JSON 解析失败，HTTP 状态码：" + response.getStatus());
            }
            if (!(result.get("success") instanceof Boolean success)) {
                throw new ServiceException("SMS8 响应解析失败：缺少有效的 success 字段");
            }
            if (!success) {
                Object error = result.get("error");
                Integer errorCode = error instanceof JSONObject details ? details.getInt("code", null) : null;
                throw new ServiceException("SMS8 拒绝提交短信", errorCode);
            }
        } catch (HttpException | IORuntimeException exception) {
            // 短信正文、手机号、密钥和原始响应均不进入应用日志。
            Throwable rootCause = ExceptionUtils.getRootCause(exception);
            String failureType = (rootCause == null ? exception : rootCause).getClass().getSimpleName();
            throw new ServiceException("SMS8 HTTP 请求或读取响应失败，异常类型：" + failureType);
        }
    }
}
