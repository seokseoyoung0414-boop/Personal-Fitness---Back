package com.nody.nodyspring.util;

import com.nody.nodyspring.DB.User;
import com.nody.nodyspring.config.JwtConfig;
import com.nody.nodyspring.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private UserService userService;

    public User validateToken(String token) throws Exception {
        if (token == null || token.isEmpty()) {
            throw new Exception("토큰이 없습니다.");
        }

        String userid = jwtConfig.getUserIdFromToken(token);
        return userService.findByUserid(userid);
    }
}