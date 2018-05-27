package com.imocc.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于角色登录控制器
 * Created by Administrator on 2018/1/6.
 */
public class LoginUrlEntryPotint extends LoginUrlAuthenticationEntryPoint{

    private PathMatcher pathMatcher=new AntPathMatcher();
    private final Map<String,String> authEntryPotintMap;

    public LoginUrlEntryPotint(String loginFormUrl) {
        super(loginFormUrl);
        authEntryPotintMap=new HashMap<>();
        //普通用户登录映射
        authEntryPotintMap.put("/user/**","/user/login");
        //管理员登录映射
        authEntryPotintMap.put("/admin/**","/admin/login");
    }

    /**
     * 根据请求跳转指定页面 父类默认 调用loginFormUrl
     * @param request
     * @param response
     * @param exception
     * @return
     */
    @Override
    protected String determineUrlToUseForThisRequest(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        String uri = request.getRequestURI().replace(request.getContextPath(), "");
        for (Map.Entry<String, String> entry : authEntryPotintMap.entrySet()) {
            if(this.pathMatcher.match(entry.getKey(),uri)){
                return entry.getValue();
            }
        }

        return super.determineUrlToUseForThisRequest(request, response, exception);
    }
}
