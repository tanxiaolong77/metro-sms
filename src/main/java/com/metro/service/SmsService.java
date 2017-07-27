package com.metro.service;

import com.quanjing.util.ActResult;

public interface SmsService {
	
	/**
	 * 发送短信验证码
	 * @param phoneNumber
	 * @return
	 */
	ActResult<Object> sendCode(String phoneNumber, String code);

	ActResult<Object> sendContent(String phoneNumber, String content);
	
}
