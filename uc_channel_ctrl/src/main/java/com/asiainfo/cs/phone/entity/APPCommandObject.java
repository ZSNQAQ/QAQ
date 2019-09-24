package com.asiainfo.cs.phone.entity;

import java.util.Map;

public class APPCommandObject {
	private String host;
	private String appName;
	private String arg;
	private String uuid;
	private Map<String,String> headers;
	public String getHost() {
		return host;
	}
	public String getAppName() {
		return appName;
	}
	public String getArg() {
		return arg;
	}
	public String getUuid() {
		return uuid;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public void setAppName(String appName) {
		this.appName = appName;
	}
	public void setArg(String arg) {
		this.arg = arg;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public Map<String,String> getHeaders() {
		return headers;
	}
	public void setHeaders(Map<String,String> headers) {
		this.headers = headers;
	}


}
