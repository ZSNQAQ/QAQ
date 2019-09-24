package com.asiainfo.cs.online.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.asiainfo.cs.common.util.*;
import com.asiainfo.cs.common.util.StaticValue.EAccessType;
import com.asiainfo.cs.common.util.StaticValue.EActionType;
import com.asiainfo.cs.common.util.StaticValue.ESeatMediaState;
import com.asiainfo.cs.common.util.StaticValue.ESeatState;
import com.asiainfo.cs.entity.OnlineCustomInfo;
import com.asiainfo.cs.log.CommonLog;
import com.asiainfo.cs.log.entity.CommonLogEntity;
import com.asiainfo.cs.online.service.OnlineChannelControl;
import com.asiainfo.cs.online.service.OnlineCustomNotify;
import com.asiainfo.cs.online.service.OnlineCustomService;
import com.asiainfo.cs.service.IntelligenRouter;
import com.asiainfo.cs.service.UserRedisCache;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component("onlineChannelControImpl")
public class OnlineChannelControlImpl implements OnlineChannelControl,OnlineCustomNotify {
	
	private static final Logger LOG = LoggerFactory.getLogger(OnlineChannelControlImpl.class);
	@Resource
	CommonLog commonlog;
	@Autowired
	private SocketIOServer server;
	@Autowired
	private UserRedisCache userRedisCache;
	@Autowired
	OnlineCustomService onlineService;
	@Autowired
	private IntelligenRouter router;

	public static final int AGENT_NONE                    = 0;
	public static final int AGENT_RING                    = 100;   //振铃			坐席振铃，应答，
	public static final int AGENT_LONG_NO_ANSWER          = 101;   //久未应答		取消坐席振铃
	public static final int AGENT_DISTRIBUTE_FAIL         = 102; 	 //分配失败
	public static final int AGENT_LINK_ESTABLISHED        = 103;	 //应答成功
	public static final int AGENT_LINK_TIMEOUT            = 104; 	 //连接超时		改为应答失败
	public static final int AGENT_CUSTOMER_RELEASE        = 105;   //客户挂机
	public static final int AGENT_OPPOSITE_TRANS_ROBOT    = 106;   //转接自助
	public static final int AGENT_CANCLE_RING             = 107; 	 //取消振铃
	public static final int AGENT_FORCE_BUSY              = 108; 	 //强制示忙
	public static final int AGENT_FORCE_FREE              = 109; 	 //强制示闲
	public static final int AGENT_ACCESS_TIMEOUT          = 110; 	 //客户接入超时
	public static final int AGENT_LISTEN_CHAT_OPPOSITE    = 111;   //监视对话被挂机
	public static final int AGENT_INNERTRANS_RING         = 112; 	 //转接振铃		与振铃合并
	public static final int AGENT_TRANS_TIMEOUT           = 114; 	 //转接超时		与转接失败合并
	public static final int AGENT_TRANS_SUCCESS           = 115; 	 //转接成功
	public static final int AGENT_TRANS_FAIL              = 116; 	 //转接失败
	public static final int AGENT_RELEASE         		  = 117; 	 //座席挂机
	public static final int INSPECTOR_NEWCHAT             = 118;   //被质检座席应答新对话
	public static final int INSPECTOR_SIGNOUT             = 119;   //被质检座席签出
	public static final int LONG_NO_SENDMSG_TIMEOUT       = 120; 	 //久未发送消息超时
	public static final int AGENT_SATE_LIMIT_FAIL         = 121;    //座席状态限制操作失败	
	public static final int PASSWORD_VERIFY               = 122;
	public static final int AGENT_INFO_PUSH               = 123;
	public static final int AGENT_FORCE_SIGNOUT        	 = 124;
	public static final int AGENTANSWER_NOTIFY_INSPECTORS         = 125;
	public static final int HELPCHAT_RELEASE         	 = 126;
	public static final int AGENT_FORCE_REALSE         	 = 128;

							
							
							
	private EventParam createOnlineEventParam(OnlineCustomInfo info){
		EventParam eventParam=new EventParam(info.getAgentId(),info.getAccessType());
		eventParam.setSessionId(info.getSessionId());
		eventParam.setOriginalUserId(info.getOriginalId());
		eventParam.setMediaState(info.getMediaState());
		eventParam.setRingType(info.getRingType());
		eventParam.setUserAccount(info.getUserAccount());
		eventParam.setUserName(info.getUserName());
		return eventParam;
	}
 
							

