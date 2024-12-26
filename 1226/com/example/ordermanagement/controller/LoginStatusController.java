package com.example.ordermanagement.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.ordermanagement.model.LoginBean;

import jakarta.servlet.http.HttpSession;

@RestController
public class LoginStatusController {
	
	@GetMapping("/loginData") //從DB存取UserInfo資料進Session (by Mantle) 
	@ResponseBody
	public ResponseEntity<?> getLoginStatus(HttpSession session) {
	    LoginBean user = (LoginBean) session.getAttribute("user");
	    if (user != null) {
	        return ResponseEntity.ok(user); // 返回最新的用戶資料
	    } else {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not logged in");
	    }
	}

    @GetMapping("/api/check-user-id")
    @ResponseBody
    public ResponseEntity<?> checkUserId(HttpSession session) {
        LoginBean user = (LoginBean) session.getAttribute("user");
        if (user != null) {
            return ResponseEntity.ok("User ID: " + user.getUserId());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not logged in");
        }
    }
	
	@GetMapping("/api/check-login")
    public boolean checkLoginStatus(HttpSession session) {
		return session.getAttribute("user") != null;
	
	}
}