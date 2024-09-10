package com.vivi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vivi.dto.Result;
import com.vivi.dto.ScrollResult;
import com.vivi.dto.UserDTO;
import com.vivi.entity.Blog;
import com.vivi.entity.Follow;
import com.vivi.entity.User;
import com.vivi.mapper.BlogMapper;
import com.vivi.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vivi.service.IFollowService;
import com.vivi.service.IUserService;
import com.vivi.utils.RedisConstants;
import com.vivi.utils.SystemConstants;
import com.vivi.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Autowired
    private IUserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private IFollowService followService;


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
        if(userId == null){
            //未登录
            return ;
        }
        String blogKey = RedisConstants.BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(blogKey, userId.toString());
        blog.setIsLike(score != null);

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
        String blogKey = RedisConstants.BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(blogKey, userId.toString());
        if(score == null){
            boolean success = update().setSql("liked = liked + 1").eq("id", id).update();
            if(success){
                stringRedisTemplate.opsForZSet().add(blogKey,userId.toString(),System.currentTimeMillis());
            }
        }else{
            //取消点赞
            boolean success = update().setSql("liked = liked - 1").eq("id", id).update();
            if(success){
                stringRedisTemplate.opsForZSet().remove(blogKey,userId.toString());
            }

        }
        return Result.ok();
    }

    /**
     * 查询点赞博客的前几名
     * @param id
     * @return
     */
    @Override
    public Result queryBlogLikes(Long id) {
        String blogKey = RedisConstants.BLOG_LIKED_KEY + id;
        //查询top5 的点赞用户 zrange key 0 4
        Set<String> range = stringRedisTemplate.opsForZSet().range(blogKey, 0, 4);
        if(range == null || range.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        //解析用户id
        List<Long> ids = range.stream().map(Long::valueOf).collect(Collectors.toList());
        //返回用户
        String Ids = StrUtil.join(",", ids);
        List<UserDTO> userDTOS = userService.query()
                .in("id",ids)
                .last("ORDER BY FIELD(id," + Ids + ")")
                .list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        return Result.ok(userDTOS);
    }

    @Override
    public Result queryBlogByUserId(Integer current, Long id) {
        Page<Blog> page = this.lambdaQuery()
                .eq(Blog::getUserId, id)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        return Result.ok(page.getRecords());

    }

    @Override
    public Result saveBlog(Blog blog) {
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        boolean success = this.save(blog);
        if(success){
            List<Follow> follows = followService.lambdaQuery()
                    .eq(Follow::getFollowUserId, user.getId())
                    .list();
            //推送博客id给所有粉丝
            for (Follow follow : follows) {
                Long userId = follow.getUserId();
                //推送
                stringRedisTemplate.opsForZSet().add(RedisConstants.FEED_KEY + userId,blog.getId().toString(),System.currentTimeMillis());
            }
        }
        //返回id
        return Result.ok(blog.getId());
    }

    /**
     * 用户拉取博客
     * @param max
     * @param offset
     * @return
     */
    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        //获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key = RedisConstants.FEED_KEY + userId;
        //查询收件箱 ZRANGEBYSCORE key max min LIMIT offset count
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        //解析数据,blodId,时间戳,offset
        if(typedTuples == null || typedTuples.isEmpty()){
            return Result.ok();
        }
        List<Long> ids = new ArrayList<>(typedTuples.size());
        Long minTime = 0L;
        int os = 1;
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            ids.add(Long.valueOf(typedTuple.getValue()));
            long time = typedTuple.getScore().longValue();
            if(time == minTime){
                os++;
            }else{
                minTime = time;
                os = 1;
            }
        }
        //根据id查询blog
        String idsStr = StrUtil.join(",", ids);
        List<Blog> blogs = this.query().in("id", idsStr)
                .last("ORDER BY FIELD(id," + idsStr + ")").list();
        for (Blog blog : blogs) {
            //查询blog有关的用户
            queryBlogUser(blog);
            //查询blog是否被点赞
            isBlogLiked(blog);
        }
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setOffset(os);
        scrollResult.setMinTime(minTime);
        return Result.ok(scrollResult);
    }

}
