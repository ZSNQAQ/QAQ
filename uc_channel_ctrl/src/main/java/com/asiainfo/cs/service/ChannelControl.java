package com.asiainfo.cs.service;

public interface ChannelControl {

	void answer(String agentId, String sessionId, int accessType);//应答
	void release(String agentId, String sessionId, int accessType);//释放
	void cancel(String agentId, String sessionId);//释放


	//服务中进行的动作
	void transferToSeat(String accessType, String agentId, String sessionId, String targetId, String type);//转移到坐席
	void transferToAuto(String agentId, String sessionId, String targetId, String type);//转移到自动应答设备
	void transferToOutsite(String agentId, String sessionId, String targetId, String type);//转移到外部
	void transferToQueue(String acceptType, String agentId, String sessionId, String targetId);//转移到队列

	void helpToSeat(String agentId, String sessionId, String targetId);//求助到坐席
//	void helpToQueue(String agentId,String sessionId,String targetId);//求助到队列
	void helpToOutsite(String agentId, String sessionId, String targetId);//求助到外部

	void callSeat(String agentId, String targetId);//呼叫坐席
	void callOutsite(String agentId, String targetId);//呼叫外部设备



	void listen(String agentId, String targetId, String targetSessionId, String originalUserId);//监听
	void whisper(String agentId, String sessionId);//耳语
	void intercept(String agentId, String target, String sessionId);//拦截
	void insert(String agentId, String sessionId, String targetId, String originalUserId);//插入

	void forceRelease(String targetAgent, String targetSessionId, String currentAgent);	// 强制挂机
	void forceSignOut(String targetAgent, String currentAgent);	// 强制签出
	void signOut(String targetAgent, String currentAgent);	// 签出
}