	@Override
	public void doCustomRequestEvent(OcsNotify packet) {

		System.out.println("doCustomRequestEvent++++++++++"+JSONObject.toJSONString(packet));
		String receiver=packet.getReceiver();
		if (!userRedisCache.agentExists(receiver)){
			// TODO 用户不存在
			return;
		}

		UUID uuid=userRedisCache.getAgentUUID(receiver);
		SocketIOClient client=server.getClient(uuid);
		if (null==client){
			return ;
		}
		
		String customId=packet.getOriginalUserId();
		String chatId=packet.getChatId();
		switch (packet.getNotifyCode()) {
		case AGENT_NONE:
			break;
		case AGENT_RING:{//振铃
			EActionType ringType=EActionType.valueOf(Integer.valueOf(packet.getProps().get("RING_TYPE")));
			EAccessType accessType=EAccessType.valueOf(Integer.valueOf(packet.getProps().get("ACCESS_TYPE")));
			String userAccount=packet.getProps().get("USER_ACCOUNT");
				String userName=packet.getProps().get("USER_NAME");
			OnlineCustomInfo info=new OnlineCustomInfo(receiver,customId,chatId);

			info.setMediaState(ESeatMediaState.RING);
			info.setUserAccount(userAccount);
			info.setUserName(userName);
			info.setRingType(ringType);
			info.setAccessType(accessType);
			info.getProps().putAll(packet.getProps());
			userRedisCache.setInternetRequest(receiver,chatId, info);	
			EventParam eventParam=createOnlineEventParam(info);
			client.sendEvent(StaticValue.onCreateChannel, eventParam);
			client.sendEvent(StaticValue.onMediaStateChange, eventParam);
			
//			CommonLogEntity entity=new CommonLogEntity();
//			entity.setUniqueId(customId);
//			entity.setSessionId(chatId);
//			entity.setSignId(uuid.toString());
//			entity.setAgentId(receiver);
//			entity.setRemark1(String.valueOf(accessType.value()));
//			entity.setRemark2(String.valueOf(ringType.value()));
//			entity.setBusiType(StaticValue.LOG_RINGING);
//			commonlog.log(entity);

			if (EActionType.MONITOR.equals(ringType)||EActionType.INSERT_CHAT.equals(ringType)){
				info.setMediaState(ESeatMediaState.TALK);
				info.setTargetId(userAccount);
				userRedisCache.setInternetRequest(receiver,chatId, info);
				client.sendEvent(StaticValue.onMediaStateChange, createOnlineEventParam(info));
			}else if (EActionType.INTERCEPT.equals(ringType)){
				info.setMediaState(ESeatMediaState.TALK);
				info.setTargetId(userAccount);
				userRedisCache.setInternetRequest(receiver,chatId, info);
				System.out.println("拦截振铃："+JSONObject.toJSONString(info));
				client.sendEvent(StaticValue.onMediaStateChange, createOnlineEventParam(info));
			}
			break;
		}
		case AGENT_LONG_NO_ANSWER:{//长时间无应答
			OnlineCustomInfo info=userRedisCache.getInternetRequest(receiver, chatId);
			if (null==info) break;
			userRedisCache.setInternetRequest(receiver,chatId,null);
			router.releaseCall(receiver, packet.getCustomerSessionId(), chatId);
			//获取挂机后示闲示忙状态
			ESeatState stateOnHangup =userRedisCache.getAgentStateOnHangup(receiver);
			if(!stateOnHangup.equals(ESeatState.keep)){
				userRedisCache.setAgentState(receiver, stateOnHangup);
			}
			
			info.setMediaState(ESeatMediaState.IDLE);

			EventParam eventParam=createOnlineEventParam(info);
			client.sendEvent(StaticValue.onMediaStateChange, eventParam);
			client.sendEvent(StaticValue.onReleaseChannel, eventParam);
			
			CommonLogEntity entity=new CommonLogEntity();
			entity.setUniqueId(customId);
			entity.setSessionId(chatId);
			entity.setSignId(uuid.toString());
			entity.setAgentId(receiver);
			entity.setRemark1(String.valueOf(info.getAccessType().value()));
			entity.setRemark2(String.valueOf(info.getRingType().value()));
			entity.setBusiType(StaticValue.LOG_DEVICE_RELEASE);
			commonlog.log(entity);
			
			break;
		}
		case AGENT_DISTRIBUTE_FAIL://无用
			break;
		case AGENT_LINK_ESTABLISHED:{//媒体连接成功
			OnlineCustomInfo info=userRedisCache.getInternetRequest(receiver, chatId);
			if (null==info) break;
			info.setMediaState(ESeatMediaState.TALK);
			if (EActionType.SERVICE.equals(info.getRingType())||
					EActionType.TRUNS_QUEUE.equals(info.getRingType())){
				//只有从队列进入的坐席才会通知排队
				router.answerSuccess(receiver, packet.getCustomerSessionId(), chatId);
			}
			userRedisCache.setInternetRequest(receiver,chatId, info);
			EventParam eventParam=createOnlineEventParam(info);
			client.sendEvent(StaticValue.onMediaStateChange, eventParam);
			
			CommonLogEntity entity=new CommonLogEntity();
			entity.setUniqueId(customId);
			entity.setSessionId(chatId);
			entity.setSignId(uuid.toString());
			entity.setAgentId(receiver);
			entity.setRemark1(String.valueOf(info.getAccessType().value()));
			entity.setRemark2(String.valueOf(info.getRingType().value()));
			entity.setBusiType(StaticValue.LOG_DEVICE_ANSWER);
			commonlog.log(entity);
			
			break;
		}
		case AGENT_LINK_TIMEOUT://无用
			break;
		case AGENT_CUSTOMER_RELEASE:{//用户释放
			OnlineCustomInfo info=userRedisCache.getInternetRequest(receiver, chatId);
			if (null==info) break;
			//获取挂机后示闲示忙状态
			ESeatState stateOnHangup =userRedisCache.getAgentStateOnHangup(receiver);
			if(!stateOnHangup.equals(ESeatState.keep)){
				userRedisCache.setAgentState(receiver, stateOnHangup);
			}
			userRedisCache.setInternetRequest(receiver,chatId,null);
			if (EActionType.SERVICE.equals(info.getRingType())||
					EActionType.TRUNS_QUEUE.equals(info.getRingType())){
				router.releaseCall(receiver, packet.getCustomerSessionId(), chatId);
			}
			info.setMediaState(ESeatMediaState.IDLE);
			EventParam eventParam=createOnlineEventParam(info);
			client.sendEvent(StaticValue.onMediaStateChange, eventParam);
			client.sendEvent(StaticValue.onReleaseChannel, eventParam);
			
			CommonLogEntity entity=new CommonLogEntity();
			entity.setUniqueId(customId);
			entity.setSessionId(chatId);
			entity.setSignId(uuid.toString());
			entity.setAgentId(receiver);
			entity.setRemark1(String.valueOf(info.getAccessType().value()));
			entity.setRemark2(String.valueOf(info.getRingType().value()));
			entity.setBusiType(StaticValue.LOG_DEVICE_RELEASE);
			commonlog.log(entity);
			
			break;
		}
		case AGENT_OPPOSITE_TRANS_ROBOT://无用
			break;
		case AGENT_CANCLE_RING:{//取消振铃
			OnlineCustomInfo info=userRedisCache.getInternetRequest(receiver, chatId);
			if (null==info) break;
			userRedisCache.setInternetRequest(receiver,chatId,null);
			if (EActionType.SERVICE.equals(info.getRingType())||
					EActionType.TRUNS_QUEUE.equals(info.getRingType())){
				router.releaseCall(receiver, packet.getCustomerSessionId(), chatId);
			}
			info.setMediaState(ESeatMediaState.IDLE);
			EventParam eventParam=createOnlineEventParam(info);
			client.sendEvent(StaticValue.onMediaStateChange, eventParam);
			client.sendEvent(StaticValue.onReleaseChannel, eventParam);
			
			CommonLogEntity entity=new CommonLogEntity();
			entity.setUniqueId(customId);
			entity.setSessionId(chatId);
			entity.setSignId(uuid.toString());
			entity.setAgentId(receiver);
			entity.setRemark1(String.valueOf(info.getAccessType().value()));
			entity.setRemark2(String.valueOf(info.getRingType().value()));
			entity.setBusiType(StaticValue.LOG_DEVICE_RELEASE);
			commonlog.log(entity);
			
			break;
		}
		case AGENT_FORCE_BUSY://强制示忙
			break;
		case AGENT_FORCE_FREE://强制示闲
			break;
		case AGENT_ACCESS_TIMEOUT://分配超时
			break;
		case AGENT_LISTEN_CHAT_OPPOSITE://无用
			break;
		case AGENT_INNERTRANS_RING://无用
			break;
		case AGENT_TRANS_TIMEOUT: {//转接超时
			OnlineCustomInfo info = userRedisCache.getInternetRequest(receiver, chatId);
			if (null == info) break;
			info.setMediaState(ESeatMediaState.IDLE);
			EventParam eventParam = createOnlineEventParam(info);
			eventParam.setEventDesc("转接分配超时，请稍后再试");
			client.sendEvent(StaticValue.onTransToQueueFail, eventParam);
			break;
		}
		case AGENT_TRANS_SUCCESS:{//转接成功
			OnlineCustomInfo info=userRedisCache.getInternetRequest(receiver, chatId);
			if (null==info) break;
			userRedisCache.setInternetRequest(receiver,chatId,null);
			if (EActionType.SERVICE.equals(info.getRingType())||
					EActionType.TRUNS_QUEUE.equals(info.getRingType())){
				router.releaseCall(receiver, packet.getCustomerSessionId(), chatId);
			}
			info.setMediaState(ESeatMediaState.IDLE);
			EventParam eventParam=createOnlineEventParam(info);
			if ( EActionType.TRUNS_QUEUE.equals(info.getRingType())){
				client.sendEvent(StaticValue.onTransToQueueSuccess, eventParam);		
			}else if ( EActionType.TRUNS_SEAT.equals(info.getRingType())){
				client.sendEvent(StaticValue.onTransToSeatSuccess, eventParam);	
			}

			client.sendEvent(StaticValue.onMediaStateChange, eventParam);
			client.sendEvent(StaticValue.onReleaseChannel, eventParam);
			
			CommonLogEntity entity=new CommonLogEntity();
			entity.setUniqueId(customId);
			entity.setSessionId(chatId);
			entity.setSignId(uuid.toString());
			entity.setAgentId(receiver);
			entity.setRemark1(String.valueOf(info.getAccessType().value()));
			entity.setRemark2(String.valueOf(info.getRingType().value()));
			entity.setBusiType(StaticValue.LOG_DEVICE_RELEASE);
			commonlog.log(entity);
			
			break;
		}
		case AGENT_TRANS_FAIL:{//转接释放
			OnlineCustomInfo info=userRedisCache.getInternetRequest(receiver, chatId);
			if (null==info) break;
			EventParam eventParam=createOnlineEventParam(info);
			if ( EActionType.TRUNS_QUEUE.equals(info.getRingType())){
				client.sendEvent(StaticValue.onTransToQueueFail, eventParam);		
			}else if ( EActionType.TRUNS_SEAT.equals(info.getRingType())){
				client.sendEvent(StaticValue.onTransToSeatFail, eventParam);	
			}
			break;
		}

		case AGENT_RELEASE:{//坐席释放
			System.out.println("ssss=========="+JSONObject.toJSONString(packet));
			OnlineCustomInfo info=userRedisCache.getInternetRequest(receiver, chatId);
			if (null==info) break;
			userRedisCache.setInternetRequest(receiver,chatId,null);
			if (EActionType.SERVICE.equals(info.getRingType())||
					EActionType.TRUNS_QUEUE.equals(info.getRingType())){
				router.releaseCall(receiver, packet.getCustomerSessionId(), chatId);
			}
			//获取挂机后示闲示忙状态
			ESeatState stateOnHangup =userRedisCache.getAgentStateOnHangup(receiver);
			if(!stateOnHangup.equals(ESeatState.keep)){
				userRedisCache.setAgentState(receiver, stateOnHangup);
			}
			info.setMediaState(ESeatMediaState.IDLE);
			EventParam eventParam=createOnlineEventParam(info);
			client.sendEvent(StaticValue.onMediaStateChange, eventParam);
			client.sendEvent(StaticValue.onReleaseChannel, eventParam);
			
			CommonLogEntity entity=new CommonLogEntity();
			entity.setUniqueId(customId);
			entity.setSessionId(chatId);
			entity.setSignId(uuid.toString());
			entity.setAgentId(receiver);
			entity.setRemark1(String.valueOf(info.getAccessType().value()));
			entity.setRemark2(String.valueOf(info.getRingType().value()));
			entity.setBusiType(StaticValue.LOG_DEVICE_RELEASE);
			commonlog.log(entity);
			
			break;
		}
		case INSPECTOR_NEWCHAT:
			break;
		case INSPECTOR_SIGNOUT:
			break;
			case LONG_NO_SENDMSG_TIMEOUT: {//坐席长时间无消息
			
				OnlineCustomInfo info = userRedisCache.getInternetRequest(receiver, chatId);
			
				if (null == info) break;
				userRedisCache.setInternetRequest(receiver, chatId, null);
				if (EActionType.SERVICE.equals(info.getRingType()) ||
						EActionType.TRUNS_QUEUE.equals(info.getRingType())) {
					router.releaseCall(receiver, packet.getCustomerSessionId(), chatId);
				}
				//获取挂机后示闲示忙状态
				ESeatState stateOnHangup =userRedisCache.getAgentStateOnHangup(receiver);
				if(!stateOnHangup.equals(ESeatState.keep)){
					userRedisCache.setAgentState(receiver, stateOnHangup);
				}
				info.setMediaState(ESeatMediaState.IDLE);
				EventParam eventParam = createOnlineEventParam(info);
			
				client.sendEvent(StaticValue.onMediaStateChange, eventParam);
				client.sendEvent(StaticValue.onReleaseChannel, eventParam);

				CommonLogEntity entity = new CommonLogEntity();
				entity.setUniqueId(customId);
				entity.setSessionId(chatId);
				entity.setSignId(uuid.toString());
				entity.setAgentId(receiver);
				entity.setRemark1(String.valueOf(info.getAccessType().value()));
				entity.setRemark2(String.valueOf(info.getRingType().value()));
				entity.setBusiType(StaticValue.LOG_DEVICE_RELEASE);
				commonlog.log(entity);
				break;
			}
		case AGENT_SATE_LIMIT_FAIL://
			break;
		case PASSWORD_VERIFY://无用
			break;
		case AGENT_INFO_PUSH:
			break;
		case AGENT_FORCE_SIGNOUT://强制签出
			break;
			case AGENT_FORCE_REALSE: {//强制挂机
				OnlineCustomInfo info = userRedisCache.getInternetRequest(receiver, chatId);
				if (null == info) break;
				userRedisCache.setInternetRequest(receiver, chatId, null);
				if (EActionType.SERVICE.equals(info.getRingType()) ||
						EActionType.TRUNS_QUEUE.equals(info.getRingType())) {
					router.releaseCall(receiver, packet.getCustomerSessionId(), chatId);
				}
				//获取挂机后示闲示忙状态
				ESeatState stateOnHangup =userRedisCache.getAgentStateOnHangup(receiver);
				if(!stateOnHangup.equals(ESeatState.keep)){
					userRedisCache.setAgentState(receiver, stateOnHangup);
				}
				info.setMediaState(ESeatMediaState.IDLE);
				EventParam eventParam = createOnlineEventParam(info);
				client.sendEvent(StaticValue.onMediaStateChange, eventParam);
				client.sendEvent(StaticValue.onReleaseChannel, eventParam);
				break;
			}
		case AGENTANSWER_NOTIFY_INSPECTORS://
			break;
		case HELPCHAT_RELEASE://无用
			break;
		default:
			break;
		}
		
	}


