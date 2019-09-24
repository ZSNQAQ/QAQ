package com.asiainfo.cs.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.asiainfo.cs.common.util.EventParam;
import com.asiainfo.cs.common.util.StaticValue;
import com.asiainfo.cs.common.util.StaticValue.EAccessType;
import com.asiainfo.cs.common.util.StaticValue.EActionType;
import com.asiainfo.cs.common.util.StaticValue.ESeatMediaState;
import com.asiainfo.cs.common.util.StaticValue.ESeatState;
import com.asiainfo.cs.log.CommonLog;
import com.asiainfo.cs.log.entity.AgentStateLogModel;
import com.asiainfo.cs.log.entity.CommonLogEntity;
import com.asiainfo.cs.online.service.OnlineCustomService;
import com.asiainfo.cs.phone.entity.EventForCC;
import com.asiainfo.cs.rabbit.RabbitmqSender;
import com.asiainfo.cs.service.IntelligenRouter;
import com.asiainfo.cs.service.SeatControl;
import com.asiainfo.cs.service.UserRedisCache;
import com.corundumstudio.socketio.SocketIOClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Component
public class SeatControlImpl implements SeatControl {
	private static final Logger LOG = LoggerFactory.getLogger(SeatControlImpl.class);
	@Value("${sys.seatChannelName:user/%s}")
	private String seatChannelName;
	
	@Resource(name="seat_list")
	Map<String,String>  seat_list;

	@Autowired
	private UserRedisCache userRedisCache;

	@Autowired
	private IntelligenRouter router;

	@Autowired
	private OnlineCustomService onlineService;

	@Autowired
	private RabbitmqSender rabbitmqSender;

	@Resource
	CommonLog commonlog;

	@Override
	public void setSkills(SocketIOClient client, String seat,Map<String, String> data) {
		String signId=client.getSessionId().toString();
		String skills=data.get(StaticValue.SignedSkills);
		ESeatState stateOnRelease=ESeatState.valueOf(data.get(StaticValue.stateOnRelease));
		userRedisCache.setAgentStateOnHangup(seat,stateOnRelease);
		String ip=client.getHandshakeData().getHttpHeaders().get("X-Forwarded-For");
		EventParam eventParam=new EventParam(seat,EAccessType.ACCESS_TYPE_AGENT);
		if (router.setSkills(seat,skills,signId,ip)){
			client.set(StaticValue.SeatName, seat);
			eventParam.setEventDesc("设置技能成功");
			eventParam.setValue(ip);
			client.sendEvent(StaticValue.onSetSkillsSuccess,eventParam);
		}else{
			eventParam.setEventDesc("重新设置技能失败");
			client.sendEvent(StaticValue.onSetSkillsFail,eventParam);
		}
	}

