package com.asiainfo.cs.common.util;

import java.util.Map;

/**
 * 通知事件实体
 * 
 * @author
 */
public class OcsNotify {
	private String receiver;
	private String chatId;
	private int notifyCode;
	private String notifyMsg;
	private String originalUserId; //用户ID
	private String customerSessionId;//用户sessionId
	private String result;
	private String errMsg;
	private int count;
	private Map<String, String> props;

	public OcsNotify() {
		super();
	}

	public String toString() {
		return "Notify [receiver=" + receiver + ", chatId=" + chatId + ", customerSessionId="+customerSessionId+", notifyCode=" + notifyCode + ", notifyMsg="
				+ notifyMsg + ", originalUserId=" + originalUserId + ", result=" + result + ", errMsg=" + errMsg + "]";
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getCustomerSessionId() {
		return customerSessionId;
	}

	public void setCustomerSessionId(String customerSessionId) {
		this.customerSessionId = customerSessionId;
	}

	public String getReceiver() {
		return receiver;
	}

	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}

	public String getChatId() {
		return chatId;
	}

	public void setChatId(String chatId) {
		this.chatId = chatId;
	}

	public int getNotifyCode() {
		return notifyCode;
	}

	public void setNotifyCode(short notifyCode) {
		this.notifyCode = notifyCode;
	}

	public String getNotifyMsg() {
		return notifyMsg;
	}

	public void setNotifyMsg(String notifyMsg) {
		this.notifyMsg = notifyMsg;
	}

	public String getOriginalUserId() {
		return originalUserId;
	}

	public void setOriginalUserId(String originalUserId) {
		this.originalUserId = originalUserId;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getErrMsg() {
		return errMsg;
	}

	public void setErrMsg(String errMsg) {
		this.errMsg = errMsg;
	}

	public Map<String, String> getProps() {
		return props;
	}

	public void setProps(Map<String, String> props) {
		this.props = props;
	}

}