	@Override
	public void answer(String agentId, String sessionId,int acceptType) {
		UUID uuid=userRedisCache.getAgentUUID(agentId);
		SocketIOClient client=server.getClient(uuid);
		if (null==client){
			return ;
		}
		
		OnlineCustomInfo info=userRedisCache.getInternetRequest(agentId, sessionId);
		if(null==info){
			EventParam tmp=new EventParam(agentId,EAccessType.ACCESS_TYPE_AGENT);
			tmp.setEventDesc("Session错误");
			client.sendEvent(StaticValue.onAnswerFail, tmp);
		}
		EventParam eventParam=createOnlineEventParam(info);
		
//		StringMap result=PublicUtils.getAppFrameResult(onlineService.answer(agentId, sessionId, info.getOriginalId(), info.getRingType().value()));
		Map<?,?> result =onlineService.answer(agentId, sessionId, info.getUserAccount(), info.getRingType().value(),acceptType);
		if ("1".equals(result.get(StaticValue.RSLTVAL))){
			client.sendEvent(StaticValue.onAnswerSuccess, eventParam);
		}else{
			eventParam.setEventDesc((String) result.get(StaticValue.RSLTMSG));
			client.sendEvent(StaticValue.onAnswerFail, eventParam);
		}
		
	}


	@Override
	public void release(String agentId, String sessionId,int accessType) {
		UUID uuid=userRedisCache.getAgentUUID(agentId);
		SocketIOClient client=server.getClient(uuid);
		if (null==client){
			return ;
		}
		
		OnlineCustomInfo info=userRedisCache.getInternetRequest(agentId, sessionId);
		if(null==info){
			EventParam tmp=new EventParam(agentId,EAccessType.ACCESS_TYPE_AGENT);
			tmp.setEventDesc("Session错误");
			client.sendEvent(StaticValue.onReleaseFail, tmp);
		}
		EventParam eventParam=createOnlineEventParam(info);
		
		Map<?,?> result =onlineService.releaseCall(agentId, sessionId, info.getRingType().value(),accessType);
		if ("1".equals(result.get(StaticValue.RSLTVAL))){
			if (!StringUtils.isEmpty(result.get("qualityType"))&& 2==(Integer) result.get("qualityType")){//2是拦截状态
				String targetId = (String) result.get("targetId");
				String chatUUID = (String) result.get("chatUUID");
				UUID targetUuid=userRedisCache.getAgentUUID(targetId);
				SocketIOClient targetClient=server.getClient(targetUuid);
				OnlineCustomInfo targetInfo=userRedisCache.getInternetRequest(targetId, chatUUID);
				if(null==info){
					EventParam tmp=new EventParam(targetId,EAccessType.ACCESS_TYPE_AGENT);
					tmp.setEventDesc("Session错误");
					client.sendEvent(StaticValue.onAnswerFail, tmp);
				}
				EventParam eventParam1=createOnlineEventParam(targetInfo);
				eventParam1.setSessionId(chatUUID);
				targetClient.sendEvent("onChangeChatState", eventParam1);
			}
			client.sendEvent(StaticValue.onReleaseSuccess, eventParam);
//			router.releaseCall(agentId, info.getOriginalUserId(), sessionId);
		}else{
			eventParam.setEventDesc((String) result.get(StaticValue.RSLTMSG));
			client.sendEvent(StaticValue.onReleaseFail, eventParam);
		}

	}

//	public static final int ENQUEUE_TYPE_QUEUE_TRANS = 2;   //转接队列
//	public static final int ENQUEUE_TYPE_AGENT_TRANS = 3;   //转接座席

