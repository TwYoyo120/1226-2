package com.example.ordermanagement.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

import com.example.ordermanagement.model.LoginBean;
import com.example.ordermanagement.service.LoginService;

import jakarta.servlet.http.HttpSession;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	private final LoginService loginService;

	 // 注入 LoginService
    public SecurityConfig(LoginService loginService) {
        this.loginService = loginService;
    }
	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
		// CSRF 設定 (方便開發期間測試，可視需求停用)
        .csrf(csrf -> csrf.disable())
        
		// OAuth2 登入設定，這是 Spring Security 內建的 OAuth2 登入功能
        .oauth2Login(oauth2 -> oauth2
        		.loginPage("/login")// 自定義登入頁面
        		.successHandler((request, response, authentication) -> {
        			// 取得 Google OAuth2 登入後的用戶資訊
        			OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        			String email = oauth2User.getAttribute("email");
        			String name = oauth2User.getAttribute("name");
        			
        			
        			// 在控制台輸出用戶資訊
        		    System.out.println("Email: " + email);
        		    System.out.println("Name: " + name);
        		    
        		    
        		 // 使用 LoginService 處理 Google 登入
                    LoginBean user = loginService.handleGoogleLogin(email, name);
        			
                 
        			
        			user.setUserEmail(email);
        			user.setUserName(name);
        			user.setStatus("success");
        			
        			// 存入 Session
        			HttpSession session = request.getSession();
        			session.setAttribute("user", user);
        			
        			// 重定向到會員中心
        			response.sendRedirect("/memberCenter");
                    
                    
                    
            })
        );
     
     
    
    return http.build();
	}
	
	
}