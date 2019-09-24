package com.asiainfo.cs.service;

import java.util.List;
import java.util.Map;

public interface IntelligenRouter {
	boolean setSkills(String agentId, String skills, String signId, String ip);
	boolean signIn(String agentId, String skills, String signId, String ip);
	Map<?,?> getUserName(String agentId);
	boolean signOut(String agentId);
	boolean answerSuccess(String agentId, String uniqueId, String chatId);
	boolean releaseCall(String agentId, String uniqueId, String chatId);
	boolean answerMore(String agentId);
	List<?> qryAuthorizedSkills(String agentId);
	List<?> qrySignedSkills(String agentId);
	public String doRoutingAction(Map<String, String> param);

}
