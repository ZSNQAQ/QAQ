package com.asiainfo.cs.service;

import com.corundumstudio.socketio.SocketIOClient;

import java.util.Map;

public interface SeatControl {
	void setSkills(SocketIOClient client, String seat, Map<String, String> data);
	void login(SocketIOClient client, Map<String, String> data);
	void logout(SocketIOClient client, String agentId);
	void answerMore(String agentId);
}
