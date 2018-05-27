package com.imocc.web.controller;

import com.imocc.base.ApiResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by Administrator on 2018/1/5.
 */
@Controller
public class HomeController {
    /**
     * 跳转到用户主页
     * @param model
     * @return
     */
    @GetMapping({"/","index"})
    public String index(Model model){
        model.addAttribute("lala","啦啦啦");
        return "index";
    }

    @GetMapping("/api")
    @ResponseBody
    public ApiResponse api(){
        return ApiResponse.ofMessage(ApiResponse.Status.SUCCESS.getCode(),ApiResponse.Status.SUCCESS.getStandardMessage());
    }
    @GetMapping("/logout/page")
    public String logoutPage(){ return "logout"; }
}
