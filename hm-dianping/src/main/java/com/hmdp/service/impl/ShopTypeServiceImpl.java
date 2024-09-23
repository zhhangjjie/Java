package com.hmdp.service.impl;

import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryList() {
        String key = RedisConstants.CACHE_SHOP_TYPE_KEY+"list";
        String s = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(s)){
            ShopType shopType = JSONUtil.toBean(s, ShopType.class);
            return Result.ok(shopType);
        }
        List<ShopType> typeList = query().orderByAsc("sort").list();
        if (typeList == null){
            Result.fail("类型不存在");
        }
        String s1 = typeList.toString();
        System.out.println(s1);
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(s1));
        return Result.ok(s1);
    }
}
