package com.imocc.security;

import com.imocc.entity.User;
import com.imocc.service.IUserService;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import javax.annotation.Resource;

/**
 * Created by Administrator on 2018/1/6.
 * 自定义认证实现
 */
public class AuthProvider implements AuthenticationProvider{

    @Resource
    private IUserService iUserService;

    private final Md5PasswordEncoder passwordEncoder=new Md5PasswordEncoder();
    /**
     * 认证逻辑
     * @param authentication
     * @return
     * @throws AuthenticationException
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String userName = authentication.getName();
        String inputPassword = (String) authentication.getCredentials();
        User user = iUserService.findUserByName(userName);
        if(user==null){
            throw new AuthenticationCredentialsNotFoundException("authError");
        }
        if (passwordEncoder.isPasswordValid(user.getPassword(),inputPassword,user.getId())){
            return new UsernamePasswordAuthenticationToken(user,user.getPassword(),user.getAuthoritieList());
        }
        throw new BadCredentialsException("authError");
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return true;
    }
}
