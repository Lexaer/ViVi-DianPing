package com.vivi.interceptor;

import com.vivi.dto.UserDTO;
import com.vivi.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor  implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //verify ThreadLocal has the user

        UserDTO user = UserHolder.getUser();
        if(user == null) {
            response.setStatus(401);
            return false;
        }
        return true;
    }


}
