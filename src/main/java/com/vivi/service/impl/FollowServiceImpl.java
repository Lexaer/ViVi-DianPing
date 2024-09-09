package com.vivi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.vivi.dto.Result;
import com.vivi.dto.UserDTO;
import com.vivi.entity.Follow;
import com.vivi.entity.User;
import com.vivi.mapper.FollowMapper;
import com.vivi.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vivi.service.IUserService;
import com.vivi.utils.RedisConstants;
import com.vivi.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.sql.Wrapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private IUserService userService;
    /**
     * 关注或者取关 redis中也缓存一份
     * @param followUserId
     * @param isFollow
     * @return
     */
    @Override
    public Result follow(Long followUserId, boolean isFollow) {
        if(BooleanUtil.isTrue(isFollow)){
            Follow follow = new Follow();
            follow.setUserId(UserHolder.getUser().getId());
            follow.setFollowUserId(followUserId);
            boolean save = this.save(follow);
            if(save){
                stringRedisTemplate.opsForSet().add(RedisConstants.FOLLOWS + UserHolder.getUser().getId().toString()
                        ,followUserId.toString());
            }
        }else{
            boolean remove = this.remove(new LambdaQueryWrapper<Follow>()
                    .eq(Follow::getUserId, UserHolder.getUser().getId())
                    .eq(Follow::getFollowUserId, followUserId)
            );
            if(remove){
                stringRedisTemplate.opsForSet().remove(RedisConstants.FOLLOWS + UserHolder.getUser().getId().toString()
                        ,followUserId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 查询是否关注
     * @param followUserId
     * @return
     */
    @Override
    public Result isFollow(Long followUserId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowUserId,followUserId)
                .eq(Follow::getUserId,UserHolder.getUser().getId());
        int count = this.baseMapper.selectCount(wrapper);
        return Result.ok(count > 0);
    }

    @Override
    public Result commonFollows(Long id) {
        Long userId = UserHolder.getUser().getId();
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(RedisConstants.FOLLOWS + userId.toString()
                ,RedisConstants.FOLLOWS + id.toString());
        //无共同关注
        if(intersect == null  || intersect.isEmpty()){
            return Result.ok();
        }
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        List<User> users = userService.listByIds(ids);
        List<UserDTO> userDTOS = users.stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(userDTOS.isEmpty()?Collections.emptyList():userDTOS);


    }
}
