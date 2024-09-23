package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *拦截器2
 * 验证登陆状态
 * 在访问需要用户登录之后才能访问的页面时，拦截并查看用户登陆状态是否为已登录。
 * 在ThreadLocal中获取用户，看是否存在
 * 登陆了放行，未登录拦截。
 */
public class LoginInterceptor implements HandlerInterceptor {


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (UserHolder.getUser()==null){
            response.setStatus(401);
            return false;
        }
        return true;

    }

}
