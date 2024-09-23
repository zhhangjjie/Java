package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result queryBlogById(Long id) {
        Blog blog = getById(id);
        if (blog == null){
            return Result.fail("blog不存在");
        }
        Long userId = blog.getUserId();

        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    /**
     * 给blog的isLiked属性赋值
     * @param blog
     */
    private void isBlogLiked(Blog blog){
        Long id = UserHolder.getUser().getId();
        String key = RedisConstants.BLOG_LIKED_KEY+blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, id.toString());
        blog.setIsLike(score != null);
    }

    /**
     * 给日记点赞
     * @param id
     * @return
     */
    @Override
    public Result likeBlog(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null){
            boolean id1 = update().setSql("liked = liked + 1").eq("id", id).update();
            if (id1){
                stringRedisTemplate.opsForZSet().add(key,userId.toString(),System.currentTimeMillis());
            }
        }else {
            boolean id1 = update().setSql("liked = liked - 1").eq("id", id).update();
            if (id1){
                stringRedisTemplate.opsForZSet().remove(key,userId.toString());
            }
        }


        return Result.ok();
    }
}
