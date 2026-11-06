package com.xhzb.framework.interceptor;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.xhzb.common.constant.HttpStatus;
import com.xhzb.common.utils.UserThreadLocal;
import com.xhzb.framework.web.service.TokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
@Component
public class MemberInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenService tokenService;

    /**
     * 小程序端Token验证核心处理逻辑(拦截方法)
     * @param request  current HTTP request
     * @param response current HTTP response
     * @param handler  chosen handler to execute, for type and/or instance evaluation
     * @return {@code true} if the execution chain should proceed with the
     * next interceptor or the handler itself. Else, DispatcherServlet assumes
     * that this interceptor has already dealt with the response itself.
     * @throws Exception in case of errors
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1，从请求头获取令牌
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);

        //2, 如果token为空，响应401
        if(StrUtil.isBlank(token)){
            response.setStatus(HttpStatus.UNAUTHORIZED);
            return false;

        }

        //3，解析Token
        try {
            Claims claims = tokenService.parseToken(token);
            //4，解析为空返回401
            Long userId = MapUtil.get(claims, "userId", Long.class);
            if(userId==null){
                response.setStatus(HttpStatus.UNAUTHORIZED);
                return false;
            }
            //5，解析成功将userId存入Threadlocal中
            UserThreadLocal.set(userId);

        } catch (Exception e) {
            //6，Token解析失败，响应401
            response.setStatus(HttpStatus.UNAUTHORIZED);
            return false;
        }
        //7，放行
        return  true;
    }

    /**
     * 释放ThreadLocal方法
     * @param request  current HTTP request
     * @param response current HTTP response
     * @param handler  the handler (or {@link HandlerMethod}) that started asynchronous
     *                 execution, for type and/or instance examination
     * @param ex       any exception thrown on handler execution, if any; this does not
     *                 include exceptions that have been handled through an exception resolver
     * @throws Exception in case of errors
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        UserThreadLocal.remove();
    }
}