	@Override
	public void transferToSeat(String accessType,String agentId, String sessionId, String targetId, String type) {
		UUID uuid=userRedisCache.getAgentUUID(agentId);
		SocketIOClient client=server.getClient(uuid);
		if (null==client){
			return ;
		}
		OnlineCustomInfo info=userRedisCache.getInternetRequest(agentId, sessionId);
		if(null==info){
			EventParam tmp=new EventParam(agentId,EAccessType.ACCESS_TYPE_AGENT);
			tmp.setEventDesc("Session错误");
			client.sendEvent(StaticValue.onTransToSeatFail, tmp);
		}
		EventParam eventParam=createOnlineEventParam(info);
//		StringMap result=PublicUtils.getAppFrameResult(onlineService.innerTrans(agentId, sessionId, targetId, 3));
		Map<?,?> result=onlineService.innerTrans(accessType,agentId, sessionId, targetId, 3);
		if ("1".equals(result.get(StaticValue.RSLTVAL))){
			client.sendEvent(StaticValue.onTransToSeatProcess, eventParam);
		}else{
			eventParam.setEventDesc((String) result.get(StaticValue.RSLTMSG));
			client.sendEvent(StaticValue.onTransToSeatFail, eventParam);
		}
		
	}
	@Override
	public void transferToQueue(String acceptType,String agentId, String sessionId, String targetId) {
		UUID uuid=userRedisCache.getAgentUUID(agentId);
		SocketIOClient client=server.getClient(uuid);
		if (null==client){
			return ;
		}
		OnlineCustomInfo info=userRedisCache.getInternetRequest(agentId, sessionId);
		if(null==info){
			EventParam tmp=new EventParam(agentId,EAccessType.ACCESS_TYPE_AGENT);
			tmp.setEventDesc("Session错误");
			client.sendEvent(StaticValue.onTransToQueueFail, tmp);
		}
		EventParam eventParam=createOnlineEventParam(info);
//		StringMap result=PublicUtils.getAppFrameResult(onlineService.innerTrans(agentId, sessionId,targetId, 2));
		Map<?,?> result=onlineService.innerTrans(acceptType,agentId, sessionId,targetId, 2);
		if ("1".equals(result.get(StaticValue.RSLTVAL))){
			client.sendEvent(StaticValue.onTransToQueueProcess, eventParam);
		}else{
			eventParam.setEventDesc((String) result.get(StaticValue.RSLTMSG));
			client.sendEvent(StaticValue.onTransToQueueFail, eventParam);
		}
		
	}


