package com.asiainfo.cs.socketio;

import java.util.Map;

public interface FairAgentEvent {
	boolean fairEvent(String agentId, String event, Map<String, String> param);

}
