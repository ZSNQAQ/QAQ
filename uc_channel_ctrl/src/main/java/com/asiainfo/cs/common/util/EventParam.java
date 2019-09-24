package com.asiainfo.cs.common.util;

import com.asiainfo.cs.common.util.StaticValue.EAccessType;
import com.asiainfo.cs.common.util.StaticValue.EActionType;
import com.asiainfo.cs.common.util.StaticValue.ESeatMediaState;
import com.asiainfo.cs.common.util.StaticValue.ESeatState;
import com.asiainfo.cs.entity.OnlineCustomInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class EventParam {
	private static final Gson gson = new GsonBuilder()
            .create();
	public static OnlineCustomInfo createFromJson(String jsonStr){
		return gson.fromJson(jsonStr,OnlineCustomInfo.class);
	}
	@Override
	public  String toString(){
		return gson.toJson(this);
	}

	public EventParam(String agentId,EAccessType accessType){
		this.agentId=agentId;
		this.accessType=accessType;
	}
	public String getAgentId() {
		return agentId;
	}
	public EAccessType getAccessType() {
		return accessType;
	}
	public String getAccessTypeDetail() {
		return accessTypeDetail;
	}
	public EActionType getRingType() {
		return ringType;
	}
	public String getSessionId() {
		return sessionId;
	}
	public ESeatMediaState getMediaState() {
		return mediaState;
	}
	public String getUserAccount() {
		return userAccount;
	}
	public ESeatState getSeatState() {
		return seatState;
	}
	public String getTargetId() {
		return targetId;
	}
	public String getEventDesc() {
		return eventDesc;
	}
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}
	public void setAccessType(EAccessType accessType) {
		this.accessType = accessType;
	}
	public void setAccessTypeDetail(String accessTypeDetail) {
		this.accessTypeDetail = accessTypeDetail;
	}
	public void setRingType(EActionType ringType) {
		this.ringType = ringType;
	}
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	public void setMediaState(ESeatMediaState mediaState) {
		this.mediaState = mediaState;
	}
	public void setUserAccount(String userAccount) {
		this.userAccount = userAccount;
	}
	public void setSeatState(ESeatState seatState) {
		this.seatState = seatState;
	}
	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}
	public void setEventDesc(String eventDesc) {
		this.eventDesc = eventDesc;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getOriginalUserId() {
		return originalUserId;
	}
	public void setOriginalUserId(String originalUserId) {
		this.originalUserId = originalUserId;
	}
	String agentId;//坐席工号
	String userAccount;//用户ID
	String userName;
	String sessionId;//会话ID--唯一值
	String originalUserId;//用户连接id
	EAccessType accessType;//渠道类型:电话、互联网
	String accessTypeDetail;//子渠道:接入号码，微信公众号
	EActionType ringType;//振铃类型:服务请求、转移坐席
	ESeatMediaState mediaState;//媒体状态：振铃、应答。。
	ESeatState seatState;//坐席状体:忙，闲，整理
	String targetId;//目标Id
	String eventDesc;//事件描述
	String value;

	
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
}
