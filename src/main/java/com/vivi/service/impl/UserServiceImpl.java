package com.vivi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vivi.dto.LoginFormDTO;
import com.vivi.dto.Result;
import com.vivi.dto.UserDTO;
import com.vivi.entity.User;
import com.vivi.mapper.UserMapper;
import com.vivi.service.IUserService;
import com.vivi.utils.RedisConstants;
import com.vivi.utils.RegexUtils;
import com.vivi.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {


    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 发送验证码
     * @param phone
     * @param session
     * @return
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // verify the phone number
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }
        // generate the verification code
        String code = RandomUtil.randomNumbers(6);

        //save the code to redis
        stringRedisTemplate.opsForValue()
                .set(RedisConstants.LOGIN_CODE_KEY + phone,code,RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //TODO 发送短信验证码，这个要接入短信运营商，模拟已经做了
        log.debug("the code is {}",code);
        return Result.ok();
    }


    /**
     * 用户登录
     * @param loginForm
     * @param session
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //verify the code
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式有误");
        }
        String code = loginForm.getCode();
        String correctCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
        if(correctCode == null || !correctCode.equals(code)){
            return Result.fail("验证码错误");
        }

        //2.search the user by phone

        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<User>();
        userWrapper.eq(User::getPhone,phone);

        User user = baseMapper.selectOne(userWrapper);

        //2.1 if not exist ,create a new user and save it to the db
        if(user == null){
            user = createUserWithPhone(phone);
        }

        //generate token
        String token = UUID.randomUUID().toString(true);
        //save user to redis
        //转换所有map的字段类型为String,然后User -> Hash
        UserDTO userDTO = new UserDTO();
        BeanUtil.copyProperties(user,userDTO);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName,fieldValue) -> fieldValue.toString()));
        stringRedisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER_KEY + token,userMap);
        //set expiration time
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token,RedisConstants.LOGIN_USER_TTL,TimeUnit.MINUTES);
        return Result.ok(token);
    }

    @Override
    public Result queryUserById(Long id) {
        User user = this.getById(id);
        if(user == null){
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        return Result.ok(userDTO);

    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        this.save(user);
        return user;
    }
}