	@Override
	public void transferToAuto(String agentId, String sessionId, String targetId, String type) {
		
	}


	@Override
	public void transferToOutsite(String agentId, String sessionId, String targetId, String type) {
		
	}




	@Override
	public void helpToSeat(String agentId, String sessionId, String targetId) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void helpToOutsite(String agentId, String sessionId, String targetId) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void callSeat(String agentId, String targetId) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void callOutsite(String agentId, String targetId) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void listen(String agentId, String targetId, String targetSessionId,String originalUserId) {
		UUID uuid=userRedisCache.getAgentUUID(agentId);
		SocketIOClient client=server.getClient(uuid);
		if (null==client){
			return ;
		}
		OnlineCustomInfo targetInfo=userRedisCache.getInternetRequest(targetId, targetSessionId);
		if(null==targetInfo){
			EventParam tmp=new EventParam(agentId,EAccessType.ACCESS_TYPE_AGENT);
			tmp.setEventDesc("Session错误");
			client.sendEvent(StaticValue.onListenFail, tmp);
		}
		
		OnlineCustomInfo info=userRedisCache.getInternetRequest(agentId, targetSessionId);
		
		if	(null!=info
				&&!EActionType.MONITOR.equals(info.getRingType())){
			EventParam tmp=new EventParam(agentId,EAccessType.ACCESS_TYPE_AGENT);
			tmp.setEventDesc("当前回话为非监听回话");
			client.sendEvent(StaticValue.onListenFail, tmp);
			return;
		}
	
//		StringMap result=PublicUtils.getAppFrameResult(onlineService.listenChat(agentId, targetSessionId,targetId));
		Map<?,?> result=onlineService.listenChat(agentId, targetSessionId,targetId,originalUserId);

		EventParam eventParam=new EventParam(agentId,targetInfo.getAccessType());
		eventParam.setSessionId(targetSessionId);
		eventParam.setTargetId(targetId);
		eventParam.setAgentId(agentId);
		if ("1".equals(result.get(StaticValue.RSLTVAL))){
			EventParam tmp=new EventParam(agentId,EAccessType.ACCESS_TYPE_AGENT);
			client.sendEvent(StaticValue.onListenSuccess, eventParam);
		}else{
			eventParam.setEventDesc((String) result.get(StaticValue.RSLTMSG));
			client.sendEvent(StaticValue.onListenFail, eventParam);
		}
		
		
	}





