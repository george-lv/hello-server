
package com.greentown.smscenter.model;

/**
 * 校验验证码请求对象
 * @author hofon
 * @version $Id: CheckAuthCodeRequest.java, v0.1 2016年4月8日 下午2:48:45 hofon Exp $
 */
public class CheckAuthCodeRO extends BaseRO {

    /** SID */
    private static final long serialVersionUID = 6271280003758864576L;


    private String            mobile;

    /** 业务号 */
    private String            bizNo;

    private String            authCode;
    
    private String            templateNo;

    
    public String getTemplateNo() {
		return templateNo;
	}

	public void setTemplateNo(String templateNo) {
		this.templateNo = templateNo;
	}

	/**
     * Getter method for property <tt>mobile</tt>.
     * 
     * @return property value of mobile
     */
    public String getMobile() {
        return mobile;
    }

    /**
     * Setter method for property <tt>mobile</tt>.
     * 
     * @param mobile value to be assigned to property mobile
     */
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    /**
     * Getter method for property <tt>bizNo</tt>.
     * 
     * @return property value of bizNo
     */
    public String getBizNo() {
        return bizNo;
    }

    /**
     * Setter method for property <tt>bizNo</tt>.
     * 
     * @param bizNo value to be assigned to property bizNo
     */
    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    /**
     * Getter method for property <tt>authCode</tt>.
     * 
     * @return property value of authCode
     */
    public String getAuthCode() {
        return authCode;
    }

    /**
     * Setter method for property <tt>authCode</tt>.
     * 
     * @param authCode value to be assigned to property authCode
     */
    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

}
