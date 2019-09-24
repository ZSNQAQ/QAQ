package com.asiainfo.cs.entity;

import com.asiainfo.cs.common.util.StaticValue.EAccessType;
import com.asiainfo.cs.common.util.StaticValue.EActionType;
import com.asiainfo.cs.common.util.StaticValue.ESeatMediaState;

import java.io.Serializable;

public class CustomInfo implements Serializable  {
	String agentId;
	//客户ID
	String userAccount;
	//用户显示名称
	String userName;
	//客户唯一标识
	String originalId;
	//请求唯一标识
	String sessionId;
	//渠道，电话 微信
	EAccessType accessType;
	//请求子类型:微信号等
	String requestSubParam;
	//请求开始时间
	Long requestTime= System.currentTimeMillis()/1000L;
	//当前请求的状态:
	ESeatMediaState mediaState=ESeatMediaState.RING; 
	//请求的队列标识
	String queueId;
	//请求的队列说明
	String queueDesc;
	//请求类型
	EActionType ringType=EActionType.SERVICE;
	EActionType agentRequestType=EActionType.NONE;
	String agentRequestId;
	String targetId;
	//转移请求开始时间
	Long transTime=0L;
	
	public EActionType getAgentRequestType() {
		return agentRequestType;
	}
	public void setAgentRequestType(EActionType requestType) {
		this.agentRequestType = requestType;
	}

	
	public CustomInfo(String agentId,String originalUUID,String sessionId){
		this.agentId=agentId;
		this.sessionId=sessionId;
		this.originalId=originalUUID;
	}
	public CustomInfo(String agentId){
		this.agentId=agentId;
	}
	
	public String getUserAccount() {
		return userAccount;
	}
	public void setUserAccount(String userAccount) {
		this.userAccount = userAccount;
	}
	
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	
	public String getAgentId() {
		return agentId;
	}
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	
	public String getSessionId() {
		return sessionId;
	}
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	
	public String getOriginalId() {
		return originalId;
	}
	public EAccessType getAccessType() {
		return accessType;
	}
	public String getRequestSubParam() {
		return requestSubParam;
	}
	public Long getRequestTime() {
		return requestTime;
	}
	public ESeatMediaState getMediaState() {
		return mediaState;
	}
	public String getQueueId() {
		return queueId;
	}
	public String getQueueDesc() {
		return queueDesc;
	}
	public EActionType getRingType() {
		return ringType;
	}
	public String getTargetId() {
		return targetId;
	}
	public Long getTransTime() {
		return transTime;
	}

	public void setOriginalId(String customId) {
		this.originalId = customId;
	}
	public void setAccessType(EAccessType requestType) {
		this.accessType = requestType;
	}
	public void setRequestSubParam(String requestSubParam) {
		this.requestSubParam = requestSubParam;
	}
	public void setRequestTime(Long requestTime) {
		this.requestTime = requestTime;
	}
	public void setMediaState(ESeatMediaState seatOnlineState) {
		this.mediaState = seatOnlineState;
	}
	public void setQueueId(String queueId) {
		this.queueId = queueId;
	}
	public void setQueueDesc(String queueDesc) {
		this.queueDesc = queueDesc;
	}
	public void setRingType(EActionType ringType) {
		this.ringType = ringType;
	}
	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}
	public void setTransTime(Long transTime) {
		this.transTime = transTime;
	}

}
