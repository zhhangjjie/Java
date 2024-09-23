package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import jdk.nashorn.internal.runtime.regexp.joni.Regex;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;



    @Override//发送验证码，保存验证码
    public Result sendCode(String phone, HttpSession session) {

        if (RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set("login:code:"+phone,code,2, TimeUnit.MINUTES);
//        log.debug("发送成功，验证码： {}", code);
        System.out.println("验证码："+code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        String code = loginForm.getCode();

        if (RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }
        String cacheCode = stringRedisTemplate.opsForValue().get("login:code:"+phone);

        if (cacheCode == null || !code.equals(cacheCode)){
            return Result.fail("验证码错误");
        }
        User user = query().eq("phone",phone).one();
        if (user == null){
            user = createUserByPhone(phone);
        }
        //以token为key，用户信息为value存入redis
        String token = UUID.randomUUID().toString(true);
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> stringObjectMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((
                        fieldName,fieldValue)->fieldValue.toString()));
        stringRedisTemplate.opsForHash().putAll("login:token:"+token,stringObjectMap);
        stringRedisTemplate.expire("login:token:"+token,30,TimeUnit.MINUTES);

        return Result.ok(token);
    }

    private User createUserByPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName("user_"+RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
