package com.imocc.config;

import com.imocc.security.AuthProvider;
import com.imocc.security.LoginAuthFailHandler;
import com.imocc.security.LoginUrlEntryPotint;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import javax.annotation.Resource;

/**
 * Created by Administrator on 2018/1/6.
 */
@EnableWebSecurity
@EnableGlobalMethodSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter{

    /**
     * HTTP权限控制
     * @param http
     * @throws Exception
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/admin/login").permitAll()
                .antMatchers("/static/**").permitAll()
                .antMatchers("/user/login").permitAll()     //普通用户登录
                .antMatchers("/admin/**").hasRole("ADMIN")
                .antMatchers("/user/**").hasAnyRole("ADMIN","USER")
                .antMatchers("/api/user/**").hasAnyRole("ADMIN","USER") //用户访问接口页面
                .and()
                .formLogin()
                .loginProcessingUrl("/login")       //处理角色配置处理入口
                .failureHandler(authFailHandler())      //登录失败处理
                .and()
                .logout()
                .logoutSuccessUrl("/logout/page")
                .deleteCookies("JSESSIONID")        //清除cookies
                .invalidateHttpSession(true)
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(urlEntryPotint())
                .accessDeniedPage("/403")
                .and();

        http.csrf().disable();
        http.headers().frameOptions().sameOrigin(); //开启iframe
    }

    /**
     * 自定义认证策略
     * @param authenticationManagerBuilder
     */
    @Resource
    public void configGlobal(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
        /*authenticationManagerBuilder.inMemoryAuthentication().withUser("admin").password("admin")
                .roles("ADMIN").and();*/
        authenticationManagerBuilder.authenticationProvider(authProvider()).eraseCredentials(true); //擦除密码
    }

    /**
     * 自定义认证类
     * @return
     */
    @Bean
    public AuthProvider authProvider(){
        return new AuthProvider();
    }

    /**
     * 不同用户登录拦截
     * @return
     */
    @Bean
    public LoginUrlEntryPotint urlEntryPotint(){
        return new LoginUrlEntryPotint("/user/login");
    }

    /**
     * 登录失败拦截
     * @return
     */
    @Bean
    public LoginAuthFailHandler authFailHandler(){
        return new LoginAuthFailHandler(urlEntryPotint());
    }
}
