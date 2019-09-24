package com.asiainfo.cs.socketio.impl;

import com.asiainfo.cs.online.service.OnlineCustomService;
import com.asiainfo.cs.service.UserRedisCache;
import com.asiainfo.cs.socketio.FairAgentEvent;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component("fairAgentEventImpl")
public class FairAgentEventImpl implements FairAgentEvent {
	@Autowired
	private SocketIOServer server;
	@Autowired
	private UserRedisCache userRedisCache;
	@Autowired
	OnlineCustomService onlineService;
	
	@Override
	public boolean fairEvent(String agentId, String event, Map<String, String> param) {
		if (!userRedisCache.agentExists(agentId)){
			return false;
		}
		UUID uuid=userRedisCache.getAgentUUID(agentId);
		SocketIOClient client=server.getClient(uuid);
		if(null!=client){
			client.sendEvent(event, param);
			return true;
		}else{
			return false;
		}
	}
}
