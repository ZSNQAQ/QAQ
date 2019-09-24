package com.asiainfo.cs.entity;

import com.asiainfo.cs.common.util.StaticValue.ESeatState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Serializable;
import java.util.Map;

public class AgentInfo  implements Serializable {
	private static final Gson gson = new GsonBuilder()
            .serializeNulls()
            .create();

	public static AgentInfo createFromJson(String jsonStr){
		return gson.fromJson(jsonStr, AgentInfo.class);
		
	}
	
	public  String toString(){
		return gson.toJson(this);
	}
	
	private static final long serialVersionUID = 4328574673300233481L;
	String agentId;
	String agnetIP;
	String agnetConnectUUID;
	String agnetServiceUUID;
	ESeatState agentState;
	ESeatState stateOnHangup;
	String phoneConnectId;
	String onlineConnectId;
	long loginTimestamp;
	
	Map<String, PhoneCustomInfo> phoneList;
	Map<String, OnlineCustomInfo> onlineList;

	public String getAgentId() {
		return agentId;
	}

	public String getAgnetIP() {
		return agnetIP;
	}

	public String getAgnetConnectUUID() {
		return agnetConnectUUID;
	}

	public String getAgnetServiceUUID() {
		return agnetServiceUUID;
	}

	public ESeatState getAgentState() {
		return agentState;
	}

	public ESeatState getStateOnHangup() {
		return stateOnHangup;
	}

	public String getPhoneConnectId() {
		return phoneConnectId;
	}

	public String getOnlineConnectId() {
		return onlineConnectId;
	}

	public long getLoginTimestamp() {
		return loginTimestamp;
	}

	public Map<String, PhoneCustomInfo> getPhoneList() {
		return phoneList;
	}

	public Map<String, OnlineCustomInfo> getOnlineList() {
		return onlineList;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public void setAgnetIP(String agnetIP) {
		this.agnetIP = agnetIP;
	}

	public void setAgnetConnectUUID(String agnetConnectUUID) {
		this.agnetConnectUUID = agnetConnectUUID;
	}

	public void setAgnetServiceUUID(String agnetServiceUUID) {
		this.agnetServiceUUID = agnetServiceUUID;
	}

	public void setAgentState(ESeatState agentState) {
		this.agentState = agentState;
	}

	public void setStateOnHangup(ESeatState stateOnHangup) {
		this.stateOnHangup = stateOnHangup;
	}

	public void setPhoneConnectId(String phoneConnectId) {
		this.phoneConnectId = phoneConnectId;
	}

	public void setOnlineConnectId(String onlineConnectId) {
		this.onlineConnectId = onlineConnectId;
	}

	public void setLoginTimestamp(long loginTimestamp) {
		this.loginTimestamp = loginTimestamp;
	}

	public void setPhoneList(Map<String, PhoneCustomInfo> phoneList) {
		this.phoneList = phoneList;
	}

	public void setOnlineList(Map<String, OnlineCustomInfo> onlineList) {
		this.onlineList = onlineList;
	}
	
	
}