	@Override
	public void intercept(String agentId, String target,String targetSessionId) {
		UUID uuid=userRedisCache.getAgentUUID(agentId);
		SocketIOClient client=server.getClient(uuid);
		UUID targetUuid=userRedisCache.getAgentUUID(target);
		SocketIOClient targetClient=server.getClient(targetUuid);
		if (null==client){
			return ;
		}
		OnlineCustomInfo info=userRedisCache.getInternetRequest(target, targetSessionId);

		if(null==info){
			EventParam tmp=new EventParam(agentId,EAccessType.ACCESS_TYPE_AGENT);
			tmp.setEventDesc("Session错误");
			client.sendEvent(StaticValue.onInterceptFail, tmp);
		}
		EventParam eventParam=createOnlineEventParam(info);
//		if (!EActionType.MONITOR.equals(info.getRingType())){
//			eventParam.setEventDesc("坐席没有处于监听态");
//			client.sendEvent(StaticValue.onInterceptFail, eventParam);
//		}

//		StringMap result=PublicUtils.getAppFrameResult(onlineService.interceptChat(agentId, sessionId,info.getTargetId()));
		Map<?,?> result=onlineService.interceptChat(agentId, targetSessionId,target);
//		Map<?,?> result=onlineService.interceptChat(agentId, sessionId,targetId);
		if ("1".equals(result.get(StaticValue.RSLTVAL))){
			client.sendEvent(StaticValue.onInterceptSuccess, eventParam);
			targetClient.sendEvent(StaticValue.onInterceptSuccess, eventParam);
		}else{
			eventParam.setEventDesc((String) result.get(StaticValue.RSLTMSG));
			client.sendEvent(StaticValue.onInterceptFail, eventParam);
		}
		
		
	}


