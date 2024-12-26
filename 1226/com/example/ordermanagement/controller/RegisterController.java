package com.example.ordermanagement.controller;



import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


import com.example.ordermanagement.model.RegisterBean;

import com.example.ordermanagement.service.RegisterService;

@Controller
public class RegisterController {
	
	@Autowired
	private RegisterService registerService;
	
	// 顯示註冊頁面
    @GetMapping("/register")
    public String showRegisterPage() {
        return "loginOut/register";  // 返回register.html頁面
    }
	
 // 處理註冊表單
    @PostMapping("/register")
    public String register(@RequestBody RegisterBean user, Model model) {
        // 呼叫Service層的registerUser方法，返回錯誤訊息列表
        List<String> errors = registerService.registerUser(user);

        // 如果有錯誤，將錯誤訊息放入model並返回註冊頁面
        if(!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            return "loginOut/register";  // 若註冊失敗，返回註冊頁面並顯示錯誤
        }

        // 若註冊成功，重定向到登入頁面
        return "redirect:loginOut/login";
    }
}