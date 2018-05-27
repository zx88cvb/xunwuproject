package com.imocc.web.controller.user;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Created by Administrator on 2018/1/6.
 */
@Controller
public class UserController {
    /**
     * 普通用户登录页面
     * @return
     */
    @GetMapping("/user/login")
    public String loginPage(){
        return "user/login";
    }

    /**
     * 普通用户主页
     * @return
     */
    @GetMapping("/user/center")
    public String centerPage(){
        return "user/center";
    }
}
