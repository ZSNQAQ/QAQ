package com.asiainfo.cs.service;

import com.asiainfo.cs.common.util.StaticValue.EActionType;
import com.asiainfo.cs.common.util.StaticValue.EMonitorType;
import com.asiainfo.cs.common.util.StaticValue.ESeatMediaState;
import com.asiainfo.cs.common.util.StaticValue.ESeatState;
import com.asiainfo.cs.entity.OnlineCustomInfo;
import com.asiainfo.cs.entity.PhoneCustomInfo;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface UserRedisCache {
	boolean  agentExists(String agent);
	boolean loginAgentInfo(String agent);
	void cleanAgentInfo(String agent);
	void setAgentInfoExpire(String agent);
	
	String setAgentState(String agent, ESeatState value);
	ESeatState getAgentState(String agent);

	void setAgentUUID(String agent, UUID value);
	UUID getAgentUUID(String agent);


	void setAgentServiceUUID(String agent, String value);
	String getAgentServiceUUID(String agent);

	void setAgentStateOnHangup(String agent, ESeatState value);
	ESeatState getAgentStateOnHangup(String agent);

	void setAgentChannelName(String agent, String value);
	String getAgentChannelName(String agent);

	void setAgentOnlineUUID(String agent, String value);
	String getAgentOnlineUUID(String agent);

	void setAgentIP(String agent, String value);
	void setAgentLoginName(String agent, String value);
	String getAgentIP(String agent);

	Set<String> getOnlineAgent();
	Map<String,String> getAgentInfo(String agent);

	void setAgentCustomInfo(String agent, String key, String value);
	String getAgentCustomInfo(String agent, String key);




	void setAgentChannId(String agent, String value);
	void setAgentChannHost(String agent, String value);

	String getAgentChannId(String agent);
	String getAgentChannHost(String agent);


	void setUserChannId(String agent, String value);
	String getUserChannId(String agent);

	void setUserChannHost(String agent, String value);
	String getUserChannHost(String agent);

	void setUserName(String agent, String value);
	String getUserName(String agent);


	void setTargetChannId(String agent, String value);
	String getTargetChannId(String agent);

	void setTargetHost(String agent, String value);
	String getTargetHost(String agent);

	void setTargetName(String agent, String value);
	String getTargetName(String agent);



	void setSourceChannId(String agent, String value);
	String getSourceChannId(String agent);

	void setSourceHost(String agent, String value);
	String getSourceHost(String agent);

	void setSourceName(String agent, String value);
	String getSourceName(String agent);


	void setAgentAudioState(String agent, ESeatMediaState value);
	ESeatMediaState getAgentAudioState(String agent);

	void setRingType(String agent, EActionType value);
	EActionType getRingType(String agent);

	void setAgentRequestType(String agent, EActionType value);
	EActionType getAgentRequestType(String agent);

	void setAgentRequestId(String agent, String value);
	String getAgentRequestId(String agent);


	void setSessionId(String agent, String value);
	String getSessionId(String agent);

	//huangqb add for test 2019-4-15
	void setOriginalNumber(String agent, String value);
	String getOriginalNumber(String agent);
	//end of huangqb add 2019-4-15

	void setMonitorType(String agent, EMonitorType value);
	EMonitorType getMonitorType(String agent);

	void setMonitorID(String agent, String value);
	String getMonitorID(String agent);

	//电话部分
	PhoneCustomInfo getPhoneRequest(String agent, String chatId);
	void setPhoneRequest(String agent, String chatId, PhoneCustomInfo value);
	//互联网部分
	OnlineCustomInfo getInternetRequest(String agent, String chatId);
	void setInternetRequest(String agent, String chatId, OnlineCustomInfo value);

}
