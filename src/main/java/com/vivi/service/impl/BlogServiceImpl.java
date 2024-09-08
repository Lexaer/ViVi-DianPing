package com.vivi.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vivi.dto.Result;
import com.vivi.entity.Blog;
import com.vivi.entity.User;
import com.vivi.mapper.BlogMapper;
import com.vivi.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vivi.service.IUserService;
import com.vivi.utils.SystemConstants;
import com.vivi.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Autowired
    private IUserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result queryHotBlog(Integer current) {

        // 根据用户查询
        Page<Blog> page = this.query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog ->{
            this.queryBlogUser(blog);
            this.isBlogLiked(blog);
        });
        return Result.ok(records);
    }



    @Override
    public Result queryBlogById(Long id){
        Blog blog = getById(id);
        if(blog == null){
            return Result.fail("博客不存在");
        }
        queryBlogUser(blog);
        //查询该博客是否被点赞了
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    private void isBlogLiked(Blog blog) {
        Long userId = UserHolder.getUser().getId();
        String blogKey = "blog:liked" + blog.getId();
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(blogKey, userId.toString());
        blog.setIsLike(BooleanUtil.isTrue(isMember));

    }

    private void queryBlogUser(Blog blog){
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    @Override
    public Result likeBlog(Long id) {
        Long userId = UserHolder.getUser().getId();
        String blogKey = "blog:liked" + id;
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(blogKey, userId.toString());
        if(BooleanUtil.isFalse(isMember)){
            boolean success = update().setSql("liked = liked + 1").eq("id", id).update();
            if(success){
                stringRedisTemplate.opsForSet().add(blogKey,userId.toString());
            }
        }else{
            //取消点赞
            boolean success = update().setSql("liked = liked - 1").eq("id", id).update();
            if(success){
                stringRedisTemplate.opsForSet().remove(blogKey,userId.toString());
            }
        }
        return Result.ok();
    }

}
