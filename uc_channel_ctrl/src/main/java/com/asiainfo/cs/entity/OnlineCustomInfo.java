package com.asiainfo.cs.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class OnlineCustomInfo extends CustomInfo implements Serializable  {


	private static final Gson gson = new GsonBuilder()
            .create();
	private static final long serialVersionUID = 6474855974870687417L;
	public static OnlineCustomInfo createFromJson(String jsonStr){
		try {
			return gson.fromJson(jsonStr, OnlineCustomInfo.class);
		} catch (JsonSyntaxException e) {
			return null;
		}
	}
	public  String toString(){
		return gson.toJson(this);
	}


	public OnlineCustomInfo(String agentId, String originalUUID, String sessionId) {
		super(agentId, originalUUID, sessionId);
		// TODO Auto-generated constructor stub
	}


	Map<String,String> props=new HashMap<String,String>();
	public Map<String, String> getProps() {
		return props;
	}
	public void setProps(Map<String, String> props) {
		this.props = props;
	}

}