	@Override
	public void insert(String agentId, String sessionId,String targetId,String originalUserId) {
		UUID uuid=userRedisCache.getAgentUUID(agentId);
		SocketIOClient client=server.getClient(uuid);
		if (null==client){
			return ;
		}
		OnlineCustomInfo info=userRedisCache.getInternetRequest(targetId, sessionId);
		if(null==info){
			EventParam tmp=new EventParam(agentId,EAccessType.ACCESS_TYPE_AGENT);
			tmp.setEventDesc("Session错误");
			client.sendEvent(StaticValue.onInsertFail, tmp);
		}
		EventParam eventParam=createOnlineEventParam(info);
		if (!EActionType.MONITOR.equals(info.getRingType())){
			eventParam.setEventDesc("坐席没有处于监听态");
			client.sendEvent(StaticValue.onInsertFail, eventParam);
		}

		Map<?,?> result= onlineService.insertChat(agentId, sessionId,targetId,originalUserId);
		if ("1".equals(result.get(StaticValue.RSLTVAL))){
			client.sendEvent(StaticValue.onInsertSuccess, eventParam);
		}else{
			eventParam.setEventDesc((String) result.get(StaticValue.RSLTMSG));
			client.sendEvent(StaticValue.onInsertFail, eventParam);
		}
		
		
	}


