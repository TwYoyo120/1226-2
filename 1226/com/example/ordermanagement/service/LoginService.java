package com.example.ordermanagement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.ordermanagement.model.LoginBean;
import com.example.ordermanagement.model.LoginRepository;




@Service
public class LoginService {

	@Autowired
	private LoginRepository loginRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	public LoginBean authenticate(String userEmail, String userPassword) {
		// 日誌輸出
		System.out.println("Authenticating user: " + userEmail + " with password: " + userPassword);
		// 查詢資料庫以驗證用戶
		LoginBean user = loginRepository.findByUserEmail(userEmail);// 根據 email 查詢用戶
		if (user == null) {
		
		// 查無此帳號，返回帳號不存在的錯誤訊息
		LoginBean failedUser = new LoginBean();
		failedUser.setStatus("fail");
		failedUser.setMessage("帳號不存在");
		System.out.println("後端提示-帳號不存在");
		return failedUser;
		}
		
		// 使用 passwordEncoder 比對明文密碼和加密密碼
		if(passwordEncoder.matches(userPassword, user.getUserPassword())) {
			// 如果密碼匹配，檢查帳號狀態
		if(user.getUserStatus() == 1){
			// 帳號啟用且密碼匹配，返回用戶資料
			user.setStatus("success");
			user.setMessage("登入成功");
			System.out.println("後端提示-登入成功 : " + user.getUserEmail());
			return user;
		}else {
			// 帳號未啟用，返回錯誤訊息
			user.setStatus("fail");
			user.setMessage("帳號未啟用");
			System.out.println("後端提示-帳號未啟用 : " + user.getUserEmail());
			return user;
		}
		}else {
			// 密碼錯誤，返回錯誤訊息
			user.setStatus("fail");
			user.setMessage("帳號或密碼錯誤");
			System.out.println("後端提示-密碼錯誤 : " + user.getUserEmail());
			return user;
		}
	}
	
	// 處理 Google 登入邏輯
	public LoginBean handleGoogleLogin(String email, String name) {
		System.out.println("Handling Google login for user: " + email);
		
		// 查詢資料庫以檢查用戶是否存在
        LoginBean user = loginRepository.findByUserEmail(email);
        
        if (user == null) {
            // 若用戶不存在，創建新用戶並保存
            user = new LoginBean();
            user.setUserEmail(email);
            user.setUserName(name);
            user.setUserStatus(1);	//預設帳號為啟用狀態
//            user.setLoginType("GOOGLE");	//用此註冊方式，值設定為GOOGLE。
            user.setStatus("success");
            user.setMessage("用戶資料創建成功");
            loginRepository.save(user);
        }
     // 返回用戶資料
        return user;
	}
}