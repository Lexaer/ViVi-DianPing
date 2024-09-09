package com.vivi.service;

import com.vivi.dto.Result;
import com.vivi.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;


public interface IFollowService extends IService<Follow> {

    Result follow(Long id, boolean isFollow);

    Result isFollow(Long followUserId);

    Result commonFollows(Long id);
}
