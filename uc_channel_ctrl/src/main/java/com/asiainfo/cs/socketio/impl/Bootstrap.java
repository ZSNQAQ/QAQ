package com.asiainfo.cs.socketio.impl;

import com.alibaba.fastjson.JSONObject;
import com.asiainfo.cs.common.util.*;
import com.asiainfo.cs.common.util.StaticValue.EAccessType;
import com.asiainfo.cs.common.util.StaticValue.ESeatState;
import com.asiainfo.cs.entity.OnlineCustomInfo;
import com.asiainfo.cs.log.entity.AgentStateLogModel;
import com.asiainfo.cs.online.service.OnlineChannelControl;
import com.asiainfo.cs.phone.service.PhoneChannelControl;
import com.asiainfo.cs.rabbit.RabbitmqSender;
import com.asiainfo.cs.service.IntelligenRouter;
import com.asiainfo.cs.service.SeatControl;
import com.asiainfo.cs.service.UserRedisCache;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;

@Component
public class Bootstrap 	implements ApplicationListener<ApplicationContextEvent> {

	private static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);
	@Autowired
	RedissonClient redissonClient;

	@Autowired
	private SocketIOServer server;

	@Autowired
	private UserRedisCache userRedisCache;
	@Autowired
	private RabbitmqSender rabbitmqSender;

	@Resource(name = "seat_list")
	Map<String, String> seat_list;



	@Autowired
	private SeatControl seat_cont;
	
	@Autowired
	PhoneChannelControl phoneControl;
	
	@Autowired
    OnlineChannelControl onlineControl;
	
	@Autowired
    IntelligenRouter intelligenRouter;
	
	@OnConnect  
	public void onConnect(SocketIOClient client) {
		LOG.debug("onConnect " + client.getSessionId().toString());
	}
	
	@OnDisconnect
	public void onDisconnect(SocketIOClient client) {
		LOG.debug("onDisconnect " + client.getSessionId().toString()+"  SeatName:"+client.get(StaticValue.SeatName));
		String user = client.get(StaticValue.SeatName);
		if (user != null) {
			seat_cont.logout(client, user);
		}
	}
	
	@OnEvent(value = StaticValue.setSkills)
	public void setSkills(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("setSkills " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		//System.out.println("0000000000000user:"+user+"data:"+JSONObject.toJSONString(data));
		seat_cont.setSkills(client,user, data);
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", StaticValue.login));
	}
	@OnEvent(value = StaticValue.login)
	public void login(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("login1 " + client.getSessionId().toString());
		LOG.debug("login2 " + data.toString());
		seat_cont.login(client, data);
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", StaticValue.login));

	}

	@OnEvent(value = StaticValue.logout)
	public void logout(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("logout " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		seat_cont.logout(client, user);
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", StaticValue.logout));
	}

	
	@OnEvent(value = StaticValue.setSeatState)
	public void setSeatState(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("setSeatState " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		ESeatState state=ESeatState.valueOf(data.get(StaticValue.SeatState));
		if (state == null){
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "SeatState Is Null!"));
			return;
		}
		userRedisCache.setAgentState(user, state);
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", StaticValue.setSeatState));		
	}
	
	@OnEvent(value = StaticValue.setStateOnHangup)
	public void setStateOnHangup(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("setStateOnHangup " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		
		ESeatState state=ESeatState.valueOf(data.get(StaticValue.SeatState));
		if (state == null){
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "SeatState Is Null!"));
			return;
		}
		
		userRedisCache.setAgentStateOnHangup(user, state);
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK",StaticValue.setStateOnHangup));
	}
	
	
	@OnEvent(value = StaticValue.answerMore)
	public void answerMore(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("answerMore " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		seat_cont.answerMore(user);
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "answerMore"));
	}
	
	@OnEvent(value = StaticValue.answer)
	public void answer(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("answer " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		int acceptType= Integer.valueOf(data.get(StaticValue.AccessType));
		String sessionId=data.get(StaticValue.SessionId);
		if (acceptType==EAccessType.ACCESS_TYPE_TELE.value()){
			phoneControl.answer(user,sessionId,acceptType);
		}else{
			onlineControl.answer(user, sessionId,acceptType);
		}
		//日志输出
		String uuid = UUID.fromString(seat_list.get(user)).toString();
		AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
		agentStateLogModel.setEventName("CUSTOM");
		agentStateLogModel.setEventSubclass("AGENT::ANSWER");
		agentStateLogModel.setEventContent("应答操作");
		agentStateLogModel.setAgnetId(user);
		agentStateLogModel.setAgentSignId(uuid);
		agentStateLogModel.setChatId(sessionId);
		agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
		agentStateLogModel.setOperTime(System.currentTimeMillis());
//		rabbitmqSender.sendDataToQueue(agentStateLogModel);
		rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
		System.out.println("AGENT::ANSWER=========++++++++++"+ JSONObject.toJSONString(agentStateLogModel));
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "answer"));
	}
	@OnEvent(value = StaticValue.release)
	public void release(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("release " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		int acceptType= Integer.valueOf(data.get(StaticValue.AccessType));
		String sessionId=data.get(StaticValue.SessionId);
		if (acceptType==EAccessType.ACCESS_TYPE_TELE.value()){
			phoneControl.release(user,sessionId,acceptType);
		}else{
			onlineControl.release(user, sessionId,acceptType);
		}
        //获取挂机后示闲示忙状态
        ESeatState stateOnHangup =userRedisCache.getAgentStateOnHangup(user);
		if(!stateOnHangup.equals(ESeatState.keep)){
			userRedisCache.setAgentState(user, stateOnHangup);
		}
		//日志输出
		String uuid = UUID.fromString(seat_list.get(user)).toString();
		AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
		agentStateLogModel.setEventName("CUSTOM");
		agentStateLogModel.setEventSubclass("AGENT::HANGUP");
		agentStateLogModel.setEventContent("坐席点击挂机");
		agentStateLogModel.setAgnetId(user);
		agentStateLogModel.setAgentSignId(uuid);
		agentStateLogModel.setChatId(sessionId);
		agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
		agentStateLogModel.setOperTime(System.currentTimeMillis());
//		rabbitmqSender.sendDataToQueue(agentStateLogModel);
		rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
		System.out.println("AGENT::HANGUP=========++++++++++"+ JSONObject.toJSONString(agentStateLogModel));
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "release"));
	}
	
	@OnEvent(value = StaticValue.cancel)
	public void cancel(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("cancel " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		int acceptType= Integer.valueOf(data.get(StaticValue.AccessType));
		String sessionId=data.get(StaticValue.SessionId);
		if (acceptType==EAccessType.ACCESS_TYPE_TELE.value()){
			phoneControl.cancel(user,sessionId);
		}else{
			onlineControl.cancel(user, sessionId);
		}
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "release"));
	}
	
	@OnEvent(value = StaticValue.transferToSeat)
	public void transferToSeat(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("transferToSeat " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}

		String sessionId=data.get(StaticValue.SessionId);
		String target=data.get(StaticValue.Target);
		String type = data.get(StaticValue.Type);
		int acceptType= Integer.valueOf(data.get(StaticValue.AccessType));
		if (acceptType==EAccessType.ACCESS_TYPE_TELE.value()){
			phoneControl.transferToSeat(String.valueOf(acceptType),user,sessionId, target, type);
		}else{
			onlineControl.transferToSeat(String.valueOf(acceptType),user,sessionId, target, type);
		}
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "transferToSeat"));
	}
	@OnEvent(value = StaticValue.transferToAuto)
	public void transferToAuto(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("transferToAuto " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		String sessionId=data.get(StaticValue.SessionId);
		String target=data.get(StaticValue.Target);
		String type = data.get(StaticValue.Type);
		int acceptType= Integer.valueOf(data.get(StaticValue.AccessType));
		if (acceptType==EAccessType.ACCESS_TYPE_TELE.value()){
			phoneControl.transferToAuto(user,sessionId, target, type);
		}else{
			onlineControl.transferToAuto(user,sessionId, target, type);
		}
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "transferToAuto"));
	}
	@OnEvent(value = StaticValue.transferToOutsite)
	public void transferToOutsite(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("transferToOutsite " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		String sessionId=data.get(StaticValue.SessionId);
		String target=data.get(StaticValue.Target);
		String type = data.get(StaticValue.Type);
		int acceptType= Integer.valueOf(data.get(StaticValue.AccessType));
		if (acceptType==EAccessType.ACCESS_TYPE_TELE.value()){
			phoneControl.transferToOutsite(user,sessionId, target, type);
		}else{
			onlineControl.transferToOutsite(user,sessionId, target, type);
		}
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "transferToAuto"));
	}

	@OnEvent(value = StaticValue.transferToQueue)
	public void transferToQueue(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("transferToQueue " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}

		String target=data.get(StaticValue.Target);
		String sessionId=data.get(StaticValue.SessionId);
		int acceptType= Integer.valueOf(data.get(StaticValue.AccessType));
		if (acceptType==EAccessType.ACCESS_TYPE_TELE.value()){
			phoneControl.transferToQueue(String.valueOf(acceptType),user,sessionId, target);
		}else{
			onlineControl.transferToQueue(String.valueOf(acceptType),user,sessionId, target);
		}
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "transferToQueue"));
	}
	@OnEvent(value = StaticValue.transferToGroupQueue)
	public void transferToGroupQueue(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("transferToGroupQueue " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}

		String target=data.get(StaticValue.Target);
		String sessionId=data.get(StaticValue.SessionId);
		int acceptType= Integer.valueOf(data.get(StaticValue.AccessType));
		if (acceptType==EAccessType.ACCESS_TYPE_TELE.value()){
			phoneControl.transferToQueue(String.valueOf(acceptType),user,sessionId, target);
		}else{
			onlineControl.transferToQueue(String.valueOf(acceptType),user,sessionId, target);
		}
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "transferToQueue"));
	}

	@OnEvent(value = StaticValue.helpToQueue)
	public void helpToQueue(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("helpToQueue " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested()) {
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));

			}
			return;
		}
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+ERR", "helpToSeat"));
	}
	
	@OnEvent(value = StaticValue.helpToSeat)
	public void helpToSeat(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("helpToSeat " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}

		String target=data.get(StaticValue.Target);
		String sessionId=data.get(StaticValue.SessionId);
		int acceptType= Integer.valueOf(data.get(StaticValue.AccessType));
		if (acceptType==EAccessType.ACCESS_TYPE_TELE.value()){
			phoneControl.helpToSeat(user,sessionId,  target);
		}else{
			onlineControl.helpToSeat(user,sessionId,  target);
		}
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "helpToSeat"));
	}
	@OnEvent(value = StaticValue.helpToOutsite)
	public void helpToOutsite(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("helpToOutsite " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}

		String target=data.get(StaticValue.Target);
		String sessionId=data.get(StaticValue.SessionId);
		int acceptType= Integer.valueOf(data.get(StaticValue.AccessType));
		if (acceptType==EAccessType.ACCESS_TYPE_TELE.value()){
			phoneControl.helpToOutsite(user,sessionId,  target);
		}else{
			onlineControl.helpToOutsite(user,sessionId,  target);
		}
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "helpToOutsite"));
	}
	
	@OnEvent(value = StaticValue.callSeat)
	public void callSeat(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("callSeat " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		
		String target=data.get(StaticValue.Target);
		int acceptType= Integer.valueOf(data.get(StaticValue.AccessType));
		if (acceptType==EAccessType.ACCESS_TYPE_TELE.value()){
			phoneControl.callSeat(user, target);
		}else{
			onlineControl.callSeat(user, target);
		}
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("-ERR", "callSeat"));
	}
	@OnEvent(value = StaticValue.callOutsite)
	public void callOutsite(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("callOutsite " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}

		String target=data.get(StaticValue.Target);
		int acceptType= Integer.valueOf(data.get(StaticValue.AccessType));
		if (acceptType==EAccessType.ACCESS_TYPE_TELE.value()){
			phoneControl.callOutsite(user, target);
		}else{
			onlineControl.callOutsite(user, target);
		}
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "callOutsite"));
	}
	@OnEvent(value = StaticValue.agentSendInfo)
	public void agentSendInfo(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("agentSendInfo " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		String acceptType=data.get(StaticValue.AccessType);
		String sessionId=data.get(StaticValue.SessionId);
		String info=data.get(StaticValue.Info);
		phoneControl.sendDTMF(user,sessionId,info);
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "agentSendDTMF"));
	}
	@OnEvent(value = StaticValue.listen)
	public void listen(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("listen " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		int acceptType= Integer.valueOf(data.get(StaticValue.AccessType));
		String target=data.get(StaticValue.Target);
		String targetSessionId=data.get(StaticValue.TargetSessionId);
		//被监听的用户
		String originalUserId=data.get(StaticValue.OriginalUserId);
		if (acceptType==EAccessType.ACCESS_TYPE_TELE.value()){
			phoneControl.listen(user, target,targetSessionId,originalUserId);
		}else{
			onlineControl.listen(user, target,targetSessionId,originalUserId);
		}
		//日志输出
		String uuid = UUID.fromString(seat_list.get(user)).toString();
		String targetUUID = userRedisCache.getAgentUUID(target).toString();
		AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
		agentStateLogModel.setEventName("CUSTOM");
		agentStateLogModel.setEventSubclass("AGENT::LISTEN");
		agentStateLogModel.setEventContent("监听");
		agentStateLogModel.setAgnetId(user);
		agentStateLogModel.setAgentSignId(uuid);
		agentStateLogModel.setTargetId(target);
		agentStateLogModel.setTargetSignId(targetUUID);
		agentStateLogModel.setTargetChatId(targetSessionId);
		agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
		agentStateLogModel.setOperTime(System.currentTimeMillis());
//		rabbitmqSender.sendDataToQueue(agentStateLogModel);
		rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
		//System.out.println("AGENT::LISTEN=========++++++++++"+ JSONObject.toJSONString(agentStateLogModel));
		
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "listen"));		
	}
	@OnEvent(value = StaticValue.whisper)
	public void whisper(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("whisper " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		int acceptType= Integer.valueOf(data.get(StaticValue.AccessType));
		String sessionId=data.get(StaticValue.SessionId);
		if (acceptType==EAccessType.ACCESS_TYPE_TELE.value()){
			phoneControl.whisper(user,sessionId);
		}else{
			onlineControl.whisper(user,sessionId);
		}
		//日志输出
		String target=data.get(StaticValue.Target);
		String uuid = UUID.fromString(seat_list.get(user)).toString();
		String targetUUID = userRedisCache.getAgentUUID(target).toString();
		AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
		agentStateLogModel.setEventName("CUSTOM");
		agentStateLogModel.setEventSubclass("AGENT::WHISPER");
		agentStateLogModel.setEventContent("耳语");
		agentStateLogModel.setAgnetId(user);
		agentStateLogModel.setAgentSignId(uuid);
		agentStateLogModel.setTargetId(target);
		agentStateLogModel.setTargetSignId(targetUUID);
		agentStateLogModel.setTargetChatId(sessionId);
		agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
		agentStateLogModel.setOperTime(System.currentTimeMillis());
//		rabbitmqSender.sendDataToQueue(agentStateLogModel);
		rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
		System.out.println("AGENT::WHISPER=========++++++++++"+ JSONObject.toJSONString(agentStateLogModel));

		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "insert"));	
	}
	
	@OnEvent(value = StaticValue.insert)
	public void insert(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("insert " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		
		
		int acceptType= Integer.valueOf(data.get(StaticValue.AccessType));
		String sessionId=data.get(StaticValue.SessionId);
		String target=data.get(StaticValue.Target);
		//被质检插话的用户
		String originalUserId=data.get(StaticValue.OriginalUserId);
		if (acceptType==EAccessType.ACCESS_TYPE_TELE.value()){
			phoneControl.insert(user,sessionId,null,null);
		}else{
			onlineControl.insert(user,sessionId,target,originalUserId);
		}
		//日志输出
		String uuid = UUID.fromString(seat_list.get(user)).toString();
		String targetUUID = userRedisCache.getAgentUUID(target).toString();
		AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
		agentStateLogModel.setEventName("CUSTOM");
		agentStateLogModel.setEventSubclass("AGENT::INSERT");
		agentStateLogModel.setEventContent("插入");
		agentStateLogModel.setAgnetId(user);
		agentStateLogModel.setAgentSignId(uuid);
		agentStateLogModel.setTargetId(target);
		agentStateLogModel.setTargetSignId(targetUUID);
		agentStateLogModel.setTargetChatId(sessionId);
		agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
		agentStateLogModel.setOperTime(System.currentTimeMillis());
//		rabbitmqSender.sendDataToQueue(agentStateLogModel);
		rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
		System.out.println("AGENT::INSERT=========++++++++++"+ JSONObject.toJSONString(agentStateLogModel));
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "insert"));
	}
	
	@OnEvent(value = StaticValue.intercept)
	public void intercept(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("intercept " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		int acceptType= Integer.valueOf(data.get(StaticValue.AccessType));
		String target=data.get(StaticValue.Target);
		String targetSessionId=data.get(StaticValue.TargetSessionId);
		if (acceptType==EAccessType.ACCESS_TYPE_TELE.value()){
			phoneControl.intercept(user,target,targetSessionId);
		}else{
			onlineControl.intercept(user,target,targetSessionId);
		}
		//日志输出
		String uuid = UUID.fromString(seat_list.get(user)).toString();
		String targetUUID = userRedisCache.getAgentUUID(target).toString();
		AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
		agentStateLogModel.setEventName("CUSTOM");
		agentStateLogModel.setEventSubclass("AGENT::INTERCEPT");
		agentStateLogModel.setEventContent("拦截");
		agentStateLogModel.setAgnetId(user);
		agentStateLogModel.setAgentSignId(uuid);
		agentStateLogModel.setTargetId(target);
		agentStateLogModel.setTargetSignId(targetUUID);
		agentStateLogModel.setTargetChatId(targetSessionId);
		agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
		agentStateLogModel.setOperTime(System.currentTimeMillis());
//		rabbitmqSender.sendDataToQueue(agentStateLogModel);
		rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
		System.out.println("AGENT::INTERCEPT=========++++++++++"+ JSONObject.toJSONString(agentStateLogModel));
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "intercept"));		
	}

	
	@OnEvent(value = StaticValue.hold)
	public void hold(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("hold " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		int acceptType= Integer.valueOf(data.get(StaticValue.AccessType));
		String sessionId=data.get(StaticValue.SessionId);
		if (acceptType==EAccessType.ACCESS_TYPE_TELE.value()){
			phoneControl.hold(user,sessionId, "1".equals(data.get(StaticValue.Type))?true:false);
		}
		
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "hold"));		
	}
	
	
	@OnEvent(value = StaticValue.forceRelease)
	public void forceRelease(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("forceRelease " + client.getSessionId().toString());
		String currentAgent = client.get(StaticValue.SeatName);
		if (currentAgent == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}

		String target=data.get(StaticValue.Target);
		String targetSessionId=data.get(StaticValue.TargetSessionId);				
		int acceptType= Integer.valueOf(data.get(StaticValue.AccessType));
		if (acceptType==EAccessType.ACCESS_TYPE_TELE.value()){
			phoneControl.forceRelease(target,targetSessionId, currentAgent);
		}else{
			onlineControl.forceRelease(target, targetSessionId, currentAgent);
		}
		//日志输出
		UUID uuid = UUID.fromString(seat_list.get(currentAgent));
		UUID userUUID = userRedisCache.getAgentUUID(target);
		AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
		agentStateLogModel.setEventName("CUSTOM");
		agentStateLogModel.setEventSubclass("AGENT::FORCEHANGUP");
		agentStateLogModel.setResult(StaticValue.RESULT_SUCCESS);
		agentStateLogModel.setEventContent("强制挂机操作");
		agentStateLogModel.setAgnetId(currentAgent);
		agentStateLogModel.setAgentSignId(uuid.toString());
		agentStateLogModel.setTargetId(target);
		agentStateLogModel.setTargetSignId(userUUID.toString());
		agentStateLogModel.setTargetChatId(targetSessionId);
		agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
		agentStateLogModel.setOperTime(System.currentTimeMillis());
//		rabbitmqSender.sendDataToQueue(agentStateLogModel);
		rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
		System.out.println("AGENT::FORCEHANGUP=========++++++++++"+ JSONObject.toJSONString(agentStateLogModel));
		
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "forceRelease"));
	}
	
	@OnEvent(value = StaticValue.forceSignOut)
	public void forceSignOut(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("forceSignOut " + client.getSessionId().toString());
		LOG.debug("forceSignOut " + JSONObject.toJSONString(data));
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		//通知FS坐席签出
		phoneControl.forceSignOut(data.get(StaticValue.Target), user);
		//通知nocs坐席签出
		onlineControl.forceSignOut(data.get(StaticValue.Target), user);

		//通知排队侧坐席签出
		UUID userUUID = userRedisCache.getAgentUUID(data.get(StaticValue.Target));
		SocketIOClient targetClient = server.getClient(userUUID);
		seat_cont.logout(targetClient, data.get(StaticValue.Target));
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "forceSignOut"));
	}
	
	@OnEvent(value = StaticValue.forceSayFree)
	public void forceSayFree(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("forceSayFree " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		phoneControl.forceSayFree(data.get(StaticValue.Target), user);
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "forceSayFree"));
	}
	
	
	@OnEvent(value = StaticValue.forceSayBusy)
	public void forceSayBusy(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("forceSayBusy " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		phoneControl.forceSayBusy(data.get(StaticValue.Target), user);
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "forceSayBusy"));
	}
	
	
	

	@OnEvent(value = StaticValue.getOnlineSeat)
	public void getOnlineSeat(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("getOnlineSeat " + client.getSessionId().toString());
		
		if (ackSender.isAckRequested())
				ackSender.sendAckData(userRedisCache.getOnlineAgent());
	}
	
	
	@OnEvent(value = StaticValue.getSeatInfo)
	public void getSeatInfo(SocketIOClient client, AckRequest ackSender, StringList data) throws Exception {
		LOG.debug("getSeatInfo " + client.getSessionId().toString());
		List<Map<String,String>> result=new LinkedList<Map<String,String>>();
		for(Object value:data){
			Map<String,String> info=userRedisCache.getAgentInfo((String)value);
			if (null!=info)
				result.add(info);
		}
		if (ackSender.isAckRequested())
				ackSender.sendAckData(result);
	}
	@OnEvent(value = StaticValue.getOnlineSeatInfo)
	public void getOnlineSeatInfo(SocketIOClient client, AckRequest ackSender, StringList data) throws Exception {
		LOG.debug("getOnlineSeatInfo " + client.getSessionId().toString());
		Set<String> onlineSeat = userRedisCache.getOnlineAgent();
		List<Map<String,String>> result=new LinkedList<>();

		for(String seatId:onlineSeat){
			Map<String,String> info=userRedisCache.getAgentInfo(seatId);
			if(null!=info){
				Map<String,String> simpleInfo=new HashMap<>();
				simpleInfo.put(StaticValue.SeatState,info.get(StaticValue.SeatState));
				simpleInfo.put(StaticValue.SeatName,info.get(StaticValue.SeatName));
				simpleInfo.put(StaticValue.AgentIP,info.get(StaticValue.AgentIP));
				simpleInfo.put(StaticValue.AgentLoginName,info.get(StaticValue.AgentLoginName));
				result.add(simpleInfo);
			}
		}
		if (ackSender.isAckRequested())
			ackSender.sendAckData(result);
	}
	@OnEvent(value = StaticValue.getSeatChatInfo)
	public void getSeatChatInfo(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		List<EventParam> list = new ArrayList<>();
		String agentId = data.get(StaticValue.SeatName);
		if (!StaticValue.ESeatMediaState.IDLE.equals(userRedisCache.getAgentAudioState(agentId))){
			EventParam eventParam = new EventParam(agentId, EAccessType.ACCESS_TYPE_TELE);
			eventParam.setSessionId(userRedisCache.getSessionId(agentId));
			eventParam.setOriginalUserId(userRedisCache.getUserChannId(agentId));
			eventParam.setUserAccount(userRedisCache.getUserName(agentId));
			eventParam.setUserName(userRedisCache.getUserName(agentId));
			eventParam.setMediaState(userRedisCache.getAgentAudioState(agentId));
			eventParam.setRingType(userRedisCache.getRingType(agentId));
			list.add(eventParam);
		}
		Map<String,String> info=userRedisCache.getAgentInfo(agentId);
		for(Map.Entry<String,String> entry:info.entrySet()){
			if(entry.getKey().startsWith("ONLINE:")) {
				OnlineCustomInfo customInfo = OnlineCustomInfo.createFromJson(entry.getValue());
				EventParam eventParam = new EventParam(customInfo.getAgentId(), customInfo.getAccessType());
				eventParam.setSessionId(customInfo.getSessionId());
				eventParam.setOriginalUserId(customInfo.getOriginalId());
				eventParam.setMediaState(customInfo.getMediaState());
				eventParam.setRingType(customInfo.getRingType());
				eventParam.setUserAccount(customInfo.getUserAccount());
				list.add(eventParam);
			}
		}
		if (ackSender.isAckRequested())
			ackSender.sendAckData(list);
	}
	
	
	
	@OnEvent(value = StaticValue.setFollowData)
	public void setFollowData(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		//Key AccessType
		//Key SessionId
		//Key Data
		LOG.debug("setFollowData " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		String acceptType=data.get(StaticValue.AccessType);
		String sessionId=data.get(StaticValue.SessionId);
		String value=data.get(StaticValue.Data);
		phoneControl.setFollowData(user,sessionId,value);
		if (ackSender.isAckRequested())
			ackSender.sendAckData(new ResultObject("+OK", "setFollowData"));
	}
	
	
	
	@OnEvent(value = StaticValue.getFollowData)
	public void getFollowData(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("setFollowData " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		
		if (ackSender.isAckRequested()){
			String acceptType=data.get(StaticValue.AccessType);
			String sessionId=data.get(StaticValue.SessionId);
			ackSender.sendAckData(phoneControl.getFollowData(user,sessionId));
		}
	}
	
	

	@OnEvent(value = StaticValue.getChannelData)
	public void getChannelData(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("setFollowData " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		if (ackSender.isAckRequested()){
			String acceptType=data.get(StaticValue.AccessType);
			String sessionId=data.get(StaticValue.SessionId);
			String key=data.get(StaticValue.Key);
			ackSender.sendAckData(phoneControl.getChannelData(user,sessionId,key));
		}
	}
	
	@OnEvent(value = StaticValue.setChannelData)
	public void setChannelData(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("setFollowData " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			if (ackSender.isAckRequested())
				ackSender.sendAckData(new ResultObject("-ERR", "The user has not checked in!"));
			return;
		}
		String acceptType=data.get(StaticValue.AccessType);
		String sessionId=data.get(StaticValue.SessionId);
		String key=data.get(StaticValue.Key);
		String value=data.get(StaticValue.Value);
		phoneControl.setChannelData(user,sessionId,key, value);
		if (ackSender.isAckRequested()){
			ackSender.sendAckData(new ResultObject("+OK", "setChannelData"));
		}
	}
	@OnEvent(value = StaticValue.doRoutingAction)
	public void doRoutingAction(SocketIOClient client, AckRequest ackSender, StringMap data) throws Exception {
		LOG.debug("doRoutingAction " + client.getSessionId().toString());
		String user = client.get(StaticValue.SeatName);
		if (user == null) {
			return;
		}
		ackSender.sendAckData(intelligenRouter.doRoutingAction(data));
	}
	
	@OnEvent(value = "qryAuthorizedSkills")
	public void qryAuthorizedSkills(SocketIOClient client, AckRequest ackSender, StringMap data){
		String agentId = data.get(StaticValue.SeatName);
		ackSender.sendAckData(intelligenRouter.qryAuthorizedSkills(agentId));
	}
	
	@OnEvent(value = "qrySignedSkills")
	public void qrySignedSkills(SocketIOClient client, AckRequest ackSender, StringMap data){
		String agentId = data.get(StaticValue.SeatName);
		ackSender.sendAckData(intelligenRouter.qrySignedSkills(agentId));
	}
	@PostConstruct
	public void start() {

	}
	
	@PreDestroy
	public void stop() {
	
	}

	@Override
	public void onApplicationEvent(ApplicationContextEvent event) {
		LOG.info("ApplicationEvent:"+event.getClass().getName());
		if (event instanceof ContextStartedEvent){
			server.start();
		}else if (event instanceof ContextStoppedEvent){
			server.stop();
		}
		
	}

}
