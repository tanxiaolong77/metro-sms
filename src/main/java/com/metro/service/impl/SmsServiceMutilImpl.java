package com.metro.service.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.metro.service.SmsService;
import com.quanjing.util.ActResult;
import com.quanjing.util.EmailUtil;
import com.quanjing.util.StringUtils;

@Service("smsService")
public class SmsServiceMutilImpl implements SmsService{

	//private static String smsUrl = "http://dxhttp.c123.cn/";
	    
	private static BlockingQueue<String[]> sendQueue=new LinkedBlockingQueue<String[]>(60000);
	
	private static Logger logg=LoggerFactory.getLogger(SmsServiceMutilImpl.class);
	
	private final int threadCount=30;
	
	private ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
	
	
	private class Send implements Runnable{
		private Map<String,Object> paramMap=new HashMap<String,Object>();
		
		public void run() {
			
			while(true){
				String[] sms=null;
				try {
					sms = sendQueue.take();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				if(sms!=null){
					
					try {
						logg.info("send:"+sms[0]+"="+sms[1]);
						send(sms[0], sms[1]);
						
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}else{
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
		}
		
	}
	
	
	public SmsServiceMutilImpl() {
		logg.info("init send thread:"+threadCount);
		for(int i=0;i<threadCount;i++){
			Send t1 = new Send();
			threadPool.execute(t1);
		}

	}


	/**
	 * 发送证码
	 */
	public ActResult<Object> sendCode(String phoneNumber,String code) {
		
		String content = "您的验证码为："+code;
		
		if(StringUtils.isPhoneNumber(phoneNumber)){
			logg.info("in send queue:"+phoneNumber+"="+content);
			sendQueue.add(new String[]{phoneNumber,content});
			return ActResult.success();
		}else{
			return ActResult.fail("手机号码有误");
		}
		
		
	}
	
	
	public ActResult<Object> sendContent(String phoneNumber,String content) {
		
		if(StringUtils.isEmpty(content)){
			return ActResult.fail("内容不能为空");
		}
		
		if(StringUtils.isPhoneNumber(phoneNumber)){
			logg.info("in send queue:"+phoneNumber+"="+content);
			sendQueue.add(new String[]{phoneNumber,content});
			return ActResult.success();
		}else{
			return ActResult.fail("手机号码有误");
		}
		
		
	}

	
	
	private static void send(String phone,String content){
		
		DefaultHttpClient httpClient = new DefaultHttpClient();
		String soapRequestData = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
		"<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">"  +
		  "<soap12:Body>" +
		   " <SendPostSMS xmlns=\"http://tempuri.org/\">" +
		     "<mobiles>"+phone+"</mobiles>" +
		      "<content>"+content+"</content>" +
		    "</SendPostSMS>" +
		 " </soap12:Body>" +
		"</soap12:Envelope>" ;
		
		HttpPost httppost = new HttpPost("http://sms.tiankong.com/service1.asmx?op=SendPostSMS");
		/*把Soap请求数据添加到PostMethod*/
		//byte[] b = soapRequestData.getBytes("utf-8");
		//InputStream is = new ByteArrayInputStream(b,0,b.length);
		try {
			HttpEntity re = new StringEntity(soapRequestData,HTTP.UTF_8);
			httppost.setHeader("Content-Type","application/soap+xml; charset=utf-8");
			//httppost.setHeader("Content-Length", String.valueOf(soapRequestData.length()));
			httppost.setEntity(re);			
			HttpResponse response = httpClient.execute(httppost);
			logg.info(response.getStatusLine().toString());
			if(response.getStatusLine().getStatusCode()!=200){
				//发邮件
				EmailUtil.getInstance().send("短信发送接口异常", phone+"<-"+content+"<hr/>"+response.getStatusLine().toString()+"<br/>"+EntityUtils.toString(response.getEntity()), "xiehs@quanjing.com");
			}
			logg.info(EntityUtils.toString(response.getEntity()));			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			httpClient.getConnectionManager().shutdown();
		}

			
	}
	


}
