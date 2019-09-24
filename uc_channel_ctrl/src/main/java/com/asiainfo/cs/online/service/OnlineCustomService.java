package com.asiainfo.cs.online.service;

import java.util.List;
import java.util.Map;

public interface OnlineCustomService {
	Map<?,?> signIn(String seat, String onlineMediaUUID, String signId);
	Map<?,?> signOut(String agentId);
	Map<?,?> forceSignOut(String agentId, List<String> chatIdlist);
	Map<?,?> answer(String agentId, String chatId, String customerId, int ringType, int accessType);
	Map<?,?> innerTrans(String accessType, String agentId, String chatId, String targetId, int operType);
	Map<?,?> releaseCall(String agentId, String chatId, int ringType, int accessType);
	Map<?,?> listenChat(String agentId, String targetAgent, String targetChatId, String originalUserId);
	Map<?,?> insertChat(String agentId, String targetChatId, String targetAgent, String originalUserId);
	Map<?,?> interceptChat(String agentId, String targetChatId, String targetAgent);
	String whisperChat(String agentId, String targetChatId, String targetAgent);
	Map<?,?> forceReleaseCall(String agentId, String chatId, String currentAgent, int ringType);

	String getChatRelation(String chatId);
	String getChatList(String agentId);
	





}