	@Override
	public void cancel(String agentId, String sessionId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void whisper(String agentId, String sessionId) {
		UUID uuid=userRedisCache.getAgentUUID(agentId);
		SocketIOClient client=server.getClient(uuid);
		if (null==client){
			return ;
		}
		OnlineCustomInfo info=userRedisCache.getInternetRequest(agentId, sessionId);
		if(null==info){
			EventParam tmp=new EventParam(agentId,EAccessType.ACCESS_TYPE_AGENT);
			tmp.setEventDesc("Session错误");
			client.sendEvent(StaticValue.onWhisperFail, tmp);
		}
		EventParam eventParam=createOnlineEventParam(info);
		if (!EActionType.MONITOR.equals(info.getRingType())){
			eventParam.setEventDesc("坐席没有处于监听态");
			client.sendEvent(StaticValue.onWhisperFail, eventParam);
		}

		StringMap result=PublicUtils.getAppFrameResult(onlineService.whisperChat(agentId, sessionId,info.getTargetId()));
		if ("1".equals(result.get(StaticValue.RSLTVAL))){
			client.sendEvent(StaticValue.onWhisperSuccess, eventParam);
		}else{
			eventParam.setEventDesc(result.get(StaticValue.RSLTMSG));
			client.sendEvent(StaticValue.onWhisperFail, eventParam);
		}
	}



	@Override
	public void forceRelease(String targetAgent, String targetSessionId, String currentAgent) {
		UUID uuid=userRedisCache.getAgentUUID(currentAgent);
		SocketIOClient client=server.getClient(uuid);
		if (null==client){
			return ;
		}
		
		OnlineCustomInfo info=userRedisCache.getInternetRequest(targetAgent, targetSessionId);
		if(null==info){
			EventParam tmp=new EventParam(currentAgent,EAccessType.ACCESS_TYPE_AGENT);
			tmp.setEventDesc("Session错误");
			client.sendEvent(StaticValue.onForceReleaseFail, tmp);
		}
		EventParam eventParam=createOnlineEventParam(info);
		eventParam.setAgentId(currentAgent);
		eventParam.setTargetId(targetAgent);
		
//		StringMap result=PublicUtils.getAppFrameResult(onlineService.forceReleaseCall(targetAgent, targetSessionId, info.getRingType().value()));
		Map<?,?> result=onlineService.forceReleaseCall(targetAgent, targetSessionId, currentAgent,info.getRingType().value());
		if ("1".equals(result.get(StaticValue.RSLTVAL))){

			client.sendEvent(StaticValue.onForceReleaseSuccess, eventParam);
//			router.releaseCall(agentId, info.getOriginalUserId(), sessionId);
		}else{
			eventParam.setEventDesc((String) result.get(StaticValue.RSLTMSG));
			client.sendEvent(StaticValue.onForceReleaseFail, eventParam);
		}
		
	}

	@Override
	public void forceSignOut(String targetAgent,String currentAgent) {
		//判断坐席中是否有未关闭会话(意外中断)
		Map<String,String> info =userRedisCache.getAgentInfo(targetAgent);
		List<String> chatIdList = new ArrayList<>();
		for (String key : info.keySet()){
			if (key.indexOf("ONLINE:") !=-1){
				String value=info.get(key);
				Map mapTypes = JSON.parseObject(value);
				String ringType = (String) mapTypes.get("ringType");
				String keyTemp = key.replace("ONLINE:","");
				chatIdList.add(keyTemp+"#"+ringType);
			}
		}
		onlineService.forceSignOut(targetAgent,chatIdList);
	}

	@Override
	public void signOut(String targetAgent, String currentAgent) {
		onlineService.signOut(targetAgent);
	}


}
