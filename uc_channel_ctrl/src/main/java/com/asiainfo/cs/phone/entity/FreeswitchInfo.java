package com.asiainfo.cs.phone.entity;

public class FreeswitchInfo {
	private String host;
	private int port;
	private String httpapi;
	private String password;
	private Long lastHeart=System.currentTimeMillis();
	private String core_uuid;

	public String getCore_uuid() {
		return core_uuid;
	}

	public void setCore_uuid(String core_uuid) {
		this.core_uuid = core_uuid;
	}

	public Long getLastHeart() {
		return lastHeart;
	}
	public void setLastHeart(Long lastHeart) {
		this.lastHeart = lastHeart;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}

	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getHttpapi() {
		return httpapi;
	}
	public void setHttpapi(String httpapi) {
		this.httpapi = httpapi;
	}
	
}
