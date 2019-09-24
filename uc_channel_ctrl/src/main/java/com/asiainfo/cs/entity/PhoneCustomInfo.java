package com.asiainfo.cs.entity;

import com.asiainfo.cs.common.util.StaticValue.EAccessType;
import com.asiainfo.cs.common.util.StaticValue.EMonitorType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Serializable;

public class PhoneCustomInfo extends CustomInfo implements Serializable  {
	public PhoneCustomInfo(String agentId, String originalUUID, String sessionId) {
		super(agentId, originalUUID, sessionId);
		this.accessType=EAccessType.ACCESS_TYPE_TELE;
	}
	private static final Gson gson = new GsonBuilder()
            .serializeNulls()
            .create();
	private static final long serialVersionUID = 6474855974870687417L;
	public static PhoneCustomInfo createFromJson(String jsonStr){
		return gson.fromJson(jsonStr, PhoneCustomInfo.class);
		
	}
	
	public  String toString(){
		return gson.toJson(this);
	}
	
	
	String agentChannId;
	String targetChannId;
	String sourceChannId;
	String hostName;
	EMonitorType monitorType;
	String monitorId;

	public String getAgentUUID() {
		return agentChannId;
	}

	public String getTargetUUID() {
		return targetChannId;
	}

	public String getSourceUUID() {
		return sourceChannId;
	}

	public String getHostName() {
		return hostName;
	}

	public EMonitorType getMonitorType() {
		return monitorType;
	}

	public String getMonitorId() {
		return monitorId;
	}

	public void setAgentUUID(String agentUUID) {
		this.agentChannId = agentUUID;
	}

	public void setTargetUUID(String targetUUID) {
		this.targetChannId = targetUUID;
	}

	public void setSourceUUID(String sourceUUID) {
		this.sourceChannId = sourceUUID;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public void setMonitorType(EMonitorType monitorType) {
		this.monitorType = monitorType;
	}

	public void setMonitorId(String monitorId) {
		this.monitorId = monitorId;
	}

	
	
}
