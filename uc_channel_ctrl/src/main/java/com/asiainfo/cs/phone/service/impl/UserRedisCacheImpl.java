package com.asiainfo.cs.phone.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.asiainfo.cs.common.util.EventParam;
import com.asiainfo.cs.common.util.StaticValue;
import com.asiainfo.cs.common.util.StaticValue.*;
import com.asiainfo.cs.entity.OnlineCustomInfo;
import com.asiainfo.cs.entity.PhoneCustomInfo;
import com.asiainfo.cs.log.CommonLog;
import com.asiainfo.cs.log.entity.AgentStateLogModel;
import com.asiainfo.cs.log.entity.CommonLogEntity;
import com.asiainfo.cs.rabbit.RabbitmqSender;
import com.asiainfo.cs.service.UserRedisCache;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.client.protocol.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserRedisCacheImpl implements UserRedisCache {
	private static final Logger LOG = LoggerFactory.getLogger(UserRedisCacheImpl.class);
	private static final String prefixAgents="cc:agents:";
	private static final String prefixStateGroup="cc:stategroup:";
	private static final String onlinekey="cc:agent:online";
	private static final String voicekey="cc:agent:voice";

	@Resource
    CommonLog commonlog;
	
	@Autowired
	private RabbitmqSender rabbitmqSender;

	@Resource
	private SocketIOServer server;

	@Resource(name = "monitor_list")
	private RMap<String, String> monitor_list;
	
	@Resource(name="service_uuid")
	String service_uuid;
	
	@Resource(name="redisson")
	private Redisson redisson;
	
	
	private Map<String, RMap<String, String>> online=new HashMap<String, RMap<String, String>>();
	
	private RSet<String> agent_online=null;
	private RSet<String> agent_voice=null;
	private RSet<String> stategroup_free=null;
	private RSet<String> stategroup_acw=null;
//	private RSet<String> stategroup_ring=null;
	private RSet<String> stategroup_busy=null;

	@PostConstruct
	private void init(){
		agent_online = redisson.getSet(onlinekey);
		agent_voice = redisson.getSet(voicekey);
		stategroup_free = redisson.getSet(prefixStateGroup+"free");
		stategroup_acw = redisson.getSet(prefixStateGroup+"acw");
//		stategroup_ring = redisson.getSet(prefixStateGroup+"ring");
		stategroup_busy = redisson.getSet(prefixStateGroup+"busy");
	}

	@Override
	public boolean loginAgentInfo(String user) {
		if (agentExists(user))
			return false;
//		if (online.containsKey(user))
//			return false;
		RMap<String,String> map=redisson.getMap(prefixAgents+user);
		map.clear();
		map.put(StaticValue.LoginTimestamp, String.valueOf( System.currentTimeMillis()/1000L));
		map.put(StaticValue.ServiceUUID, service_uuid);
		online.put(user, map);
		agent_online.add(user);
		agent_voice.add(user);
		return false;
		
	}
	
	@Override
	public void cleanAgentInfo(String user) {
		monitor_list.remove(user);
		RMap<String, String> map=online.remove(user);
		if (null!=map){
			map.delete();
			agent_online.remove(user);
			agent_voice.remove(user);
			stategroup_free.remove(user);
			stategroup_acw.remove(user);
			stategroup_busy.remove(user);

		}
	
	}

	
	private void setUserCustomInfo(String user,String key,String value,boolean exists,boolean expire){

		if (online.containsKey(user)&&online.get(user).isExists()){
			RMap<String,String> map=online.get(user);
			if (null==value){
				map.remove(key);
			}else{
				map.put(key, value);
			}
		}else{
			if (redisson.getKeys().isExists(prefixAgents+user)>0){
				if (null==value){
					redisson.getMap(prefixAgents+user).remove(key);
				}else{
					redisson.getMap(prefixAgents+user).put(key, value);
				}
				
			}
		}
		if(expire){
			setAgentInfoExpire(user);
		}

	}
	
	@Override
	public  void setAgentCustomInfo(String user, String key, String value ) {
		 setUserCustomInfo(user,key,value,true,false);
	}


	@Override
	public String getAgentCustomInfo(String user, String key) {
		if (online.containsKey(user)){
			RMap<String,String> map=online.get(user);
			return map.get(key);
		}else{
			
			LOG.warn("NOT IN LOCAL "+user+" = = " + key);
			return (String)redisson.getMap(prefixAgents+user).get(key);
		}
	}

	
	@Override
	public String setAgentState(String user, ESeatState state) {
		if (online.containsKey(user)){
			RMap<String,String> map=online.get(user);
			String result=map.get(StaticValue.SeatState);
			map.put(StaticValue.SeatState, state.name());
			stategroup_free.remove(user);
			stategroup_acw.remove(user);
//			stategroup_ring.remove(user);
			stategroup_busy.remove(user);
			switch (state) {
			case free:
				stategroup_free.add(user);
				break;
			case acw:
				stategroup_acw.add(user);
				break;
//			case ring:
//				stategroup_ring.add(user);
//				break;
			case busy:
				stategroup_busy.add(user);
				break;
			}
			
			UUID uuid=getAgentUUID(user);
			SocketIOClient client=server.getClient(uuid);
			if (client!=null){
				EventParam eventParam=new EventParam(user, EAccessType.ACCESS_TYPE_TELE);
				eventParam.setEventDesc("状态改变");
				eventParam.setSeatState(state);
				client.sendEvent(StaticValue.onSeatStateChange, eventParam);
				
				CommonLogEntity entity=new CommonLogEntity();
				entity.setSignId(uuid.toString());
				entity.setAgentId(user);
				entity.setBusiType(StaticValue.LOG_SET_STATE);
				entity.setRemark1(state.name());
				commonlog.log(entity);
				//日志输出
				AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
				agentStateLogModel.setEventName("CUSTOM");
				agentStateLogModel.setEventSubclass("AGENT::STATE");
				agentStateLogModel.setEventCode(StaticValue.LOG_SET_STATE);
				agentStateLogModel.setResult(StaticValue.RESULT_SUCCESS);
				agentStateLogModel.setEventContent("状态改变成功："+state);
				agentStateLogModel.setAgnetId(user);
				agentStateLogModel.setAgentSignId(client.getSessionId().toString());
				switch (state) {
					case free:
						agentStateLogModel.setParam("FREE");
						break;
					case busy:
						agentStateLogModel.setParam("BUSY");
						break;
				}
				agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
				agentStateLogModel.setOperTime(System.currentTimeMillis());
//				rabbitmqSender.sendDataToQueue(agentStateLogModel);
				rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
				System.out.println("状态改变成功："+JSONObject.toJSONString(agentStateLogModel));

				
			}
			return result;
			
		}else
		{
			return null;
		}

	}


	
	@Override
	public ESeatState getAgentState(String user) {
		ESeatState result=null;
		try {
			result= ESeatState.valueOf(getAgentCustomInfo(user, StaticValue.SeatState).toLowerCase());
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return result;
	}

	
	@Override
	public void setAgentStateOnHangup(String user, ESeatState state) {
		setUserCustomInfo(user, StaticValue.StateOnHangup, state.toString(),false,true);
	}

	
	
	@Override
	public void setAgentInfoExpire(String user) {
		redisson.getCommandExecutor().writeAsync(prefixAgents+user, RedisCommands.PEXPIRE, prefixAgents+user, TimeUnit.SECONDS.toMillis(15));
	}
	

	@Override
	public ESeatState getAgentStateOnHangup(String user) {
		return  ESeatState.valueOf(getAgentCustomInfo(user, StaticValue.StateOnHangup));
	}

	
	@Override
	public void setAgentUUID(String user, UUID uuid) {
		setUserCustomInfo(user, StaticValue.SeatUUID, uuid.toString(),false,true);
		setUserCustomInfo(user, StaticValue.SeatName, user,false,true);
	}

	@Override
	public UUID getAgentUUID(String user) {
		String result=getAgentCustomInfo(user, StaticValue.SeatUUID);
		if (result==null) 
			return null;
		else
			return UUID.fromString(result);
	}


	@Override
	public void setAgentServiceUUID(String seat, String value) {
		 setAgentCustomInfo(seat, StaticValue.ServiceUUID,value);
	}

	@Override
	public String getAgentServiceUUID(String seat) {
		return getAgentCustomInfo(seat, StaticValue.ServiceUUID);
	}



	@Override
	public void setAgentAudioState(String seat, ESeatMediaState value) {

		if (online.containsKey(seat)){

			if (ESeatMediaState.IDLE.equals(value)){
				agent_voice.remove(seat);
			}else{
				agent_voice.add(seat);
			}
			ESeatMediaState old=getAgentAudioState(seat);
			setUserCustomInfo(seat, StaticValue.SeatAudioState, value.name(),true,false);
			if (value==old) return;
			UUID uuid=getAgentUUID(seat);
			SocketIOClient client=server.getClient(uuid);
			if (client!=null){
				String session_id=getSessionId(seat);
				EActionType firstAction=getRingType(seat);
				String requesterName=getUserName(seat);
				String unique_id=getUserChannId(seat);
				EventParam eventParam=new EventParam(seat, EAccessType.ACCESS_TYPE_TELE);
				eventParam.setAccessTypeDetail("");
				eventParam.setRingType(firstAction);
				eventParam.setUserAccount(requesterName);
				eventParam.setUserName(requesterName);
				eventParam.setMediaState(value);
				eventParam.setEventDesc("媒体状态改变2");
				eventParam.setSessionId(session_id);
//				if (ESeatMediaState.RING.equals(value)){
//					client.sendEvent(StaticValue.onCreateChannel,eventParam);
//				}
//				if (ESeatMediaState.IDLE.equals(value)){
//					client.sendEvent(StaticValue.onReleaseChannel,eventParam);
//				}
				client.sendEvent(StaticValue.onMediaStateChange, eventParam);

				switch (value) {
				case RING:{
					//日志输出
					AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
					agentStateLogModel.setEventName("CUSTOM");
					agentStateLogModel.setEventSubclass("AGENT::RING");
					agentStateLogModel.setEventContent("电话振铃");
					agentStateLogModel.setAgnetId(seat);
					agentStateLogModel.setChatId(session_id);
					agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
					agentStateLogModel.setOperTime(System.currentTimeMillis());
					rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
					System.out.println("AGENT::RING=========++++++++++"+ JSONObject.toJSONString(agentStateLogModel));

					CommonLogEntity entity=new CommonLogEntity();
					entity.setUniqueId(unique_id);
					entity.setSessionId(session_id);
					entity.setSignId(uuid.toString());
					entity.setAgentId(seat);
					entity.setRemark1(String.valueOf(EAccessType.ACCESS_TYPE_TELE.value()));
					entity.setRemark2(String.valueOf(firstAction.value()));
					entity.setBusiType(StaticValue.LOG_RINGING);
					commonlog.log(entity);
					break;
				}
				case ANSWER:{
					CommonLogEntity entity=new CommonLogEntity();
					entity.setUniqueId(unique_id);
					entity.setSessionId(session_id);
					entity.setSignId(uuid.toString());
					entity.setAgentId(seat);
					entity.setRemark1(String.valueOf(EAccessType.ACCESS_TYPE_TELE.value()));
					entity.setRemark2(String.valueOf(firstAction.value()));
					entity.setBusiType(StaticValue.LOG_DEVICE_ANSWER);
					commonlog.log(entity);
					break;
				}
				case TALK:{
					CommonLogEntity entity=new CommonLogEntity();
					entity.setUniqueId(unique_id);
					entity.setSessionId(session_id);
					entity.setSignId(uuid.toString());
					entity.setAgentId(seat);
					entity.setRemark1(String.valueOf(EAccessType.ACCESS_TYPE_TELE.value()));
					entity.setRemark2(String.valueOf(firstAction.value()));
					entity.setBusiType(StaticValue.LOG_DEVICE_TALKING);
					commonlog.log(entity);
					break;
				}
				case WAITRET:{
					CommonLogEntity entity=new CommonLogEntity();
					entity.setUniqueId(unique_id);
					entity.setSessionId(session_id);
					entity.setSignId(uuid.toString());
					entity.setAgentId(seat);
					entity.setRemark1(String.valueOf(EAccessType.ACCESS_TYPE_TELE.value()));
					entity.setRemark2(String.valueOf(firstAction.value()));
					entity.setBusiType(StaticValue.LOG_DEVICE_WAITRET);
					commonlog.log(entity);
					break;
				}
				case IDLE:{
					CommonLogEntity entity=new CommonLogEntity();
					entity.setUniqueId(unique_id);
					entity.setSessionId(session_id);
					entity.setSignId(uuid.toString());
					entity.setAgentId(seat);
					entity.setRemark1(String.valueOf(EAccessType.ACCESS_TYPE_TELE.value()));
					entity.setRemark2(String.valueOf(firstAction.value()));
					entity.setBusiType(StaticValue.LOG_DEVICE_RELEASE);
					commonlog.log(entity);
					break;
				}
				default:
					break;
				}
			}
		}
	}
	
	
	@Override
	public boolean agentExists(String seat) {
		return redisson.getKeys().isExists(prefixAgents+seat)>0;
		
//		if (redisson.getKeys().isExists(prefixAgents+seat)>0){
//			String uuid=(String)redisson.getMap(prefixAgents+seat).get(UserDataCache.ServiceUUID);
//			if (null!=uuid&&!uuid.isEmpty()&&redisson.getKeys().isExists("cc:seat_list:"+uuid)>0){
//				return redisson.getMap("cc:seat_list:"+uuid).containsKey(seat);
//			}else{
//				return true;
//			}
//		} else return false;
			
	}
	
	
	@Override
	public Map<String, String> getAgentInfo(String seat) {
		if (online.containsKey(seat))
			return online.get(seat);
		else
			return redisson.getMap(prefixAgents+seat);
	}
	


	@Override
	public void setAgentChannId(String seat,String value){
		setUserCustomInfo(seat, StaticValue.ChannelUUID, value,true,false);
	}
	@Override
	public void setAgentChannHost(String seat,String value){
		setUserCustomInfo(seat, StaticValue.ChannelHost, value,true,false);
	}
	

	@Override
	public String getAgentChannId(String user) {
		return getAgentCustomInfo(user, StaticValue.ChannelUUID);
	}

	//huangqb add for OriginalNumber 2019-4-15
	@Override
	public String getOriginalNumber(String user) {
		return getAgentCustomInfo(user, StaticValue.OriginalNumber);
	}
	@Override
	public void setOriginalNumber(String seat,String value){
		setUserCustomInfo(seat, StaticValue.OriginalNumber, value,true,false);
	}
	//end of huangqb add 2019-4-15

	@Override
	public String getAgentChannHost(String user) {
		return getAgentCustomInfo(user, StaticValue.ChannelHost);
	}


	
	@Override
	public void setAgentOnlineUUID(String user, String value) {
		setUserCustomInfo(user, StaticValue.OnlineUUID, value,false,true);
		
	}

	@Override
	public String getAgentOnlineUUID(String user) {
		return getAgentCustomInfo(user, StaticValue.OnlineUUID);
	}
	

	@Override
	public void setAgentChannelName(String user, String value) {
		setUserCustomInfo(user, StaticValue.ChannelName, value,false,true);
		
	}

	@Override
	public String getAgentChannelName(String user) {
		return getAgentCustomInfo(user, StaticValue.ChannelName);
	}

	@Override
	public void setAgentIP(String user, String value) {
		setUserCustomInfo(user, StaticValue.AgentIP, value,false,true);
	}

	@Override
	public void setAgentLoginName(String user, String value) {
		setUserCustomInfo(user, StaticValue.AgentLoginName, value,false,true);
	}

	@Override
	public String getAgentIP(String user) {
		return getAgentCustomInfo(user, StaticValue.AgentIP);
	}

	@Override
	public void setUserChannId(String user, String value) {
		setUserCustomInfo(user, StaticValue.RequesterChannelUUID, value,true,false);
		
	}

	@Override
	public String getUserChannId(String user) {
		return getAgentCustomInfo(user, StaticValue.RequesterChannelUUID);
	}

	
	@Override
	public void setUserChannHost(String user, String value) {
		setUserCustomInfo(user, StaticValue.RequesterHostName, value,true,false);
		
	}

	@Override
	public String getUserChannHost(String user) {
		return getAgentCustomInfo(user, StaticValue.RequesterHostName);
	}
	


	

	@Override
	public ESeatMediaState getAgentAudioState(String seat) {
		String result=getAgentCustomInfo(seat, StaticValue.SeatAudioState);
		if (result==null)
			return ESeatMediaState.IDLE;
		else{
			return ESeatMediaState.valueOf(result);
		}
	}

	@Override
	public void setRingType(String seat, EActionType value) {
		if (value.equals(getRingType(seat))){
			return; 
		}
		setUserCustomInfo(seat, StaticValue.SeatFirstAction, value.name(),true,false);
		
	}

	@Override
	public EActionType getRingType(String seat) {
		String result=getAgentCustomInfo(seat, StaticValue.SeatFirstAction);
		if (result==null)
			return null;
		else{
			return EActionType.valueOf(result);
		}
	}


	@Override
	public void setAgentRequestType(String seat, EActionType value) {
		EActionType old= getAgentRequestType(seat);
		if (value.equals(old)){
				return; 
		}
		if(EActionType.NONE.equals(value)&&old!=null){
			EventParam eventParam=new EventParam(seat, EAccessType.ACCESS_TYPE_TELE);
			eventParam.setRingType(old);
			eventParam.setSessionId(getSessionId(seat));
			eventParam.setTargetId(getTargetName(seat));
			UUID uuid=getAgentUUID(seat);
			SocketIOClient client=server.getClient(uuid);
			if (client!=null){
				client.sendEvent(StaticValue.onRequestCancel, eventParam);
			}
		}
		setUserCustomInfo(seat, StaticValue.SeatSecondAction, value.name(),true,false);
		
	}

	@Override
	public EActionType getAgentRequestType(String seat) {
		String result=getAgentCustomInfo(seat, StaticValue.SeatSecondAction);
		if (result==null)
			return null;
		else{
			return EActionType.valueOf(result);
		}
	}
	
	
	@Override
	public void setAgentRequestId(String seat, String value) {
		setAgentCustomInfo(seat, StaticValue.SeatRequestId,value);
		
	}

	@Override
	public String getAgentRequestId(String seat) {
		return getAgentCustomInfo(seat, StaticValue.SeatRequestId);
	}

	@Override
	public void setUserName(String seat, String value) {
		setAgentCustomInfo(seat, StaticValue.RequesterName,value);
		
	}

	@Override
	public String getUserName(String seat) {
		return getAgentCustomInfo(seat, StaticValue.RequesterName);
	}

	
	@Override
	public void setTargetChannId(String user, String value) {
		setUserCustomInfo(user, StaticValue.TargetChannelUUID, value,true,false);
		
	}
	
	@Override
	public String getTargetChannId(String user) {
		return getAgentCustomInfo(user, StaticValue.TargetChannelUUID);
	}

	
	@Override
	public void setTargetHost(String seat, String value) {
		setUserCustomInfo(seat, StaticValue.TargetHostName, value,true,false);
		
	}

	@Override
	public String getTargetHost(String seat) {
		return getAgentCustomInfo(seat, StaticValue.TargetHostName);
	}

	@Override
	public void setTargetName(String seat, String value) {
		setUserCustomInfo(seat, StaticValue.TargetName, value,true,false);
		
	}

	@Override
	public String getTargetName(String seat) {
		return getAgentCustomInfo(seat, StaticValue.TargetName);
	}

	@Override
	public void setSourceChannId(String seat, String value) {
		setUserCustomInfo(seat, StaticValue.SourceChannelUUID, value,true,false);
		
	}

	@Override
	public String getSourceChannId(String seat) {
		return getAgentCustomInfo(seat, StaticValue.SourceChannelUUID);
	}

	@Override
	public void setSourceHost(String seat, String value) {
		setUserCustomInfo(seat, StaticValue.SourceHostName, value,true,false);
		
	}

	@Override
	public String getSourceHost(String seat) {
		return getAgentCustomInfo(seat, StaticValue.SourceHostName);
	}

	@Override
	public void setSourceName(String seat, String value) {
		setUserCustomInfo(seat, StaticValue.SourceName, value,true,false);
		
	}

	@Override
	public String getSourceName(String seat) {
		return getAgentCustomInfo(seat, StaticValue.SourceName);
	}

	@Override
	public void setSessionId(String seat, String value) {
		setUserCustomInfo(seat, StaticValue.SessionId, value,true,false);
		
	}

	@Override
	public String getSessionId(String seat) {
		return getAgentCustomInfo(seat, StaticValue.SessionId);
	}

	@Override
	public void setMonitorType(String seat, EMonitorType value) {
		setUserCustomInfo(seat, StaticValue.MonitorType, value.name(),true,false);
		
	}

	@Override
	public EMonitorType getMonitorType(String seat) {
		String result=getAgentCustomInfo(seat, StaticValue.MonitorType);
		if (result==null)
			return EMonitorType.NONE;
		else{
			return EMonitorType.valueOf(result);
		}
	}

	@Override
	public void setMonitorID(String seat, String value) {
		setUserCustomInfo(seat, StaticValue.MonitorID, value,true,false);
		
	}

	@Override
	public String getMonitorID(String seat) {
		return getAgentCustomInfo(seat, StaticValue.MonitorID);
	}



	@Override
	public Set<String> getOnlineAgent() {
		Set<String> result=new HashSet<String>();
		//for (String key:server_list.readAllKeySet()){
		for(String key:redisson.getKeys().getKeysByPattern("cc:seat_list:*")){
			//redisson.getKeys().getKeysByPattern("cc:seat_list:");
			Map<String,String > map=redisson.getMap(key);
			LOG.debug(key+" Count "+map.size());
			result.addAll(map.keySet());
		}
		LOG.debug("Online Count "+result.size());
		return result;
	}

	@Override
	public OnlineCustomInfo getInternetRequest(String seat, String sessionId) {
		String jsonStr=getAgentCustomInfo(seat,"ONLINE:"+sessionId);
		return OnlineCustomInfo.createFromJson(jsonStr);
	}

	@Override
	public void setInternetRequest(String seat, String sessionId, OnlineCustomInfo value) {
		if (null==value){
			 setUserCustomInfo(seat,"ONLINE:"+sessionId,null,true,false);
		}else{
			 setUserCustomInfo(seat,"ONLINE:"+sessionId, value.toString(),true,false);
		}

	}

	@Override
	public PhoneCustomInfo getPhoneRequest(String seat, String sessionId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPhoneRequest(String seat, String sessionId, PhoneCustomInfo value) {
		if (null==value){
			 setUserCustomInfo(seat,"_ONLINE:"+sessionId,null,true,false);
		}else{
			 setUserCustomInfo(seat,"_ONLINE:"+sessionId, value.toString(),true,false);
		}

	}








}
