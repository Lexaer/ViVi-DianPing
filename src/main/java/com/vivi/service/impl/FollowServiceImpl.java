package com.vivi.service.impl;

import com.vivi.entity.Follow;
import com.vivi.mapper.FollowMapper;
import com.vivi.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;


@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

}
