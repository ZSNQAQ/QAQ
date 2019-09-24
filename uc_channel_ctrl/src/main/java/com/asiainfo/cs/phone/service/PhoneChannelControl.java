package com.asiainfo.cs.phone.service;

import com.asiainfo.cs.service.ChannelControl;

public interface PhoneChannelControl extends ChannelControl {

	void setFollowData(String agentId, String sessionId, String date);//设置用户信道的随路数据
	String getFollowData(String agentId, String sessionId);//设置用户信道的随路数据
	void setChannelData(String agentId, String sessionId, String key, String date);
	String getChannelData(String agentId, String sessionId, String key);//设置用户信道的随路数据

	void startRecordSingle(String agentId, String sessionId, String file);
	void stopRecordSingle(String agentId, String sessionId);

	void sendDTMF(String agentId, String sessionId, String DTMF);//发送消息到对端
	void hold(String currentAgent, String sessionId, boolean hold);

	void forceSignOut(String targetAgent, String currentAgent);	// 强制签出
    void forceSayFree(String targetAgent, String currentAgent);	// 强制示闲
    void forceSayBusy(String targetAgent, String currentAgent);	// 强制示忙

 

}
