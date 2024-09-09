package com.vivi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vivi.dto.LoginFormDTO;
import com.vivi.dto.Result;
import com.vivi.entity.User;

import javax.servlet.http.HttpSession;


public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result queryUserById(Long id);
}
