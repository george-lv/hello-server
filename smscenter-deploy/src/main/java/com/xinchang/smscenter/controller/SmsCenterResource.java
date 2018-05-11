package com.xinchang.smscenter.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang3.math.NumberUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONObject;
import com.xinchang.common.util.HttpSender;
import com.xinchang.smscenter.result.CallRemoteResult;
import com.xinchang.smscenter.result.RemoteResultEnum;
import com.xinchang.smscenter.ro.CheckAuthCodeRO;
import com.xinchang.smscenter.ro.SendAuthCodeRO;
import com.xinchang.smscenter.utils.SystemConstants;


@RestController
@RequestMapping("/rest/sms")
public class SmsCenterResource {

	 private static final Logger  logger = LoggerFactory.getLogger(SmsCenterResource.class);
	 
    @Resource
    private RedisTemplate<String,Long> redisTemplate;

    @Value("${message.url}")
    private  String url;
    
    @Value("${message.name}")
    private  String name;
    
    @Value("${message.password}")
    private  String password;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @RequestMapping(value="/sendMsg",method = RequestMethod.POST)
	public CallRemoteResult<Boolean> sendMsg(@RequestBody Map<String, String> paramMap) {
		
		logger.info("Sending message {} to {} ",paramMap.get("content"),paramMap.get("mobile"));
		
		Boolean result=false ;
		
        Map<String, String> map = new HashMap<String, String>();
   		map.put("name", name);
   		map.put("pswd", password);
   		map.put("mobile", paramMap.get("mobile"));
   		map.put("msg", paramMap.get("content"));
   		map.put("needstatus", String.valueOf(true));
   		map.put("sender", null);
   		map.put("type", "json");
   		String returnString=HttpSender.send(url, map);
   		JSONObject json=JSONObject.parseObject(returnString);
   		
   		int respStatus=(int)json.get("respstatus");
   		if (respStatus==0){
   			result=true;
   		}
   		
		return new CallRemoteResult<>(result);
	}
	
	
	
    @RequestMapping(value="/sendAuthCode",method = RequestMethod.POST)
    public CallRemoteResult<Boolean> sendAuthCode(@RequestBody SendAuthCodeRO sendAuthCodeRo)  {
    	CallRemoteResult<Boolean> result = new CallRemoteResult<>();
            String bizNo = sendAuthCodeRo.getBizNo();
            String mobile = sendAuthCodeRo.getMobile();
            String key = bizNo + SystemConstants.SEPARATOR + mobile;
            String verifiableCount = bizNo + SystemConstants.SEPARATOR + mobile
                                     + SystemConstants.SEPARATOR
                                     + SystemConstants.VERIFIABLE_COUNT;
            Long authCode = (Long) redisTemplate.opsForValue().get(key);// 先查缓存
            // 如果 缓存 不存在,验证码失效或未申请过
            if (authCode==null) {
                int codeLength = sendAuthCodeRo.getCodeLength();
                authCode = generateAuthCode(codeLength);
                // 发送验证码
                logger.info("开始发送验证码,sendAuthCodeRo=" + sendAuthCodeRo);
                Map<String, String> map = new HashMap<String, String>();
        		map.put("name", name);
        		map.put("pswd", password);
        		map.put("mobile", sendAuthCodeRo.getMobile());
        		map.put("msg", "您的验证码是"+authCode+",感谢您的支持,请勿泄露给他人!");//用自己的内容白名单做测试
        		map.put("needstatus", String.valueOf(true));
        		map.put("sender", null);
        		map.put("type", "json");
        		
        		String returnString=this.restTemplate.postForObject(url, map, String.class);
        		
        		logger.info("returnString:{}",returnString);
        		
        		logger.info("mobile:{},authCode:{},key:{}",mobile,authCode,key);
        		 
        		JSONObject json=JSONObject.parseObject(returnString);
        		int respStatus=(int)json.get("respstatus");
        		if (respStatus==0){
        			 // 缓存验证码
                    System.out.println("validperid:"+sendAuthCodeRo.getValidPeriod());
                    
                    redisTemplate.opsForValue().set(key, authCode,sendAuthCodeRo.getValidPeriod(),TimeUnit.SECONDS);
                    // 缓存验证码可验证次数
                    redisTemplate.opsForValue().set(verifiableCount,sendAuthCodeRo.getVerifiableCount(), sendAuthCodeRo.getValidPeriod(),TimeUnit.SECONDS);
        		}
               
            } else {
            	result.setReturnObject(false);
            	result.setResultEnum(RemoteResultEnum.DUPLICATE_REQUEST);
              
            }

        return result;
    }
    
    @RequestMapping(value="/checkAuthCode",method = RequestMethod.POST)
   	public CallRemoteResult<Boolean> checkAuthCode(CheckAuthCodeRO checkAuthCodeRo) {
       	CallRemoteResult<Boolean> result = new CallRemoteResult<>();
           String bizNo = checkAuthCodeRo.getBizNo();
           String mobile = checkAuthCodeRo.getMobile();
           String key = bizNo + SystemConstants.SEPARATOR + mobile;
           String verifiedCount = bizNo + SystemConstants.SEPARATOR + mobile
                                  + SystemConstants.SEPARATOR
                                  + SystemConstants.VERIFIED_COUNT;
           String verifiableCount = bizNo + SystemConstants.SEPARATOR + mobile
                                    + SystemConstants.SEPARATOR
                                    + SystemConstants.VERIFIABLE_COUNT;
           
           Long authCode = (Long) redisTemplate.opsForValue().get(key);// 先查缓存
           
           logger.info("key:{},mobile:{},authCode:{}",key,mobile,authCode);
           
           result.setReturnObject(true);
           
           if (authCode==null) {
           	result.setReturnObject(false);
           	result.setResultEnum(RemoteResultEnum.AUTH_CODE_EXPIRE);
           }else{
           	Long count1 = redisTemplate.opsForValue().increment(verifiedCount, 1l);
           	Long count2 = redisTemplate.opsForValue().get(verifiableCount);
           	if(!checkAuthCodeRo.getAuthCode().equals(String.valueOf(authCode))){
           		//达到最大次数
           		if(count2.longValue()==count1.longValue()){
           			result.setReturnObject(false);
           			redisTemplate.delete(key);
           			redisTemplate.delete(verifiedCount);
           			redisTemplate.delete(verifiableCount);
           			result.setReturnObject(false);
           			result.setResultEnum(RemoteResultEnum.BEYOND_MAXIMUM_TIMES);
                       return result;
           		}
           		result.setReturnObject(false);
           		result.setResultEnum(RemoteResultEnum.INPUT_AUTH_CODE_WRONG);
           	}else{
       			result.setReturnObject(true);
       			redisTemplate.delete(key);
       			redisTemplate.delete(verifiedCount);
       			redisTemplate.delete(verifiableCount);
           	}
           }
   		return result;
   	}
    
    
    @RequestMapping(value="/hello",method = RequestMethod.GET)
    public String helloSms(){
    	
    	return "from sms server";
    }
    /**
     * 生成验证码
     * 
     * @param length
     * @param seed
     * @return
     */
    private Long generateAuthCode(int length) {
        StringBuilder s = new StringBuilder();
        char[] seed = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
        for (int i = 0; i < length; i++) {
            int index = (int) Math.floor(Math.random() * seed.length);
            s.append(seed[index]);
        }
        return NumberUtils.toLong(s.toString());
    }
    
    
}