	@Override
	public void login(SocketIOClient client, Map<String,String> data) {
		String signId=client.getSessionId().toString();
		String seat=data.get(StaticValue.SeatName);
		
		String skills=data.get(StaticValue.SignedSkills);
		String state=data.get(StaticValue.SeatState);
		ESeatState stateOnHungup=ESeatState.valueOf(data.get(StaticValue.stateOnRelease));
		String ip=client.getHandshakeData().getHttpHeaders().get("X-Forwarded-For");
		if (ip==null||ip.isEmpty()){
			ip=client.getHandshakeData().getAddress().getAddress().getHostAddress();
		}
		String channelName=data.get(StaticValue.ChannelName);
//		String channelName="sofia/external/"+seat+"@"+ip;
//		if (data.containsKey(StaticValue.ChannelName)){
//			channelName=data.get(StaticValue.ChannelName);
//		}

		String online=data.get(StaticValue.OnlineUUID);
		
		LOG.debug("Login:"+seat+"@["+channelName+"&" +online+"]" +client.getSessionId().toString());
		
		
		EventParam eventParam=new EventParam(seat,EAccessType.ACCESS_TYPE_AGENT);

		if (client.has(StaticValue.SeatName) 
				&&!client.get(StaticValue.SeatName).equals(seat)){
			eventParam.setEventDesc("当前链接已经登陆，请先签出或重新连接！");
			client.sendEvent(StaticValue.onLoginFail,eventParam);
			return;
		}
		if (client.has(StaticValue.SeatName)) {
			
			if (data.containsKey(StaticValue.ChannelName)){
				userRedisCache.setAgentChannelName(seat,channelName);
			}
			if (data.containsKey(StaticValue.OnlineUUID)){
				userRedisCache.setAgentOnlineUUID(seat,online);
				onlineService.signIn(seat, online,signId);
			}
			eventParam.setEventDesc(ip);
			client.sendEvent(StaticValue.onLoginSuccess,eventParam);
			return;
		}
		



		if(userRedisCache.agentExists(seat)){
			eventParam.setEventDesc("坐席"+seat+"已在"+userRedisCache.getAgentIP(seat)+"签入!");
			client.sendEvent(StaticValue.onLoginFail,eventParam);
			return;
		}

		userRedisCache.loginAgentInfo(seat);
//		userRedisCache.setAgentStateOnHangup(seat,ESeatState.free);
		userRedisCache.setAgentStateOnHangup(seat,stateOnHungup);
		userRedisCache.setAgentUUID(seat, client.getSessionId());
		userRedisCache.setAgentIP(seat,ip);
		String agentLoginName = data.get("StaffId");
		userRedisCache.setAgentLoginName(seat,agentLoginName);
		
		if (data.containsKey(StaticValue.ChannelName)){
			userRedisCache.setAgentChannelName(seat,channelName);
		}
		if (data.containsKey(StaticValue.OnlineUUID)){
			userRedisCache.setAgentOnlineUUID(seat,online);
		}
		
		
		userRedisCache.setAgentAudioState(seat, ESeatMediaState.IDLE);
		userRedisCache.setRingType(seat, EActionType.NONE);
		userRedisCache.setAgentRequestType(seat, EActionType.NONE);

		LOG.debug("Login========"+ client.get(StaticValue.SeatName)+" "+client.getSessionId().toString());
		
		userRedisCache.setAgentInfoExpire(seat);
		if (router.signIn(seat,skills,signId,ip)){
			
			userRedisCache.setAgentState(seat, ESeatState.valueOf(state.toLowerCase()));
			seat_list.put(seat,client.getSessionId().toString());
			
		}else{
			eventParam.setEventDesc("签入到排队系统系统失败");
			client.sendEvent(StaticValue.onLoginFail,eventParam);
			userRedisCache.cleanAgentInfo(seat);
			seat_list.remove(seat);

			//日志输出
			AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
			agentStateLogModel.setEventName("CUSTOM");
			agentStateLogModel.setEventSubclass("AGENT::SIGIN");
			agentStateLogModel.setEventCode(StaticValue.LOG_SIGNIN);
			agentStateLogModel.setResult(StaticValue.RESULT_FAIL);
			agentStateLogModel.setEventContent("签入到排队系统系统失败");
			agentStateLogModel.setAgnetId(seat);
			agentStateLogModel.setAgentSignId(signId);
			agentStateLogModel.setIp(ip);
			agentStateLogModel.setSkillId(skills);
			agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
			agentStateLogModel.setOperTime(System.currentTimeMillis());
//			rabbitmqSender.sendDataToQueue(agentStateLogModel);
			rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
			return;
		}
		if (data.containsKey(StaticValue.OnlineUUID)){
			userRedisCache.setAgentOnlineUUID(seat,online);
			onlineService.signIn(seat, online,signId);
		}
		
		client.set(StaticValue.SeatName, seat);
		eventParam.setEventDesc("登陆成功");
		eventParam.setValue(ip);
		client.sendEvent(StaticValue.onLoginSuccess,eventParam);
		
		CommonLogEntity entity=new CommonLogEntity();
		entity.setSignId(signId);
		entity.setAgentId(seat);
		entity.setRemark1(ip);
		entity.setRemark2(skills);
		entity.setBusiType(StaticValue.LOG_SIGNIN);
		commonlog.log(entity);
	

		//日志输出
		AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
		agentStateLogModel.setEventName("CUSTOM");
		agentStateLogModel.setEventSubclass("AGENT::SIGNIN");
		agentStateLogModel.setResult(StaticValue.RESULT_SUCCESS);
		agentStateLogModel.setEventContent("签入成功");
		agentStateLogModel.setAgnetId(seat);
		agentStateLogModel.setAgentSignId(signId);
		agentStateLogModel.setIp(ip);
		agentStateLogModel.setSkillId(skills);
		agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
		agentStateLogModel.setOperTime(System.currentTimeMillis());
//		rabbitmqSender.sendDataToQueue(agentStateLogModel);

		rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
	}
	
	
	@Override
	public void logout(SocketIOClient client,String seat) {
		EventParam eventParam=new EventParam(seat,EAccessType.ACCESS_TYPE_AGENT);
		if (!client.has(StaticValue.SeatName)){
			eventParam.setEventDesc("坐席没有登录");
			client.sendEvent(StaticValue.onLogoutFail,eventParam);
			return;
		}
		
		CommonLogEntity entity=new CommonLogEntity();
		entity.setSignId(client.getSessionId().toString());
		entity.setAgentId(seat);
		entity.setBusiType(StaticValue.LOG_SIGNOUT);
		commonlog.log(entity);

		//日志输出
		AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
		agentStateLogModel.setEventName("CUSTOM");
		agentStateLogModel.setEventSubclass("AGENT::SIGNOUT");
		agentStateLogModel.setResult(StaticValue.RESULT_SUCCESS);
		agentStateLogModel.setEventContent("签出成功");
		agentStateLogModel.setAgnetId(seat);
		agentStateLogModel.setAgentSignId(client.getSessionId().toString());
		agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
		agentStateLogModel.setOperTime(System.currentTimeMillis());
//		rabbitmqSender.sendDataToQueue(agentStateLogModel);
		rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
		String user = (String) client.get(StaticValue.SeatName);
		client.del(StaticValue.SeatName);

		client.sendEvent(StaticValue.onLogoutSuccess,eventParam);

		router.signOut(user);
		EActionType first = userRedisCache.getRingType(user);
		if (EActionType.SERVICE.equals(first)) {
			String reqChannelHost = userRedisCache.getUserChannHost(user);
			String reqChannelUUID = userRedisCache.getUserChannId(user);
			String session_id = userRedisCache.getSessionId(user);
			String core_uuid = userRedisCache.getAgentServiceUUID(user);
//			EventCommandObject obj = new EventCommandObject();
//			obj.setHost(reqChannelHost);
//			obj.setUniqueId(reqChannelUUID);
//			obj.setEventName("CUSTOM");
//			obj.setEventSubClass("cc::resp");
//			obj.putHeader("response_type", "busy");
//			obj.putHeader("session_id", session_id);
//			eslSupportImpl.sendEventCommand(obj);
//			rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
			EventForCC obj = new EventForCC();
			obj.setEventName("CUSTOM");
			obj.setEventSubClass("cc::resp");
			obj.setHost(reqChannelHost);
			obj.setFreeSWITCH_Hostname(reqChannelHost);
			obj.setResponse_type("busy");
			obj.setUnique_id(reqChannelUUID);
			obj.setSession_id(session_id);
			rabbitmqSender.sendDataToCC(reqChannelHost,"cc::resp",reqChannelUUID,JSONObject.toJSONString(obj));

			
			
			
//			EventCommandObject objLogout = new EventCommandObject();
//			objLogout.setHost(reqChannelHost);
//			objLogout.setUniqueId(reqChannelUUID);
//			objLogout.setEventName("CUSTOM");
//			objLogout.setEventSubClass("cc::logout");
//			objLogout.putHeader(StaticValue.SeatName, seat);
//			eslSupportImpl.sendEventCommand(objLogout);
//			rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
			EventForCC object = new EventForCC();
			object.setEventName("CUSTOM");
			object.setEventSubClass("cc::logout");
			object.setHost(reqChannelHost);
			object.setFreeSWITCH_Hostname(reqChannelHost);
			object.setResponse_type("busy");
			object.setSeatName(seat);
			rabbitmqSender.sendDataToCC(reqChannelHost,"cc::logout",reqChannelUUID,JSONObject.toJSONString(object));
			router.releaseCall(user, reqChannelUUID, session_id);
		}

		userRedisCache.cleanAgentInfo(user);
		seat_list.remove(user);
		onlineService.signOut(user);
	}


	@Override
	public void answerMore(String agentId) {
		router.answerMore(agentId);
		
	}
}
