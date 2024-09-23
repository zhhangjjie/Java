package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 缓存商家信息，如果第一次访问就访问数据库，把信息放到redis中
     * 如果第二次之后访问，直接从redis中获取。
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        String key = CACHE_SHOP_KEY + id;
        String s = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(s)){
            Shop shop = JSONUtil.toBean(s, Shop.class);
            return Result.ok(shop);
        }
        if (s != null){//穿透
            return Result.fail("商铺不存在");
        }
        Shop shop = getById(id);
        if (shop == null){
            //穿透
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);

            return Result.fail("商铺不存在");
        }
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return Result.ok(shop);
    }
    //击穿
    private boolean tryLock(String key){
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(key, "1",10,TimeUnit.MINUTES);
        return BooleanUtil.isTrue(aBoolean);
    }
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }

}
