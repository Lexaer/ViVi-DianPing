package com.vivi.service.impl;

import com.vivi.entity.UserInfo;
import com.vivi.mapper.UserInfoMapper;
import com.vivi.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
