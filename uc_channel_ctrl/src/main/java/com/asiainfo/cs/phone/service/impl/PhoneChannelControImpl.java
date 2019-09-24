package com.asiainfo.cs.phone.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.asiainfo.cs.common.util.EventParam;
import com.asiainfo.cs.common.util.StaticValue;
import com.asiainfo.cs.common.util.StaticValue.*;
import com.asiainfo.cs.entity.PhoneCustomInfo;
import com.asiainfo.cs.log.entity.AgentStateLogModel;
import com.asiainfo.cs.phone.entity.EventForCC;
import com.asiainfo.cs.phone.service.*;
import com.asiainfo.cs.rabbit.RabbitmqSender;
import com.asiainfo.cs.service.IntelligenRouter;
import com.asiainfo.cs.service.UserRedisCache;
import com.asiainfo.cs.task.TimeTask;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

//import com.asiainfo.cs.phone.entity.EventCommandObject;

@Component("phoneChannelControImpl")
@Lazy
public class PhoneChannelControImpl implements PhoneChannelControl, ChannelEvent, ChannelEventTimeOut, TelephoneEvent {
    private static final Logger LOG = LoggerFactory.getLogger(PhoneChannelControImpl.class);
    private String bridgePlan = "'wait_for_answer,set:${uuid_bridge %s ${uuid}}' inline";
    @Value("${sys.outboundFormat:user/%s}")
    private String outboundFormat;

    @Value("${sys.musicOnHold:music_on_hold}")
    private String musicOnHold;
    @Value("${sys.seatRequestTimeOut:30}")
    private Integer seatRequestTimeOut;

    @Value("${sys.otherRequestTimeOut:30}")
    private Integer otherRequestTimeOut;

    @Value("${sys.recordPath:/usr/local/freeswitch/recordings/}")
    private String recordPath;
    @Value("${sys.sessionPath:/usr/local/freeswitch/recordings/}")
    private String sessionPath;

    @Value("${sys.telephone.transfer_after_release}")
    private String transfer_after_release;

    @Autowired
    private IntelligenRouter router;
    @Autowired
    private SocketIOServer server;
    @Autowired
    private UserRedisCache userRedisCache;
    @Autowired
    private EslSupport eslSupport;
    @Autowired
    private RabbitmqSender rabbitmqSender;

    @Autowired
    private TimeTask task;

    @Resource(name = "channel_seat_map")
    private Map<String, String> channel_seat_map;

    @Resource(name = "seat_list")
    private Map<String, String> seat_list;

    @Resource(name = "monitor_list")
    private Map<String, String> monitor_list;

    @PostConstruct
    private void init() {

        LOG.info("outboundFormat:" + outboundFormat);
        LOG.info("musicOnHold:" + musicOnHold);
        LOG.info("recordPath:" + recordPath);
        LOG.info("seatRequestTimeOut:" + seatRequestTimeOut);
        LOG.info("otherRequestTimeOut:" + otherRequestTimeOut);

    }
    private EventParam createTeleEventParam(String agentId){
        EventParam eventParam=new EventParam(agentId, EAccessType.ACCESS_TYPE_TELE);
        eventParam.setSessionId(userRedisCache.getSessionId(agentId));
        eventParam.setOriginalUserId(userRedisCache.getUserChannId(agentId));
        eventParam.setUserAccount(userRedisCache.getUserName(agentId));
        eventParam.setUserName(userRedisCache.getUserName(agentId));
        eventParam.setMediaState(userRedisCache.getAgentAudioState(agentId));
        eventParam.setRingType(userRedisCache.getRingType(agentId));
        return eventParam;
    }

    private boolean sendCustomResp(String seat, String response_type) {
        String reqChannelHost = userRedisCache.getUserChannHost(seat);
        String reqChannelUUID = userRedisCache.getUserChannId(seat);
        String session_id = userRedisCache.getSessionId(seat);
        String core_uuid = userRedisCache.getAgentServiceUUID(seat);
//		/*EventCommandObject obj = new EventCommandObject();
//		obj.setHost(reqChannelHost);
//	    obj.setUniqueId(reqChannelUUID);
//		obj.setEventName("CUSTOM");
//	    obj.setEventSubClass("cc::resp");
//		obj.putHeader("response_type", response_type);
//		obj.putHeader("session_id", session_id);*/
//		//eslSupport.sendEventCommand(obj);
        //	rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
        //huangqb @2019-4-18 解决bug页面振铃一段时间消失振铃问题....
        EventForCC obj = new EventForCC();
        obj.setEventName("CUSTOM");
        obj.setEventSubClass("cc::resp");
        obj.setHost(reqChannelHost);
        obj.setFreeSWITCH_Hostname(reqChannelHost);
        obj.setResponse_type(response_type);
        obj.setUnique_id(reqChannelUUID);
        obj.setSource_uuid(reqChannelUUID);
        obj.setSession_id(session_id);
        obj.setLocal_unique_id(reqChannelUUID);
        rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
        //rabbitmqSender.sendDataToCC(reqChannelHost,"cc::resp",reqChannelUUID,JSONObject.toJSONString(obj));
        // end of huangqb 2019-4-18
        return false;
    }

    public void setSeatToIdle(String seat) {
        userRedisCache.setAgentAudioState(seat, ESeatMediaState.IDLE);
        userRedisCache.setRingType(seat, EActionType.NONE);
        userRedisCache.setAgentRequestType(seat, EActionType.NONE);


        String seatChannelHost = userRedisCache.getAgentChannHost(seat);
        String seatChannelUUID = userRedisCache.getAgentChannId(seat);

        if (eslSupport.uuid_exists(seatChannelHost, seatChannelUUID))
            eslSupport.uuid_kill(seatChannelHost, seatChannelUUID);

        String sessionId = userRedisCache.getSessionId(seat);
        String requesterUUID = userRedisCache.getUserChannId(seat);
        router.releaseCall(seat, requesterUUID, sessionId);

        userRedisCache.setPhoneRequest(seat, sessionId, null);
        // String state = userRedisCache.getStateOnHangup(seat);
        // userRedisCache.setUserState(seat, state);
    }

    private void answerSERVICE(String seat,String sessionId) {
        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);
        String reqChannelHost = userRedisCache.getUserChannHost(seat);
        String reqChannelUUID = userRedisCache.getUserChannId(seat);
        String channelName = userRedisCache.getAgentChannelName(seat);
        ESeatMediaState audioState = userRedisCache.getAgentAudioState(seat);
        //todo huangqb add 修改bug：软电话终端上没有获取到来电号码，显示为000000000
        String sourcename = userRedisCache.getSourceName(seat);

        EventParam eventParam=createTeleEventParam(seat);
        if (!ESeatMediaState.RING.equals(audioState)) {
            eventParam.setEventDesc("状态错误:"+audioState.desc());
            client.sendEvent(StaticValue.onAnswerFail, eventParam);
            return;
        }
        if (!eslSupport.uuid_exists(reqChannelHost, reqChannelUUID)) {
            setSeatToIdle(seat);
            eventParam.setEventDesc("服务对象不存在");
            client.sendEvent(StaticValue.onAnswerFail, eventParam);
            return;
        }

        userRedisCache.setAgentAudioState(seat, ESeatMediaState.WAITCON);

        String seatChannelHost = reqChannelHost;
        String seatChannelUUID = userRedisCache.getSessionId(seat);
//		userRedisCache.setActiveChannel(seat, seatChannelUUID, seatChannelHost);
        userRedisCache.setAgentChannHost(seat, seatChannelHost);
        userRedisCache.setAgentChannId(seat, seatChannelUUID);
        //String param = "{origination_uuid=" + seatChannelUUID + "}{req_uuid="+reqChannelUUID+"}{"+ StaticValue.SeatName+"=" + seat + "}{"+ StaticValue.RingType+"="+ EActionType.SERVICE.name()+"}"
        //		+ channelName + " " + String.format(bridgePlan, reqChannelUUID);
        //todo huangqb modify bug:软电话终端上没有获取到来电号码，显示为000000000
        String param = "{origination_uuid=" + seatChannelUUID + "}{origination_caller_id_number="+ sourcename +"}{target_signId=" + uuid.toString() + "}{req_uuid="+reqChannelUUID+"}{"+ StaticValue.SeatName+"=" + seat + "}{"+ StaticValue.RingType+"="+ EActionType.SERVICE.name()+"}"
                + channelName + " " + String.format(bridgePlan, reqChannelUUID);
        String result = eslSupport.originate(reqChannelHost, param);
        if (result.startsWith("+OK")) {
            if (EActionType.SERVICE.equals(userRedisCache.getRingType(seat))) {
                router.answerSuccess(seat, reqChannelUUID, seatChannelUUID);
            }
            userRedisCache.setAgentAudioState(seat, ESeatMediaState.ANSWER);
            // TODO 再次检查用户话路是否存在
            // TODO 检查用户话路是否已经被桥接到其他坐席上。
            // TODO 两种处理方式，一种是由脚本进行桥接，另外一种是由CC进行话路桥接
//			if (eslSupport.uuid_bridge(reqChannelHost, reqChannelUUID, seatChannelUUID)) {
            //阅读工号
            //eslSupport.uuid_broadcast(reqChannelHost, reqChannelUUID, "say::zh\\snumber\\siterated\\s" + seat);
            //eslSupport.uuid_broadcast(reqChannelHost, reqChannelUUID, "srv_for_you.wav");
            //sendCustomResp(seat, seatChannelUUID);
            userRedisCache.setAgentAudioState(seat, ESeatMediaState.TALK);
            eventParam.setEventDesc("坐席应答服务请求成功。");
            client.sendEvent(StaticValue.onAnswerSuccess, eventParam);
//			} else {
//				sendCustomResp(seat, "busy");
//				setSeatToIdle(seat);
//				eventParam.setEventDesc( "桥接坐席话机失败");
//				client.sendEvent(StaticValue.onAnswerFail,eventParam);
//			}
        } else {
            sendCustomResp(seat, "busy");
            setSeatToIdle(seat);
            eventParam.setEventDesc( "呼叫坐席话机失败");
            client.sendEvent(StaticValue.onAnswerFail, eventParam);
        }
        //增加log部分...
        AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
        agentStateLogModel.setEventName("CUSTOM");
        agentStateLogModel.setEventSubclass("AGENT::ANSWER_AGENT");
        agentStateLogModel.setResult(result.startsWith("+OK")? StaticValue.RESULT_SUCCESS:StaticValue.RESULT_FAIL);
        agentStateLogModel.setEventContent(result.startsWith("+OK")? "坐席应答服务请求成功":"呼叫坐席话机失败");
        agentStateLogModel.setAgnetId(seat);
        agentStateLogModel.setAgentSignId(UUID.fromString(seat_list.get(seat)).toString());
        agentStateLogModel.setTargetId(seat);
        agentStateLogModel.setTargetSignId(uuid.toString());
        agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
        agentStateLogModel.setOperTime(System.currentTimeMillis());
        agentStateLogModel.setTargetChatId(userRedisCache.getSessionId(seat));
        agentStateLogModel.setChatId(userRedisCache.getSourceChannId(seat));
        rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));

    }

    private void releaseSERVICE(String seat,String sessionId) {
        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);
        ESeatMediaState audioState = userRedisCache.getAgentAudioState(seat);

        EventParam eventParam=createTeleEventParam(seat);
        eventParam.setEventDesc( "坐席取消服务请求。");
        if (ESeatMediaState.RING.equals(audioState)) {
            sendCustomResp(seat, "busy");
        } else {
            String host = userRedisCache.getUserChannHost(seat);
            String requesterId = userRedisCache.getUserChannId(seat);
            if (!transfer_after_release.isEmpty()){
                String value=String.format(transfer_after_release, seat);
                eslSupport.uuid_setvar(host, requesterId, "transfer_after_bridge", value);
            }
        }
        setSeatToIdle(seat);
        client.sendEvent(StaticValue.onReleaseSuccess, eventParam);
    }

    private void answerINNER(String seat,String sessionId) {

        EventParam eventParam=createTeleEventParam(seat);

        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);
        String reqChannelHost = userRedisCache.getUserChannHost(seat);
        String reqChannelUUID = userRedisCache.getUserChannId(seat);
        String reqName = userRedisCache.getUserName(seat);


        if (!eslSupport.uuid_exists(reqChannelHost, reqChannelUUID)) {
            eventParam.setEventDesc( "内部呼叫不存在");
            client.sendEvent(StaticValue.onAnswerFail, eventParam);
            return;
        }
        userRedisCache.setAgentAudioState(seat, ESeatMediaState.WAITCON);
        String channelName = userRedisCache.getAgentChannelName(seat);
        String core_uuid = userRedisCache.getAgentServiceUUID(seat);
//		String sessionId=userRedisCache.getSessionId(seat);
        String seatChannelHost = reqChannelHost;
        String seatChannelUUID = sessionId;
//		userRedisCache.setActiveChannel(seat, seatChannelUUID, seatChannelHost);
        userRedisCache.setAgentChannHost(seat, seatChannelHost);
        userRedisCache.setAgentChannId(seat, seatChannelUUID);
        String param = "{origination_uuid=" + seatChannelUUID + "}{"+ StaticValue.SeatName+"=" + seat + "}{"+ StaticValue.RingType+"="+ EActionType.INNER_CALL.name()+"}"
                + channelName + " "  + String.format(bridgePlan, reqChannelUUID);
        String result = eslSupport.originate(reqChannelHost, param);
        if (result.startsWith("+OK")) {
            userRedisCache.setAgentAudioState(seat, ESeatMediaState.ANSWER);
//			if (eslSupport.uuid_bridge(reqChannelHost, reqChannelUUID, seatChannelUUID)) {
            userRedisCache.setAgentAudioState(seat, ESeatMediaState.TALK);
//				EventCommandObject obj = new EventCommandObject();
//				obj.setHost(reqChannelHost);
//				obj.setEventName("CUSTOM");
//				obj.setEventSubClass("cc::call_resp");
//				obj.putHeader("target_agent", reqName);
//				obj.putHeader("source_name", seat);
//				obj.putHeader("source_uuid", seatChannelUUID);
//				obj.putHeader("session_id", sessionId);
//				obj.putHeader("response_type", "ok");
            //eslSupport.sendEventCommand(obj);
//			    rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
            EventForCC obj = new EventForCC();
            obj.setEventName("CUSTOM");
            obj.setEventSubClass("cc::call_resp");
            obj.setHost(reqChannelHost);
            obj.setFreeSWITCH_Hostname(reqChannelHost);
            obj.setSource_name(seat);
            obj.setTarget_agent(reqName);
            obj.setResponse_type("ok");
            obj.setSource_uuid(seatChannelUUID);
            obj.setSession_id(sessionId);
            rabbitmqSender.sendDataToCC(reqChannelHost,"cc::call_resp","",JSONObject.toJSONString(obj));
            eventParam.setEventDesc( "应答成功。");
            client.sendEvent(StaticValue.onAnswerSuccess, eventParam);
            return;
//			}
        }

//		EventCommandObject obj = new EventCommandObject();
//		obj.setHost(reqChannelHost);
//		obj.setEventName("CUSTOM");
//		obj.setEventSubClass("cc::call_resp");
//		obj.putHeader("target_agent", reqName);
//		obj.putHeader("source_name", seat);
//		obj.putHeader("source_uuid", seatChannelUUID);
//		obj.putHeader("session_id", sessionId);
//		obj.putHeader("response_type", "busy");
        //eslSupport.sendEventCommand(obj);
//		rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
        EventForCC obj = new EventForCC();
        obj.setEventName("CUSTOM");
        obj.setEventSubClass("cc::call_resp");
        obj.setHost(reqChannelHost);
        obj.setFreeSWITCH_Hostname(reqChannelHost);
        obj.setSource_name(seat);
        obj.setTarget_agent(reqName);
        obj.setResponse_type("busy");
        obj.setSource_uuid(seatChannelUUID);
        obj.setSession_id(sessionId);
        rabbitmqSender.sendDataToCC(reqChannelHost,"cc::call_resp","",JSONObject.toJSONString(obj));

        eventParam.setEventDesc( "呼叫坐席话机失败");
        client.sendEvent(StaticValue.onAnswerFail, eventParam);
        setSeatToIdle(seat);

    }

    private void releaseINNER(String seat,String sessionId) {
        sessionId=userRedisCache.getSessionId(seat);
        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);

        String reqChannelHost = userRedisCache.getUserChannHost(seat);
        String core_uuid = userRedisCache.getAgentServiceUUID(seat);

        String sourceName = userRedisCache.getSourceName(seat);
        ESeatMediaState audioState = userRedisCache.getAgentAudioState(seat);
        if (ESeatMediaState.RING.equals(audioState)) {
//			EventCommandObject obj = new EventCommandObject();
//			obj.setHost(reqChannelHost);
//			obj.setEventName("CUSTOM");
//			obj.setEventSubClass("cc::call_resp");
//			obj.putHeader("target_agent", sourceName);
//			obj.putHeader("source_name", seat);
//			obj.putHeader("session_id", sessionId);
//			obj.putHeader("response_type", "busy");
//			eslSupport.sendEventCommand(obj);
//			rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
            EventForCC obj = new EventForCC();
            obj.setEventName("CUSTOM");
            obj.setEventSubClass("cc::call_resp");
            obj.setHost(reqChannelHost);
            obj.setFreeSWITCH_Hostname(reqChannelHost);
            obj.setSource_name(seat);
            obj.setTarget_agent(sourceName);
            obj.setResponse_type("busy");
            obj.setSession_id(sessionId);
            rabbitmqSender.sendDataToCC(reqChannelHost,"cc::call_resp","",JSONObject.toJSONString(obj));
        }
        setSeatToIdle(seat);
        EventParam eventParam=createTeleEventParam(seat);
        eventParam.setEventDesc("释放成功");
        client.sendEvent(StaticValue.onReleaseSuccess,eventParam);
    }

    private void answerTRUNS_SEAT(String seat,String sessionId) {
        EventParam eventParam=createTeleEventParam(seat);;
//		userRedisCache.setSeatRingType(seat, ESeatActionType.SERVICE);
        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);

        String userChannHost = userRedisCache.getUserChannHost(seat);
        String userChannId = userRedisCache.getUserChannId(seat);

        String channelName = userRedisCache.getAgentChannelName(seat);
        String targetName = userRedisCache.getSourceName(seat);
        // String targetUUID=userRedisCache.getTargetChannelUUID(seat);

        ESeatMediaState audioState = userRedisCache.getAgentAudioState(seat);
        if (!ESeatMediaState.RING.equals(audioState)) {
            setSeatToIdle(seat);
            eventParam.setEventDesc("坐席没有服务请求到达");
            client.sendEvent(StaticValue.onAnswerFail, eventParam);
            return;
        }
        if (!eslSupport.uuid_exists(userChannHost, userChannId)) {
            setSeatToIdle(seat);
            eventParam.setEventDesc("服务对象不存在");
            client.sendEvent(StaticValue.onAnswerFail, eventParam);
            return;
        }

        userRedisCache.setAgentAudioState(seat, ESeatMediaState.WAITCON);

        String seatChannelHost = userChannHost;
        String seatChannelUUID = userRedisCache.getSessionId(seat);
        String core_uuid = userRedisCache.getAgentServiceUUID(seat);
        String operation_result; //求助坐席的结果...
        Integer iresult; //求助结果,枚举描述...
//		userRedisCache.setActiveChannel(seat, seatChannelUUID, seatChannelHost);
        userRedisCache.setAgentChannHost(seat, seatChannelHost);
        userRedisCache.setAgentChannId(seat, seatChannelUUID);
        String originateNumber = userRedisCache.getOriginalNumber(seat);
        eslSupport.uuid_setvar(userChannHost, userChannId, "ai_target_agent", seat);

        //huangqb modify for number display 2019-4-15
        //String param = "{origination_uuid=" + seatChannelUUID + "}{req_uuid="+userChannId+"}{"+ StaticValue.SeatName+"=" + seat + "}{"+ StaticValue.RingType+"="+ EActionType.TRUNS_SEAT.name()+"}"
        //		+ channelName + " " + String.format(bridgePlan, userChannId);
        String param = "{origination_uuid=" + seatChannelUUID + "}{origination_caller_id_number="+ originateNumber +"}{req_uuid="+userChannId+"}{"+ StaticValue.SeatName+"=" + seat + "}{"+ StaticValue.RingType+"="+ EActionType.TRUNS_SEAT.name()+"}"
                + channelName + " " + String.format(bridgePlan, userChannId);
        //end of huangqb modify 2019-4-15
        String result = eslSupport.originate(userChannHost, param);
        if (result.startsWith("+OK")) {
             userRedisCache.setAgentAudioState(seat, ESeatMediaState.ANSWER);
            //if (eslSupport.uuid_bridge(userChannHost, userChannId, seatChannelUUID)) {
            //eslSupport.uuid_broadcast(userChannHost, userChannId, "say::zh\\snumber\\siterated\\s" + seat);
            //eslSupport.uuid_broadcast(userChannHost, userChannId, "srv_for_you.wav");
//				EventCommandObject obj = new EventCommandObject();
//				obj.setHost(userChannHost);
//				obj.setEventName("CUSTOM");
//				obj.setEventSubClass("cc::trans_resp");
//				obj.putHeader("unique_id", userChannId);
//				obj.putHeader("source_name", seat);
//				obj.putHeader("target_agent", targetName);
//				obj.putHeader("response_type", "ok");
//				obj.putHeader("FreeSWITCH-Hostname",userChannHost);
            //eslSupport.sendEventCommand(obj);
//			rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
            EventForCC obj = new EventForCC();
            obj.setEventName("CUSTOM");
            obj.setEventSubClass("cc::trans_resp");
            obj.setHost(userChannHost);
            obj.setFreeSWITCH_Hostname(userChannHost);
            obj.setSource_name(seat);
            obj.setTarget_agent(targetName);
            obj.setResponse_type("ok");
            obj.setUnique_id(userChannId);
            rabbitmqSender.sendDataToCC(userChannHost,"cc::trans_resp",userChannId,JSONObject.toJSONString(obj));

            userRedisCache.setAgentAudioState(seat, ESeatMediaState.TALK);
            eventParam.setEventDesc("坐席应答成功");
            client.sendEvent(StaticValue.onAnswerSuccess, eventParam);
            //log部分添加 2019-4-29...
            operation_result = "转坐席成功";
            iresult = StaticValue.RESULT_SUCCESS;
//			} else {
//				EventCommandObject obj = new EventCommandObject();
//				obj.setHost(userChannHost);
//				obj.setEventName("CUSTOM");
//				obj.setEventSubClass("cc::trans_resp");
//				obj.putHeader("unique_id", userChannId);
//				obj.putHeader("source_name", seat);
//				obj.putHeader("target_agent", targetName);
//				obj.putHeader("response_type", "busy");
//				eslSupport.sendEventCommand(obj);
//				setSeatToIdle(seat);
//				eventParam.setEventDesc("呼叫坐席失败");
//				client.sendEvent(StaticValue.onAnswerFail, eventParam);
//			}
        } else {
//			EventCommandObject obj = new EventCommandObject();
//			obj.setHost(userChannHost);
//			obj.setEventName("CUSTOM");
//			obj.setEventSubClass("cc::trans_resp");
//			obj.putHeader("unique_id", userChannId);
//			obj.putHeader("source_name", seat);
//			obj.putHeader("target_agent", targetName);
//			obj.putHeader("response_type", "busy");
            //eslSupport.sendEventCommand(obj);
//			rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
            eslSupport.uuid_play_tts_voice(userChannHost,userChannId,"转接失败，当前话务员继续为您服务");
            EventForCC obj = new EventForCC();
            obj.setEventName("CUSTOM");
            obj.setEventSubClass("cc::trans_resp");
            obj.setHost(userChannHost);
            obj.setFreeSWITCH_Hostname(userChannHost);
            obj.setSource_name(seat);
            obj.setTarget_agent(targetName);
            obj.setResponse_type("busy");
            obj.setUnique_id(userChannId);
            rabbitmqSender.sendDataToCC(userChannHost,"cc::trans_resp",userChannId,JSONObject.toJSONString(obj));
            setSeatToIdle(seat);
            eventParam.setEventDesc("呼叫坐席失败");
            client.sendEvent(StaticValue.onAnswerFail, eventParam);
            //log 部分...
            operation_result = "转坐席失败";
            iresult = StaticValue.RESULT_FAIL;
        }
        //增加转坐席的log部分,huangqb 2019-4-29
        AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
        agentStateLogModel.setEventName("CUSTOM");
        agentStateLogModel.setEventSubclass("AGENT::TRANS_SEAT");
        agentStateLogModel.setResult(iresult);
        agentStateLogModel.setEventContent(operation_result);
        agentStateLogModel.setAgnetId(targetName);
        agentStateLogModel.setAgentSignId(UUID.fromString(seat_list.get(targetName)).toString());
        agentStateLogModel.setTargetId(seat);
        agentStateLogModel.setTargetSignId(uuid.toString());
        agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
        agentStateLogModel.setOperTime(System.currentTimeMillis());
        agentStateLogModel.setTargetChatId(userRedisCache.getSessionId(seat));
        agentStateLogModel.setChatId(userRedisCache.getSourceChannId(seat));
        rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
    }

    private void releaseTRUNS_SEAT(String seat,String sessionId) {

        EventParam eventParam=createTeleEventParam(seat);
        eventParam.setEventDesc("释放成功");

        // 坐席在接受转入前会执行此释放，对于接受转入后，坐席的状态变更成了SERVICE，这样释放的时候就不会执行此状态
        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);
        String reqChannelHost = userRedisCache.getUserChannHost(seat);
        String reqChannelUUID = userRedisCache.getUserChannId(seat);
        String targetName = userRedisCache.getSourceName(seat);
        String core_uuid = userRedisCache.getAgentServiceUUID(seat);
        String bridge_uuid = eslSupport.uuid_getvar(reqChannelHost, reqChannelUUID, "bridge_uuid");
        if (eslSupport.uuid_exists(reqChannelHost, bridge_uuid)) {
            // 成功转执行此段代码
//			EventCommandObject obj = new EventCommandObject();
//			obj.setHost(reqChannelHost);
//
//			obj.setEventName("CUSTOM");
//			obj.setEventSubClass("cc::trans_resp");
//			obj.putHeader("unique_id", reqChannelUUID);
//			obj.putHeader("source_name", seat);
//			obj.putHeader("target_agent", targetName);
//			obj.putHeader("response_type", "busy");
            //eslSupport.sendEventCommand(obj);
//			rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
            EventForCC obj = new EventForCC();
            obj.setEventName("CUSTOM");
            obj.setEventSubClass("cc::trans_resp");
            obj.setHost(reqChannelHost);
            obj.setFreeSWITCH_Hostname(reqChannelHost);
            obj.setSource_name(seat);
            obj.setTarget_agent(targetName);
            obj.setResponse_type("busy");
            obj.setUnique_id(reqChannelUUID);
            rabbitmqSender.sendDataToCC(reqChannelHost,"cc::trans_resp",reqChannelUUID,JSONObject.toJSONString(obj));
        } else {
            String host = userRedisCache.getUserChannHost(seat);
            String requesterId = userRedisCache.getUserChannId(seat);
            if (!transfer_after_release.isEmpty()){
                String value=String.format(transfer_after_release, seat);
                eslSupport.uuid_setvar(host, requesterId, "transfer_after_bridge", value);
            }
        }
        setSeatToIdle(seat);
        client.sendEvent(StaticValue.onReleaseSuccess,eventParam);

    }

    @Async
    @Override
    public void answer(String seat,String sessionId,int accessType) {



        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);
        EActionType first = userRedisCache.getRingType(seat);
        String reghost = userRedisCache.getUserChannHost(seat);
     //   LOG.debug("in answer function-------> first:" + first + "\n\n");
        switch (first) {
            case SERVICE:
                answerSERVICE(seat,sessionId);
                break;
            case HELP_SEAT:
                answerHELP_SEAT(seat,sessionId);
                break;
            case TRUNS_SEAT:
                answerTRUNS_SEAT(seat,sessionId);
                break;
            case INNER_CALL: {
                answerINNER(seat,sessionId);
                break;
            }
            default:
                EventParam eventParam=new EventParam(seat, EAccessType.ACCESS_TYPE_TELE);
                eventParam.setEventDesc("坐席没有任何服务请求");
                client.sendEvent(StaticValue.onAnswerFail, eventParam);
                setSeatToIdle(seat);
                break;
        }

    }

    void answerHELP_SEAT(String seat,String sessionId) {
        EventParam eventParam=createTeleEventParam(seat);
        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);
        String sourceHost = userRedisCache.getSourceHost(seat);
        String sourceUUID = userRedisCache.getSourceChannId(seat);
        String sourceName = userRedisCache.getSourceName(seat);
        UUID source_uuid = UUID.fromString(seat_list.get(sourceName));


        if (!eslSupport.uuid_exists(sourceHost, sourceUUID)) {
            eventParam.setEventDesc("源求助信道不存在");
            client.sendEvent(StaticValue.onAnswerFail, eventParam);
            return;
        }

        userRedisCache.setAgentAudioState(seat, ESeatMediaState.WAITCON);
        String channelName = userRedisCache.getAgentChannelName(seat);
        String core_uuid = userRedisCache.getAgentServiceUUID(seat);

        String seatChannelHost = sourceHost;
        String seatChannelUUID = userRedisCache.getSessionId(seat);
        //userRedisCache.setc
//		userRedisCache.setActiveChannel(seat, seatChannelUUID, seatChannelHost);
        userRedisCache.setAgentChannHost(seat, seatChannelHost);
        userRedisCache.setAgentChannId(seat, seatChannelUUID);
        //String Original_number = userRedisCache.getOriginalNumber(seat);
        String param = "{origination_uuid=" + seatChannelUUID +"}{origination_caller_id_number="+ sourceName + "}{"+ StaticValue.HelpSource+"=" + sourceName + "}{"+ StaticValue.SeatName+"=" + seat
                + "}{"+ StaticValue.RingType+"="+ EActionType.HELP_SEAT.name()+"}" + channelName + " " + musicOnHold;
        String result = eslSupport.originate(sourceHost, param);

        //huangqb add for put log
        String operation_result; //求助坐席的结果...
        Integer iresult; //求助结果,枚举描述...

        if (result.startsWith("+OK")) {

            userRedisCache.setAgentAudioState(seat, ESeatMediaState.ANSWER);
            eventParam.setEventDesc("坐席应答成功。");
            client.sendEvent(StaticValue.onAnswerSuccess, eventParam);


            //huangqb add 2019-4-18
            try
            {
                Thread.sleep(2000);    //延时2秒
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }
            //end of huangqb add 2019-4-18
            eslSupport.uuid_att_xfer(sourceHost,sourceUUID,seatChannelUUID);
            //eslSupport.uuid_att_xfer(sourceHost,seatChannelUUID,sourceUUID);
//			APPCommandObject obj = new APPCommandObject();
//			obj.setHost(sourceHost);
//			obj.setUuid(sourceUUID);
//			obj.setAppName("att_xfer");
//			obj.setArg(seatChannelUUID);
//			eslSupport.sendAppCommand(obj);
//			rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));

//			EventCommandObject reply = new EventCommandObject();
//			reply.setHost(sourceHost);
//			reply.setEventName("CUSTOM");
//			reply.setEventSubClass("cc::help_resp");
//			reply.putHeader("target_agent", sourceName);
//			reply.putHeader("source_name", seat);
//			reply.putHeader("seatB_UUID", seatChannelUUID);
//			reply.putHeader("response_type", "ok");
//			eslSupport.sendEventCommand(reply);
//			rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(reply));
            EventForCC obj = new EventForCC();
            obj.setEventName("CUSTOM");
            obj.setEventSubClass("cc::help_resp");
            obj.setHost(sourceHost);
            obj.setFreeSWITCH_Hostname(sourceHost);
            obj.setSource_name(seat);
            obj.setTarget_agent(sourceName);
            obj.setResponse_type("ok");
            obj.setSeatB_UUID(seatChannelUUID);
            rabbitmqSender.sendDataToCC(sourceHost,"cc::help_resp","",JSONObject.toJSONString(obj));

            if (userRedisCache.getTargetName(sourceName).equals(seat)) {
                userRedisCache.setTargetChannId(sourceName, seatChannelUUID);
                userRedisCache.setTargetHost(sourceName, sourceHost);

            }
            //求助坐席结果赋值...
            operation_result = "求助坐席成功，坐席ID：" + channelName;
            iresult = StaticValue.RESULT_SUCCESS;
        } else {
            setSeatToIdle(seat);
            eventParam.setEventDesc("呼叫坐席失败");
            client.sendEvent(StaticValue.onAnswerFail,eventParam);

//			EventCommandObject reply = new EventCommandObject();
//			reply.setHost(sourceHost);
//			reply.setEventName("CUSTOM");
//			reply.setEventSubClass("cc::help_resp");
//			reply.putHeader("target_agent", sourceName);
//			reply.putHeader("source_name", seat);
//			reply.putHeader("response_type", "busy");
//			eslSupport.sendEventCommand(reply);

//			rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(reply));
            EventForCC obj = new EventForCC();
            obj.setEventName("CUSTOM");
            obj.setEventSubClass("cc::help_resp");
            obj.setHost(sourceHost);
            obj.setFreeSWITCH_Hostname(sourceHost);
            obj.setSource_name(seat);
            obj.setTarget_agent(sourceName);
            obj.setResponse_type("busy");
            rabbitmqSender.sendDataToCC(sourceHost,"cc::help_resp","",JSONObject.toJSONString(obj));
            //求助log部分
            operation_result = "求助坐席失败，坐席ID：" + channelName;
            iresult = StaticValue.RESULT_FAIL;
        }
        //增加对业务的log推送部分 2019-4-28
        AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
        agentStateLogModel.setEventName("CUSTOM");
        agentStateLogModel.setEventSubclass("AGENT::HELP_SEAT");
        agentStateLogModel.setResult(iresult);
        agentStateLogModel.setEventContent(operation_result);
        agentStateLogModel.setAgnetId(sourceName);
        agentStateLogModel.setAgentSignId(source_uuid.toString());
        agentStateLogModel.setTargetId(seat);
        agentStateLogModel.setTargetSignId(uuid.toString());
        agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
        agentStateLogModel.setOperTime(System.currentTimeMillis());
        agentStateLogModel.setTargetChatId(seatChannelUUID);
        agentStateLogModel.setChatId(sourceUUID);
        rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
        //LOG.debug("send msg to log queue:" + JSONObject.toJSONString(agentStateLogModel));

    }

    private void releaseHELP_SEAT(String seat,String sessionId) {

        EventParam eventParam=createTeleEventParam(seat);
        eventParam.setEventDesc("释放成功");

        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);

        // ESeatActionType first = userRedisCache.getSeatFirstAction(seat);
        ESeatMediaState audioState = userRedisCache.getAgentAudioState(seat);

        // 这里只能是被叫座席挂机
        // 主叫挂机的first=SERVICE，在releaseSERVICE中处理
        String seatB = seat;
        String seatA = userRedisCache.getSourceName(seatB);
        String seatA_host = userRedisCache.getAgentChannHost(seatA);
        String core_uuid = userRedisCache.getAgentServiceUUID(seatB);


        if (ESeatMediaState.TALK.equals(audioState)) {
            // 坐席之间接通后，被叫挂机
            setSeatToIdle(seat);
            client.sendEvent(StaticValue.onReleaseSuccess, eventParam);
        } else if (ESeatMediaState.RING.equals(audioState)) {
            // 坐席之间接通前，被叫拒接

//			EventCommandObject obj = new EventCommandObject();
//			obj.setHost(seatA_host);
//			obj.setEventName("CUSTOM");
//			obj.setEventSubClass("cc::help_resp");
//			obj.putHeader("response_type", "busy");
//			obj.putHeader("target_agent", seatA);
//			obj.putHeader("source_name", seatB);
            //eslSupport.sendEventCommand(obj);
//			rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
            EventForCC obj = new EventForCC();
            obj.setEventName("CUSTOM");
            obj.setEventSubClass("cc::help_resp");
            obj.setHost(seatA_host);
            obj.setFreeSWITCH_Hostname(seatA_host);
            obj.setSource_name(seatB);
            obj.setTarget_agent(seatA);
            obj.setResponse_type("busy");
            rabbitmqSender.sendDataToCC(seatA_host,"cc::help_resp","",JSONObject.toJSONString(obj));

            setSeatToIdle(seat);
            client.sendEvent(StaticValue.onReleaseSuccess, eventParam);
        }

    }

    @Async
    @Override
    public void cancel(String seat,String sessionId) {
        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);
        String seatChannelUUID = userRedisCache.getAgentChannId(seat);
        String reqChannelHost = userRedisCache.getUserChannHost(seat);

        EActionType second = userRedisCache.getAgentRequestType(seat);
        String core_uuid = userRedisCache.getAgentServiceUUID(seat);
//		EventCommandObject obj = new EventCommandObject();
        EventForCC obj = new EventForCC();

        EventParam eventParam=createTeleEventParam(seat);
        eventParam.setRingType(second);
        eventParam.setEventDesc("综合接续取消成功");

        String agentRequestId = userRedisCache.getAgentRequestId(seat);
        String target_name = userRedisCache.getTargetName(seat);
        switch (second) {
            case HELP_SEAT:
                if (userRedisCache.getAgentAudioState(target_name).value() < ESeatMediaState.WAITCON.value()) {
//				obj.setHost(reqChannelHost);
//				obj.setEventName("CUSTOM");
//				obj.putHeader("session_id", agentRequestId);
//				obj.setEventSubClass("cc::help_req");
//				obj.putHeader("target_agent", target_name);
//				obj.putHeader("source_name", seat);
//				obj.putHeader("source_uuid", seatChannelUUID);
//				obj.putHeader("action", "cancel");
                    //eslSupport.sendEventCommand(obj);
//				rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
                    obj.setEventName("CUSTOM");
                    obj.setEventSubClass("cc::help_req");
                    obj.setHost(reqChannelHost);
                    obj.setFreeSWITCH_Hostname(reqChannelHost);
                    obj.setSource_name(seat);
                    obj.setTarget_agent(target_name);
                    obj.setSession_id(agentRequestId);
                    obj.setSource_uuid(seatChannelUUID);
                    obj.setAction("cancel");
                    rabbitmqSender.sendDataToCC(reqChannelHost,"cc::help_req","",JSONObject.toJSONString(obj));
                }else{
                    setSeatToIdle(target_name);
                }
                userRedisCache.setAgentRequestType(seat, EActionType.NONE);
//			client.sendEvent(StaticValue.onRequestCancel, eventParam);
                break;
            case HELP_OUTSITE:{
                String targetHOST = userRedisCache.getTargetHost(seat);
                String targetUUID = userRedisCache.getTargetChannId(seat);
                eslSupport.uuid_kill(targetHOST, targetUUID);
//			client.sendEvent(StaticValue.onRequestCancel, eventParam);
                break;
            }
            case TRUNS_SEAT:{
//			obj.setHost(reqChannelHost);
//			obj.setEventName("CUSTOM");
//			obj.setEventSubClass("cc::trans_req");
//			obj.putHeader("target_agent", target_name);
//			obj.putHeader("source_name", seat);
//			obj.putHeader("source_uuid", seatChannelUUID);
//			obj.putHeader("session_id", agentRequestId);
//			obj.putHeader("action", "cancel");
                //eslSupport.sendEventCommand(obj);
//			rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));

                obj.setEventName("CUSTOM");
                obj.setEventSubClass("cc::trans_req");
                obj.setHost(reqChannelHost);
                obj.setFreeSWITCH_Hostname(reqChannelHost);
                obj.setSource_name(seat);
                obj.setTarget_agent(target_name);
                obj.setSession_id(agentRequestId);
                obj.setSource_uuid(seatChannelUUID);
                obj.setAction("cancel");
                rabbitmqSender.sendDataToCC(reqChannelHost,"cc::trans_req","",JSONObject.toJSONString(obj));
                userRedisCache.setAgentRequestType(seat, EActionType.NONE);
                break;
            }
            case TRUNS_OUTSITE:{
                String targetHOST = userRedisCache.getTargetHost(seat);
                String targetUUID = userRedisCache.getTargetChannId(seat);
                eslSupport.uuid_kill(targetHOST, targetUUID);
//			rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
                rabbitmqSender.sendDataToCC(reqChannelHost,"cc::trans_req","",JSONObject.toJSONString(obj));
                userRedisCache.setAgentRequestType(seat, EActionType.NONE);
                break;
            }
            default:
                eventParam.setEventDesc("综合接续请求不存在");
                client.sendEvent(StaticValue.onRequestCancelFail, eventParam);
        }
    }

    @Async
    @Override
    public void release(String seat,String sessionId,int accessType) {
        EventParam eventParam=createTeleEventParam(seat);
        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);
        EActionType firstType = userRedisCache.getRingType(seat);

        LOG.debug("\n\nhuangqb add for test:" +
                "\nseat:" + seat +
                "\nsessionId:" + sessionId +
                "accessType:" + accessType + "\n\n");

        if (EActionType.NONE.equals(firstType)) {
            setSeatToIdle(seat);
            eventParam.setEventDesc("坐席没有任何服务请求");
            client.sendEvent(StaticValue.onReleaseFail, eventParam);
            return;
        }
        switch (firstType) {
            case SERVICE:// 队列服务请求
                // 以及处理向外的请求
                releaseSERVICE(seat,sessionId);
                break;
            case MONITOR:
                // 清除监控
                String target = userRedisCache.getTargetName(seat);
                if (seat.equals(userRedisCache.getMonitorID(target))) {
                    userRedisCache.setMonitorID(target, null);
                    userRedisCache.setMonitorType(target, EMonitorType.NONE);
                }
                monitor_list.remove(target);
                setSeatToIdle(seat);
                eventParam.setTargetId(target);
                eventParam.setEventDesc("取消监控");
                client.sendEvent(StaticValue.onCleanMonitor, eventParam);
                client.sendEvent(StaticValue.onReleaseSuccess, eventParam);
                break;

            // 被请求
            case HELP_SEAT:
                releaseHELP_SEAT(seat,sessionId);
                break;
            case TRUNS_SEAT:
                releaseTRUNS_SEAT(seat,sessionId);
                break;
            case INNER_CALL:
                releaseINNER(seat,sessionId);
                break;
            case DIRECT_CALL:
                setSeatToIdle(seat);
                eventParam.setEventDesc("释放成功");
                client.sendEvent(StaticValue.onReleaseSuccess, eventParam);
                break;
            default:
                eventParam.setEventDesc("坐席没有任何服务请求");
                client.sendEvent(StaticValue.onReleaseFail,eventParam );
                break;
        }

    }

    @Async
    @Override
    public void transferToSeat(String accessType,String agent, String sessionId, String target, String type) {

        UUID uuid = UUID.fromString(seat_list.get(agent));
        SocketIOClient client = server.getClient(uuid);

        String seatChannelHost = userRedisCache.getAgentChannHost(agent);
        String seatChannelUUID = userRedisCache.getAgentChannId(agent);

        String reqChannelUUID = eslSupport.uuid_getvar(seatChannelHost, seatChannelUUID, "bridge_uuid");
        EActionType ringType = userRedisCache.getRingType(agent);

        //huangqb add for save original number 2019-4-15
        String original_number = userRedisCache.getSourceName(agent);
        userRedisCache.setOriginalNumber(target,original_number);
        //end of huangqb modify 2019-4-15

        EventParam eventParam=createTeleEventParam(agent);
        eventParam.setTargetId(target);


        if (EActionType.NONE.equals(ringType)) {
            eventParam.setEventDesc("当前坐席没有服务请求");
            client.sendEvent(StaticValue.onTransToSeatFail, eventParam);
            return;
        }

        if (EActionType.MONITOR.equals(ringType)) {
            eventParam.setEventDesc("正在监听其他坐席");
            client.sendEvent(StaticValue.onTransToSeatFail,eventParam );
            return;
        }
        if (EActionType.HELP_SEAT.equals(ringType)) {
            eventParam.setEventDesc("失败：正在被求助状态");
            client.sendEvent(StaticValue.onTransToSeatFail,eventParam );
            return;
        }
        EActionType second = userRedisCache.getAgentRequestType(agent);
        if (!EActionType.NONE.equals(second)) {
            eventParam.setEventDesc("有其他请求正在进行");
            client.sendEvent(StaticValue.onTransToSeatFail, eventParam);
            return;
        }

        if ((userRedisCache.getAgentState(target)).value() > ESeatState.free.value()) {
            eventParam.setEventDesc("目标坐席忙");
            client.sendEvent(StaticValue.onTransToSeatFail, eventParam);
            return;
        }

        if (!eslSupport.uuid_exists(seatChannelHost, reqChannelUUID)) {
            eventParam.setEventDesc("服务的对象不存在");
            client.sendEvent(StaticValue.onTransToSeatFail,eventParam );
            return;
        }

        if (!eslSupport.uuid_exists(seatChannelHost, seatChannelUUID)) {
            eventParam.setEventDesc("坐席通话不存在");
            client.sendEvent(StaticValue.onTransToSeatFail, eventParam);
            return;
        }
        String bridge_uuid = eslSupport.uuid_getvar(seatChannelHost, seatChannelUUID, "bridge_uuid");
        if (!reqChannelUUID.equals(bridge_uuid)) {
            eventParam.setEventDesc("当前坐席没有对用户进行服务");
            client.sendEvent(StaticValue.onTransToSeatFail, eventParam);
            return;
        }

        String targetChannHost = userRedisCache.getAgentChannHost(target);
        String targetChannId = userRedisCache.getAgentChannId(target);
        EActionType targetRingType = userRedisCache.getRingType(target);
        ESeatMediaState targetAudio = userRedisCache.getAgentAudioState(target);
        String core_uuid = userRedisCache.getAgentServiceUUID(target);

        eslSupport.uuid_play_tts_voice(seatChannelHost,seatChannelUUID,"转接中，请稍后");
        if (!EActionType.NONE.equals(targetRingType)) {
            eventParam.setEventDesc("目标坐席忙");
            client.sendEvent(StaticValue.onTransToSeatFail, eventParam);
            eslSupport.uuid_play_tts_voice(seatChannelHost,seatChannelUUID,"坐席忙，当前话务员重新为您服务");
            return;
        }
        if (!ESeatMediaState.IDLE.equals(targetAudio)) {
            eventParam.setEventDesc("目标坐席忙");
            client.sendEvent(StaticValue.onTransToSeatFail, eventParam);
            eslSupport.uuid_play_tts_voice(seatChannelHost,seatChannelUUID,"坐席忙，当前话务员重新为您服务");
            return;
        }
        if (eslSupport.uuid_exists(targetChannHost, targetChannId)) {
            eventParam.setEventDesc("目标坐席忙");
            client.sendEvent(StaticValue.onTransToSeatFail, eventParam);
            eslSupport.uuid_play_tts_voice(seatChannelHost,seatChannelUUID,"坐席忙，当前话务员重新为您服务");
            return;
        }
        //LOG.debug("------------->转接客服，转接中，请稍后<-----------------targetChannHost:" +targetChannHost + "seatChannelUUID:" + seatChannelUUID);


        if ("0".equals(type)) {
            String agentRequestId=UUID.randomUUID().toString();
//			EventCommandObject obj = new EventCommandObject();
//			obj.setHost(seatChannelHost);
//			obj.setEventName("CUSTOM");
//			obj.setEventSubClass("cc::trans_req");
//			obj.putHeader("session_id",agentRequestId );
//			obj.putHeader("target_agent", target);
//			obj.putHeader("unique_id", reqChannelUUID);
//			obj.putHeader("source_name", agent);
//			obj.putHeader("source_uuid", seatChannelUUID);
//			obj.putHeader("type", type);
            //eslSupport.sendEventCommand(obj);
//			rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
            EventForCC obj = new EventForCC();
            obj.setEventName("CUSTOM");
            obj.setEventSubClass("cc::trans_req");
            obj.setHost(seatChannelHost);
            obj.setFreeSWITCH_Hostname(seatChannelHost);
            obj.setSource_name(agent);
            obj.setTarget_agent(target);
            obj.setUnique_id(reqChannelUUID);
            obj.setSession_id(agentRequestId);
            obj.setSource_uuid(seatChannelUUID);
            obj.setType(type);
            rabbitmqSender.sendDataToCC(seatChannelHost,"cc::trans_req",reqChannelUUID,JSONObject.toJSONString(obj));
            eslSupport.uuid_setvar(seatChannelHost, reqChannelUUID, "ai_target_agent",target);
            eslSupport.uuid_transfer(seatChannelHost, reqChannelUUID, musicOnHold);
            setSeatToIdle(agent);
            eventParam.setEventDesc("释放转坐席成功");
            client.sendEvent(StaticValue.onTransToSeatSuccess, eventParam);
            client.sendEvent(StaticValue.onReleaseSuccess, eventParam);
            return;
        } else {
            // 发送请求
            String agentRequestId=UUID.randomUUID().toString();
//			EventCommandObject obj = new EventCommandObject();
//			obj.setHost(userRedisCache.getUserChannHost(agent));
//			obj.setEventName("CUSTOM");
//			obj.setEventSubClass("cc::trans_req");
//			obj.putHeader("session_id", agentRequestId);
//			obj.putHeader("target_agent", target);
//			obj.putHeader("unique_id", reqChannelUUID);
//			obj.putHeader("source_name", agent);
//			obj.putHeader("source_uuid", seatChannelUUID);
            //eslSupport.sendEventCommand(obj);
//			rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
            EventForCC obj = new EventForCC();
            obj.setEventName("CUSTOM");
            obj.setEventSubClass("cc::trans_req");
            obj.setHost(userRedisCache.getUserChannHost(agent));
            obj.setFreeSWITCH_Hostname(userRedisCache.getUserChannHost(agent));
            obj.setSource_name(agent);
            obj.setTarget_agent(target);
            obj.setUnique_id(reqChannelUUID);
            obj.setSession_id(agentRequestId);
            obj.setSource_uuid(seatChannelUUID);
            rabbitmqSender.sendDataToCC(userRedisCache.getUserChannHost(agent),"cc::trans_req",reqChannelUUID,JSONObject.toJSONString(obj));

            userRedisCache.setAgentRequestType(agent, EActionType.TRUNS_SEAT);
            userRedisCache.setAgentRequestId(agent,agentRequestId);
            userRedisCache.setTargetName(agent, target);
            eventParam.setEventDesc("成功转坐席进行中");
            client.sendEvent(StaticValue.onTransToSeatProcess, eventParam);
            // 压入超时队列
        }
    }

    @Async
    @Override
    public void transferToAuto(String seat,String sessionId, String desc, String type) {

        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);


        String seatChannelHost = userRedisCache.getAgentChannHost(seat);
        String seatChannelUUID = userRedisCache.getAgentChannId(seat);
        String reqChannelUUID = eslSupport.uuid_getvar(seatChannelHost, seatChannelUUID, "bridge_uuid");
        EActionType first = userRedisCache.getRingType(seat);
        EventParam eventParam=createTeleEventParam(seat);
        Integer iresult = 0;
        String operation_result = "";
        eventParam.setTargetId(desc);

        /******************************************************
         * 解决bug：
         * bug描述：
         *    点击拨号按钮，输入号码进行拨号，振铃过程中点击挂机按钮，
         *      提示自助流转失败，无法正常挂机
         * 修改人：huangqb@2019-4-11
         ******************************************************* */
        if ("".equals(reqChannelUUID))
        {
            eventParam.setEventDesc("主动挂机中");
            client.sendEvent(StaticValue.onTransToAutoFail, eventParam);
            eslSupport.uuid_kill(seatChannelHost,seatChannelUUID);
            return;
        }
        //end of huangqb@2019-4-11 modify

        if (EActionType.MONITOR.equals(first)) {
            eventParam.setEventDesc("坐席正在监听中");
            client.sendEvent(StaticValue.onTransToAutoFail, eventParam);
            LOG.debug("\n\n huangqb add for test:transferToAuto :坐席正在监听中\n\n");
            return;
        }
        if (EActionType.HELP_SEAT.equals(first)) {
            eventParam.setEventDesc("失败：正在被求助状态");
            client.sendEvent(StaticValue.onTransToAutoFail,eventParam );
            LOG.debug("\n\n huangqb add for test:transferToAuto :失败：正在被求助状态\n\n");
            return;
        }
        if (EActionType.NONE.equals(first)) {
            eventParam.setEventDesc("坐席不在服务中");
            client.sendEvent(StaticValue.onTransToAutoFail, eventParam);
            LOG.debug("\n\n huangqb add for test:transferToAuto :失败：坐席不在服务中1\n\n");
            return;
        }
        EActionType second = userRedisCache.getAgentRequestType(seat);
        if (!EActionType.NONE.equals(second)) {
            eventParam.setEventDesc("坐席不在服务中");
            client.sendEvent(StaticValue.onTransToAutoFail,eventParam);
            LOG.debug("\n\n huangqb add for test:transferToAuto :失败：坐席不在服务中2\n\n");
            return;
        }


        if (!eslSupport.uuid_exists(seatChannelHost, reqChannelUUID)) {
            eventParam.setEventDesc("服务的对象不存在");
            client.sendEvent(StaticValue.onTransToAutoFail, eventParam);
            LOG.debug("\n\n huangqb add for test:transferToAuto :服务的对象不存在\n\n");
            return;
        }

        if (!eslSupport.uuid_exists(seatChannelHost, seatChannelUUID)) {
            eventParam.setEventDesc("坐席通话不存在");
            client.sendEvent(StaticValue.onTransToAutoFail, eventParam);
            LOG.debug("\n\n huangqb add for test:transferToAuto :坐席通话不存在\n\n");
            return;
        }

        if ("0".equals(type)) {
            //bug:转ivr流程失败,huangqb@2019-4-16 modify
            //if (eslSupport.uuid_transfer(seatChannelHost, seatChannelUUID, "-bleg " +  desc )) {
            String bridge_uuid = eslSupport.uuid_getvar(seatChannelHost, seatChannelUUID, "bridge_uuid");
            if (eslSupport.uuid_transfer(seatChannelHost, seatChannelUUID, "-bleg " + "ivr:"+ desc )) {
                eslSupport.uuid_setvar(seatChannelHost, bridge_uuid, "transfer_to_auto_return", "");
                // 释放转
                setSeatToIdle(seat);
                eventParam.setEventDesc("释放转自助成功");
                client.sendEvent(StaticValue.onTransToAutoSuccess, eventParam);
            } else {
                eventParam.setEventDesc("话路转移失败");
                client.sendEvent(StaticValue.onTransToAutoFail, eventParam);
            }
        } else {
            String bridge_uuid = eslSupport.uuid_getvar(seatChannelHost, seatChannelUUID, "bridge_uuid");
            eslSupport.uuid_setvar(seatChannelHost, bridge_uuid, "transfer_to_auto_return", seatChannelUUID);
            // eslSupport.uuid_setvar(seatChannelHost, bridge_uuid,
            // "follow_data", desc);
            //huangqb modify 2019-4-16 ,bug:转ivr失败...
            //if (eslSupport.uuid_dual_transfer(seatChannelHost, seatChannelUUID, musicOnHold, desc)) {
            if (eslSupport.uuid_dual_transfer(seatChannelHost, seatChannelUUID, musicOnHold, "ivr:"+ desc)) {
                userRedisCache.setAgentRequestType(seat, EActionType.TRUNS_AUTO);
                userRedisCache.setAgentAudioState(seat, ESeatMediaState.WAITRET);

                eventParam.setEventDesc("释放转自助成功");
                client.sendEvent(StaticValue.onTransToAutoSuccess, eventParam);
            } else {
                eventParam.setEventDesc("话路转移失败");
                client.sendEvent(StaticValue.onTransToAutoFail, eventParam);
            }
        }
        // AGENT::TRANS_AUTO
        AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
        agentStateLogModel.setEventName("CUSTOM");
        agentStateLogModel.setEventSubclass("AGENT::TRANS_AUTO");
        agentStateLogModel.setResult(iresult);
        agentStateLogModel.setEventContent(operation_result);
        agentStateLogModel.setAgnetId(seat);
        agentStateLogModel.setAgentSignId(UUID.fromString(seat_list.get(seat)).toString());
        agentStateLogModel.setTargetId(desc);
        //agentStateLogModel.setTargetSignId(uuid.toString());
        agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
        agentStateLogModel.setOperTime(System.currentTimeMillis());
        agentStateLogModel.setTargetChatId(userRedisCache.getSessionId(seat));
        agentStateLogModel.setChatId(seatChannelUUID);
        rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));

    }

    @Async
    @Override
    public void transferToQueue(String accessType,String seat,String sessionId, String descQueue) {

        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);
        String seatChannelHost = userRedisCache.getAgentChannHost(seat);
        String seatChannelUUID = userRedisCache.getAgentChannId(seat);
        String reqChannelUUID = eslSupport.uuid_getvar(seatChannelHost, seatChannelUUID, "bridge_uuid");
        EActionType first = userRedisCache.getRingType(seat);

        EventParam eventParam=createTeleEventParam(seat);

        if (EActionType.MONITOR.equals(first)) {
            eventParam.setEventDesc("坐席正在监听中");
            client.sendEvent(StaticValue.onTransToQueueFail, eventParam);
            return;
        }

        if (EActionType.HELP_SEAT.equals(first)) {
            eventParam.setEventDesc("失败：正在被求助状态");
            client.sendEvent(StaticValue.onTransToQueueFail,eventParam );
            return;
        }
        if (EActionType.NONE.equals(first)) {
            eventParam.setEventDesc("坐席不在服务中");
            client.sendEvent(StaticValue.onTransToQueueFail, eventParam);
            return;
        }
        EActionType second = userRedisCache.getAgentRequestType(seat);
        if (!EActionType.NONE.equals(second)) {
            eventParam.setEventDesc("有其他请求正在进行");
            client.sendEvent(StaticValue.onTransToQueueFail, eventParam);
            return;
        }

        if (!eslSupport.uuid_exists(seatChannelHost, reqChannelUUID)) {
            eventParam.setEventDesc("服务的对象不存在");
            client.sendEvent(StaticValue.onTransToQueueFail, eventParam);
            return;
        }

        if (!eslSupport.uuid_exists(seatChannelHost, seatChannelUUID)) {
            eventParam.setEventDesc("坐席通话不存在");
            client.sendEvent(StaticValue.onTransToQueueFail, eventParam);
            return;
        }

        if (eslSupport.uuid_transfer(seatChannelHost, seatChannelUUID, "-bleg " + "request_queue" + descQueue)) {
            // 释放转
            setSeatToIdle(seat);
            eventParam.setEventDesc("转队列成功");
            client.sendEvent(StaticValue.onTransToQueueSuccess,eventParam);
        } else {
            eventParam.setEventDesc("转队列失败");
            client.sendEvent(StaticValue.onTransToQueueFail,eventParam);
        }
    }

    @Async
    @Override
    public void transferToOutsite(String seat,String sessionId, String desc, String type) {
        // String date = desc + ":" + type;
        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);
        ESeatMediaState audioState = userRedisCache.getAgentAudioState(seat);
        EActionType first = userRedisCache.getRingType(seat);
        String operation_result = ""; //log文字描述...
        Integer iresult = 0 ; //求助结果,枚举描述...

        EventParam eventParam=createTeleEventParam(seat);
        eventParam.setTargetId(desc);

        if (EActionType.MONITOR.equals(first)) {
            eventParam.setEventDesc("坐席正在监听中");
            client.sendEvent(StaticValue.onTransToOutFail, eventParam);
            return;
        }

        if (EActionType.HELP_SEAT.equals(first)) {
            eventParam.setEventDesc("失败：正在被求助状态");
            client.sendEvent(StaticValue.onTransToOutFail,eventParam );
            return;
        }
        if (EActionType.NONE.equals(first)) {
            eventParam.setEventDesc("坐席不在服务中");
            client.sendEvent(StaticValue.onTransToOutFail, eventParam);
            return;
        }
        EActionType second = userRedisCache.getAgentRequestType(seat);
        if (!EActionType.NONE.equals(second)) {
            eventParam.setEventDesc("有其他请求正在进行");
            client.sendEvent(StaticValue.onTransToOutFail, eventParam);
            return;
        }
        if (ESeatMediaState.ANSWER.value() > audioState.value()) {

            eventParam.setEventDesc("坐席不在通话中");
            client.sendEvent(StaticValue.onTransToOutFail,eventParam );
            return;
        }
        String chanHost = userRedisCache.getAgentChannHost(seat);
        String chanUUID = userRedisCache.getAgentChannId(seat);
        String reqChannelHost = userRedisCache.getUserChannHost(seat);
        String reqChannelUUID = userRedisCache.getUserChannId(seat);
        String target_uuid_log = "";
        eslSupport.uuid_play_tts_voice(chanHost,chanUUID,"正在为您转接，请稍后");

        if ("0".equals(type)) {
            // uuid_transfer 1126034e-017c-11e4-9417-2994f1b54fbf -bleg
            // 'm:^:bridge:{origination_caller_id_name=1377XXX4955,origination_caller_id_number=1377XXX4955}sofia/gateway/fsctigw/1377XXX4966' inline
            //String param = "-bleg  'm:^:bridge:" + String.format(outboundFormat, desc) + "' inline";
            client.sendEvent(StaticValue.onTransToOutProcess, eventParam);
            //String broad_param = "transfer.wav inline";
            //if (eslSupport.uuid_broadcast(chanHost,chanUUID,broad_param)){
            //	LOG.debug("\n\nhuangqb add for test play voice successful....broad_param:" + param + "\n\n");
            //}

            String param = "-bleg  'm:^:bridge:" + String.format(outboundFormat, desc) + "' inline"; //modify for test....
            //String param = "-bleg  'm:^:bridge:" + String.format(outboundFormat, desc) +"@10.21.19.22:5080"+ "' inline";
            eslSupport.uuid_setvar(reqChannelHost, reqChannelUUID, "ai_target_agent", ""); //设置不放音...
            if (eslSupport.uuid_transfer(chanHost, chanUUID, param)) {
                setSeatToIdle(seat);
                eventParam.setEventDesc("转移到外部成功");
                client.sendEvent(StaticValue.onTransToOutSuccess, eventParam);
                operation_result = "转移到外部成功";
                target_uuid_log = chanUUID;
                iresult = StaticValue.RESULT_SUCCESS;
            } else {
                eventParam.setEventDesc("转移失败");
                client.sendEvent(StaticValue.onTransToOutFail, eventParam);
                operation_result = "转移到外部失败";
                iresult = StaticValue.RESULT_FAIL;
            }
        } else {
            userRedisCache.setAgentRequestType(seat, EActionType.TRUNS_OUTSITE);

            String targetUUID = UUID.randomUUID().toString();
            userRedisCache.setTargetName(seat, desc);
            userRedisCache.setTargetHost(seat, chanHost);
            userRedisCache.setTargetChannId(seat, targetUUID);
//			String param = "{origination_uuid=" + targetUUID + "}{"+StaticValue.RingType+"="+ESeatActionType.TRUNS_OUTSITE.name()+"}"
//					+ String.format(outboundFormat, desc) + " " + musicOnHold;
            eslSupport.uuid_setvar(reqChannelHost, reqChannelUUID, "ai_target_agent", ""); //设置不放音...
            String param = "{origination_uuid=" + targetUUID + "}"+ String.format(outboundFormat, desc) + " " + musicOnHold;
            String result = eslSupport.originate(chanHost, param);
            if (result.startsWith("+OK")) {
                String bridge_uuid = eslSupport.uuid_getvar(chanHost, chanUUID, "bridge_uuid");
                if (eslSupport.uuid_bridge(chanHost, bridge_uuid, targetUUID)) {
                    setSeatToIdle(seat);
                    eventParam.setEventDesc("转移到外部成功");
                    client.sendEvent(StaticValue.onTransToOutSuccess, eventParam);
                    operation_result = "转移到外部成功";
                    iresult = StaticValue.RESULT_SUCCESS;
                } else {
                    eslSupport.uuid_kill(chanHost, targetUUID);
                    eventParam.setEventDesc("转移失败");
                    client.sendEvent(StaticValue.onTransToOutFail, eventParam);
                    operation_result = "转移到外部失败";
                    iresult = StaticValue.RESULT_FAIL;
                }
            } else {

                userRedisCache.setAgentRequestType(seat, EActionType.NONE);
                if (!result.startsWith("-ERR NORMAL_CLEARING")){
                    eventParam.setEventDesc("呼叫外部电话失败");
                    client.sendEvent(StaticValue.onTransToOutFail, eventParam);
                    operation_result = "呼叫外部电话失败";
                    iresult = StaticValue.RESULT_FAIL;
                }
                eslSupport.uuid_play_tts_voice(chanHost,chanUUID,"转接失败，当前话务员重新为您服务");
            }
            target_uuid_log = targetUUID;

        }
        //增加log部分，huangqb 2019-4-29...
        AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
        agentStateLogModel.setEventName("CUSTOM");
        agentStateLogModel.setEventSubclass("AGENT::TRANS_OUTSITE");
        agentStateLogModel.setResult(iresult);
        agentStateLogModel.setTranstype(type);
        agentStateLogModel.setEventContent(operation_result);
        agentStateLogModel.setAgnetId(seat);
        agentStateLogModel.setAgentSignId(uuid.toString());
        agentStateLogModel.setTargetId(desc);
        agentStateLogModel.setTargetSignId(target_uuid_log);
        agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
        agentStateLogModel.setOperTime(System.currentTimeMillis());
        agentStateLogModel.setTargetChatId(userRedisCache.getSessionId(seat));
        agentStateLogModel.setChatId(userRedisCache.getSourceChannId(seat));
        rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
        //LOG.debug("------------------------->转接外部电话<------------------------------\n\n");
    }

    @Async
    @Override
    public void helpToSeat(String seat,String sessionId, String desc) {

        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);

        String seatChannelHost = userRedisCache.getAgentChannHost(seat);
        String seatChannelUUID = userRedisCache.getAgentChannId(seat);
        String reqChannelHost = userRedisCache.getUserChannHost(seat);
        String reqChannelUUID = userRedisCache.getUserChannId(seat);
        EActionType first = userRedisCache.getRingType(seat);
        //huangqb add 2019-4-15
		/*
		  保留原始的用户服务号码...
		 */
        String source_name = userRedisCache.getSourceName(seat);
        userRedisCache.setOriginalNumber(desc,source_name);
        //end of huangqb modify 2019-4-15...

        EventParam eventParam=createTeleEventParam(seat);
        eventParam.setTargetId(desc);


        if ((userRedisCache.getAgentState(desc)).value() > ESeatState.free.value()) {
            eventParam.setEventDesc("坐席不在空闲");
            client.sendEvent(StaticValue.onHelpToSeatFail, eventParam);
            return;
        }


        if (EActionType.MONITOR.equals(first)) {
            eventParam.setEventDesc("坐席正在监听中");
            client.sendEvent(StaticValue.onHelpToSeatFail, eventParam);
            return;
        }

        if (EActionType.HELP_SEAT.equals(first)) {
            eventParam.setEventDesc("失败：正在被求助状态");
            client.sendEvent(StaticValue.onHelpToSeatFail,eventParam );
            return;
        }

        if (EActionType.NONE.equals(first)) {
            eventParam.setEventDesc("坐席不在服务中");
            client.sendEvent(StaticValue.onHelpToSeatFail, eventParam);
            return;
        }
        EActionType second = userRedisCache.getAgentRequestType(seat);
        if (!EActionType.NONE.equals(second)) {
            eventParam.setEventDesc("有其他请求正在进行");
            client.sendEvent(StaticValue.onHelpToSeatFail,eventParam );
            return;
        }
        if (!eslSupport.uuid_exists(seatChannelHost, seatChannelUUID)) {
            eventParam.setEventDesc("没有在通话中");
            client.sendEvent(StaticValue.onHelpToSeatFail,eventParam );
            return;
        }

        if (!eslSupport.uuid_exists(reqChannelHost, reqChannelUUID)) {
            eventParam.setEventDesc("没有在通话中");
            client.sendEvent(StaticValue.onHelpToSeatFail, eventParam);
            return;
        }

        String targetChaneHost = userRedisCache.getAgentChannHost(desc);
        String targetChanUUID = userRedisCache.getAgentChannId(desc);
        EActionType targetFirst = userRedisCache.getRingType(desc);
        ESeatMediaState targetAudio = userRedisCache.getAgentAudioState(desc);
        String core_uuid = userRedisCache.getAgentServiceUUID(desc);

        if (!EActionType.NONE.equals(targetFirst)) {
            eventParam.setEventDesc( "目标坐席忙");
            client.sendEvent(StaticValue.onHelpToSeatFail,eventParam);
            return;
        }
        if (!ESeatMediaState.IDLE.equals(targetAudio)) {
            eventParam.setEventDesc( "目标坐席忙");
            client.sendEvent(StaticValue.onHelpToSeatFail, eventParam);
            return;
        }
        if (eslSupport.uuid_exists(targetChaneHost, targetChanUUID)) {
            eventParam.setEventDesc( "目标坐席忙");
            client.sendEvent(StaticValue.onHelpToSeatFail, eventParam);
            return;
        }

        client.sendEvent(StaticValue.onHelpToSeatProcess, eventParam);
        // 发送请求
        String agentRequestId=UUID.randomUUID().toString();
//		EventCommandObject obj = new EventCommandObject();
//		obj.setHost(userRedisCache.getUserChannHost(seat));
//		obj.setEventName("CUSTOM");
//		obj.setEventSubClass("cc::help_req");
//		obj.putHeader("session_id", agentRequestId);
//		obj.putHeader("target_agent", desc);
//		obj.putHeader("source_name", seat);
//		obj.putHeader("source_uuid", seatChannelUUID);
        //eslSupport.sendEventCommand(obj);
//		rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
        EventForCC obj = new EventForCC();
        obj.setEventName("CUSTOM");
        obj.setEventSubClass("cc::help_req");
        obj.setHost(userRedisCache.getUserChannHost(seat));
        obj.setFreeSWITCH_Hostname(userRedisCache.getUserChannHost(seat));
        obj.setSource_name(seat);
        obj.setTarget_agent(desc);
        obj.setSession_id(agentRequestId);
        obj.setSource_uuid(seatChannelUUID);
        rabbitmqSender.sendDataToCC(userRedisCache.getUserChannHost(seat),"cc::help_req","",JSONObject.toJSONString(obj));

        userRedisCache.setAgentRequestType(seat, EActionType.HELP_SEAT);
        userRedisCache.setAgentRequestId(seat,agentRequestId);
        userRedisCache.setTargetName(seat, desc);
        userRedisCache.setUserChannHost(desc,userRedisCache.getUserChannHost(seat));
    }

    @Async
    @Override
    public void helpToOutsite(String seat,String sessionId, String desc) {
        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);

        String seatChannelHost = userRedisCache.getAgentChannHost(seat);
        String seatChannelUUID = userRedisCache.getAgentChannId(seat);
        String reqChannelHost = userRedisCache.getUserChannHost(seat);
        String reqChannelUUID = userRedisCache.getUserChannId(seat);
        EActionType second = userRedisCache.getAgentRequestType(seat);
        EActionType first = userRedisCache.getRingType(seat);
        String core_uuid = userRedisCache.getAgentServiceUUID(seat);

        EventParam eventParam=createTeleEventParam(seat);
        eventParam.setTargetId(desc);


        if (EActionType.MONITOR.equals(first)) {
            eventParam.setEventDesc("坐席正在监听中");
            client.sendEvent(StaticValue.onHelpToOutsiteFail,eventParam);
            return;
        }

        if (EActionType.HELP_SEAT.equals(first)) {
            eventParam.setEventDesc("失败：正在被求助状态");
            client.sendEvent(StaticValue.onHelpToOutsiteFail,eventParam );
            return;
        }

        if (!EActionType.NONE.equals(second)) {
            eventParam.setEventDesc("有其他请求");
            client.sendEvent(StaticValue.onHelpToOutsiteFail,eventParam );
            return;
        }
        if (!eslSupport.uuid_exists(seatChannelHost, seatChannelUUID)) {
            eventParam.setEventDesc("没有在通话中");
            client.sendEvent(StaticValue.onHelpToOutsiteFail, eventParam);
            return;
        }

        if (!eslSupport.uuid_exists(reqChannelHost, reqChannelUUID)) {
            eventParam.setEventDesc("没有在通话中");
            client.sendEvent(StaticValue.onHelpToOutsiteFail, eventParam);
            return;
        }
        client.sendEvent(StaticValue.onHelpToOutsiteProcess, eventParam);
        userRedisCache.setAgentRequestType(seat, EActionType.HELP_OUTSITE);
        String targetUUID = UUID.randomUUID().toString();
        userRedisCache.setTargetName(seat, desc);
        userRedisCache.setTargetHost(seat, seatChannelHost);
        userRedisCache.setTargetChannId(seat, targetUUID);

//		String param = "{origination_uuid=" + targetUUID + "}{"+StaticValue.HelpSource+"=" + seat + "}{"+StaticValue.RingType+"="+ESeatActionType.HELP_OUTSITE.name()+"}"
//				+ String.format(outboundFormat, desc) + " " + musicOnHold;
        //String param = "{origination_uuid=" + seatChannelUUID +"}{origination_caller_id_number="+ sourceName + "}{"+ StaticValue.HelpSource+"=" + sourceName + "}{"+ StaticValue.SeatName+"=" + seat
        //		+ "}{"+ StaticValue.RingType+"="+ EActionType.HELP_SEAT.name()+"}" + channelName + " " + musicOnHold;

        String param = "{origination_uuid=" + targetUUID + "}{"+ StaticValue.HelpSource+"=" + seat + "}"
                + String.format(outboundFormat, desc) + " " + musicOnHold;
        String result = eslSupport.originate(seatChannelHost, param);

        if (result.startsWith("+OK")) {

            //huangqb add 2019-4-18
            try
            {
                Thread.sleep(2000);    //延时2秒
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }
            //end of huangqb add 2019-4-18
            eslSupport.uuid_att_xfer(seatChannelHost,seatChannelUUID,targetUUID);
//			APPCommandObject obj = new APPCommandObject();
//			obj.setHost(seatChannelHost);
//			obj.setUuid(seatChannelUUID);
//			obj.setAppName("att_xfer");
//			obj.setArg(targetUUID);
//			eslSupport.sendAppCommand(obj);
//			rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
            eventParam.setEventDesc("求助成功");
            client.sendEvent(StaticValue.onHelpToOutsiteSuccess,eventParam);
        } else {
            userRedisCache.setAgentRequestType(seat, EActionType.NONE);
            if (!result.startsWith("-ERR NORMAL_CLEARING"))
                eventParam.setEventDesc("呼叫外部失败");
            client.sendEvent(StaticValue.onHelpToOutsiteFail, eventParam);
            //增加对用户的转接失败放音...
           // String user_uuid = userRedisCache.getAgentUUID(seat).toString();
            if(eslSupport.uuid_broadcast(seatChannelHost,seatChannelUUID,"/usr/local/freeswitch/sounds/zh/cn/sinmei/ivr_56/trans_failed.wav"))
            {
                LOG.debug("------------------------->转接失败放音成功<---------------------\n\n");
            }
            else
            {
                LOG.debug("------------------------->转接失败放音失败<------------------------------\n\n");
            }
        }
        //增加求助外部的log..
        AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
        agentStateLogModel.setEventName("CUSTOM");
        agentStateLogModel.setEventSubclass("AGENT::HELP_OUTSITE");
        agentStateLogModel.setResult(result.startsWith("+OK")? StaticValue.RESULT_SUCCESS:StaticValue.RESULT_FAIL);
        agentStateLogModel.setEventContent(result.startsWith("+OK")? "求助外部成功":"呼叫外部失败");
        agentStateLogModel.setAgnetId(seat);
        agentStateLogModel.setAgentSignId(UUID.fromString(seat_list.get(seat)).toString());
        agentStateLogModel.setTargetId(desc);
        //agentStateLogModel.setTargetSignId(uuid.toString());
        agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
        agentStateLogModel.setOperTime(System.currentTimeMillis());
        agentStateLogModel.setTargetChatId(targetUUID);
        agentStateLogModel.setChatId(reqChannelUUID);
        rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
        LOG.debug("........发布求助外部成功消息......");
    }

    @Async
    @Override
    public void callSeat(String seat,String desc) {
        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);

        String seatChannelHost = userRedisCache.getAgentChannHost(seat);
        String seatChannelUUID = userRedisCache.getAgentChannId(seat);

        ESeatMediaState audioState = userRedisCache.getAgentAudioState(seat);
        //log部分
        //Integer iresult = 0;
        //String operation_result = "";

        EActionType first = userRedisCache.getRingType(seat);


        EventParam eventParam=createTeleEventParam(seat);
        eventParam.setTargetId(desc);

        if (EActionType.MONITOR.equals(first)) {
            eventParam.setEventDesc("坐席正在监听中");
            client.sendEvent(StaticValue.onCallSeatFail, eventParam);
            return;
        }

        if (!EActionType.NONE.equals(first)) {
            eventParam.setEventDesc("坐席不在空闲中");
            client.sendEvent(StaticValue.onCallSeatFail, eventParam);
            return;
        }

        if (!ESeatMediaState.IDLE.equals(audioState)) {
            eventParam.setEventDesc("坐席电话不空闲");
            client.sendEvent(StaticValue.onCallSeatFail, eventParam);
            return;
        }

        if (eslSupport.uuid_exists(seatChannelHost, seatChannelUUID)) {
            eventParam.setEventDesc("坐席电话不空闲");
            client.sendEvent(StaticValue.onCallSeatFail, eventParam);
            return;
        }

        String targetChaneHost = userRedisCache.getAgentChannHost(desc);
        String targetChanUUID = userRedisCache.getAgentChannId(desc);
        EActionType targetFirst = userRedisCache.getRingType(desc);
        ESeatMediaState targetAudio = userRedisCache.getAgentAudioState(desc);

        if (!EActionType.NONE.equals(targetFirst)) {
            eventParam.setEventDesc("目标坐席忙");
            client.sendEvent(StaticValue.onCallSeatFail, eventParam);
            return;
        }
        if (!ESeatMediaState.IDLE.equals(targetAudio)) {
            eventParam.setEventDesc("目标坐席忙");
            client.sendEvent(StaticValue.onCallSeatFail, eventParam);
            return;
        }
        if (eslSupport.uuid_exists(targetChaneHost, targetChanUUID)) {
            eventParam.setEventDesc("目标坐席忙");
            client.sendEvent(StaticValue.onCallSeatFail, eventParam);
            return;
        }
        client.sendEvent(StaticValue.onCallSeatProcess, eventParam);
        String sessionId = UUID.randomUUID().toString();
        userRedisCache.setSessionId(seat, sessionId);
        userRedisCache.setRingType(seat, EActionType.INNER_CALL);
        userRedisCache.setAgentAudioState(seat, ESeatMediaState.RING);
        userRedisCache.setUserName(seat, desc);
        // 发送请求

        for (String host : eslSupport.getInstanceList()) {

            String channelName = userRedisCache.getAgentChannelName(seat);
            seatChannelHost = host;
            seatChannelUUID = sessionId;

            userRedisCache.setAgentChannHost(seat, seatChannelHost);
            userRedisCache.setAgentChannId(seat, seatChannelUUID);

            String param = "{origination_uuid=" + seatChannelUUID + "}{"+ StaticValue.SeatName+"=" + seat + "}{"+ StaticValue.RingType+"="+ EActionType.INNER_CALL.name()+"}"
                    + channelName + " " + musicOnHold;
            String result = eslSupport.originate(seatChannelHost, param);
            if (result.startsWith("+OK")) {
                seatChannelUUID = result.split(" ")[1];
                userRedisCache.setAgentAudioState(seat, ESeatMediaState.ANSWER);
//				EventCommandObject obj = new EventCommandObject();
//				obj.setHost(seatChannelHost);
//				obj.setEventName("CUSTOM");
//				obj.setEventSubClass("cc::call_req");
//				obj.putHeader("target_agent", desc);
//				obj.putHeader("source_name", seat);
//				obj.putHeader("source_uuid", seatChannelUUID);
//				obj.putHeader("session_id", sessionId);
//				String core_uuid = userRedisCache.getAgentServiceUUID(seat);
                //eslSupport.sendEventCommand(obj);
//				rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
                EventForCC obj = new EventForCC();
                obj.setEventName("CUSTOM");
                obj.setEventSubClass("cc::call_req");
                obj.setHost(seatChannelHost);
                obj.setFreeSWITCH_Hostname(seatChannelHost);
                obj.setSource_name(seat);
                obj.setTarget_agent(desc);
                obj.setSession_id(sessionId);
                obj.setSource_uuid(seatChannelUUID);
                rabbitmqSender.sendDataToCC(seatChannelHost,"cc::call_req","",JSONObject.toJSONString(obj));
                userRedisCache.setTargetName(seat, desc);

            } else {
                eventParam.setEventDesc("呼叫坐席失败");
                client.sendEvent(StaticValue.onCallOutsiteFail,eventParam);
                setSeatToIdle(seat);
            }
            // AGENT:CALL_SEAT
            AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
            agentStateLogModel.setEventName("CUSTOM");
            agentStateLogModel.setEventSubclass("AGENT::CALL_SEAT");
            agentStateLogModel.setResult(result.startsWith("+OK")? StaticValue.RESULT_SUCCESS:StaticValue.RESULT_FAIL);
            agentStateLogModel.setEventContent(result.startsWith("+OK")? "":"");
            agentStateLogModel.setAgnetId(seat);
            agentStateLogModel.setAgentSignId(UUID.fromString(seat_list.get(seat)).toString());
            agentStateLogModel.setTargetId(desc);
            //agentStateLogModel.setTargetSignId(uuid.toString());
            agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
            agentStateLogModel.setOperTime(System.currentTimeMillis());
            agentStateLogModel.setTargetChatId(userRedisCache.getSessionId(seat));
            agentStateLogModel.setChatId(seatChannelUUID);
            rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
            break;
        }
    }

    @Async
    @Override
    public void callOutsite(String seat, String outsite) {
        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);
        LOG.debug("\n\n =============seat===================" +
                "\n seat:" + seat +
                "\n target:" + outsite );
        System.out.println("1111111222=="+seat+"==outsite=="+outsite);

        String seatChannelHost = userRedisCache.getAgentChannHost(seat);
        String seatChannelUUID = userRedisCache.getAgentChannId(seat);


        EventParam eventParam=createTeleEventParam(seat);
        eventParam.setTargetId(outsite);
        EActionType requestType = userRedisCache.getRingType(seat);
        if (EActionType.MONITOR.equals(requestType)) {
            eventParam.setEventDesc("坐席正在监听中");
            client.sendEvent(StaticValue.onCallOutsiteFail, eventParam);
            return;
        }

        ESeatMediaState audioState = userRedisCache.getAgentAudioState(seat);

        String reqChannelHost = userRedisCache.getUserChannHost(seat);
        String reqChannelUUID = userRedisCache.getUserChannId(seat);
        if (!EActionType.NONE.equals(requestType)) {
            eventParam.setEventDesc("坐席正在服务中");
            client.sendEvent(StaticValue.onCallOutsiteFail,eventParam);
            return;
        }

        if (!ESeatMediaState.IDLE.equals(audioState)) {
            eventParam.setEventDesc("坐席电话忙");
            client.sendEvent(StaticValue.onCallOutsiteFail, eventParam);
            return;
        }

        if (eslSupport.uuid_exists(seatChannelHost, seatChannelUUID)) {
            eventParam.setEventDesc("坐席电话忙");
            client.sendEvent(StaticValue.onCallOutsiteFail, eventParam);
            return;
        }
        client.sendEvent(StaticValue.onCallOutsiteProcess, eventParam);

        String sessionId = UUID.randomUUID().toString();
        userRedisCache.setSessionId(seat, sessionId);
        userRedisCache.setUserName(seat, outsite);

        userRedisCache.setRingType(seat, EActionType.DIRECT_CALL);
        userRedisCache.setAgentAudioState(seat, ESeatMediaState.RING);
        for (String host : eslSupport.getInstanceList()) {
            String channelName = userRedisCache.getAgentChannelName(seat);
            seatChannelHost = host;
            seatChannelUUID = sessionId;
            reqChannelUUID = UUID.randomUUID().toString();
            reqChannelHost = host;
            userRedisCache.setAgentChannHost(seat, seatChannelHost);
            userRedisCache.setAgentChannId(seat, seatChannelUUID);
            userRedisCache.setUserChannHost(seat, reqChannelHost);
            userRedisCache.setUserChannId(seat, reqChannelUUID);
            String param = "{origination_uuid=" + seatChannelUUID + "}{req_uuid="+reqChannelUUID+"}{origination_caller_id_number=" + seat + "}{"+ StaticValue.SeatName+"=" + seat + "}{"+ StaticValue.RingType+"="+ EActionType.DIRECT_CALL.name()+"}"
                    + channelName + " 'm:^:bridge:{origination_uuid=" + reqChannelUUID + "}"
                    + String.format(outboundFormat, outsite) + "' inline";

            System.out.println("1111111222paramparamparam=="+param);
            System.out.println("seatChannelHost=="+seatChannelHost);
            String result = eslSupport.originate(seatChannelHost, param);
            if (result.startsWith("+OK")) {
                userRedisCache.setAgentAudioState(seat, ESeatMediaState.ANSWER);
            } else {
                eventParam.setEventDesc( "呼叫坐席失败");
                client.sendEvent(StaticValue.onCallOutsiteFail,eventParam);
                setSeatToIdle(seat);
            }
            //增加外呼的log日志
            AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
            agentStateLogModel.setEventName("CUSTOM");
            agentStateLogModel.setEventSubclass("AGENT::CALL_OUTSITE");
            agentStateLogModel.setResult(result.startsWith("+OK")?StaticValue.RESULT_SUCCESS:StaticValue.RESULT_FAIL);
            agentStateLogModel.setEventContent(result.startsWith("+OK")? "外呼成功":"外呼失败");
            agentStateLogModel.setAgnetId(seat);
            agentStateLogModel.setAgentSignId(uuid.toString());
            agentStateLogModel.setTargetId(seat);
            agentStateLogModel.setTargetSignId(uuid.toString());
            agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
            agentStateLogModel.setOperTime(System.currentTimeMillis());
            agentStateLogModel.setTargetChatId(seatChannelUUID);
            agentStateLogModel.setChatId(reqChannelUUID);
            rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
            break;
        }
    }

    @Async
    @Override
    public void sendDTMF(String seat,String sessionId, String DTMF) {
        // TODO 向当前活动的信道发送DTMF
        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);
        String host = userRedisCache.getUserChannHost(seat);
        String channel = userRedisCache.getUserChannId(seat);
        eslSupport.uuid_send_dtmf(host, channel, DTMF);
        EventParam eventParam=createTeleEventParam(seat);
        eventParam.setValue(DTMF);
        client.sendEvent(StaticValue.onSendInfoSuccess, eventParam);
    }

    @Async
    @Override
    public void listen(String seat,String desc,String targetSessionId,String originalUserId) {
        // TODO 将要监控的坐席URI放到监控队列中，当这个URI建立起呼叫后，马上对其进行监听。
        // 如果监控队列已经存在这个目标且是当前坐席发起的监控 就发送DTMF，使其恢复到监听状态
        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);

        String seatChannelHost = userRedisCache.getAgentChannHost(seat);
        String seatChannelUUID = userRedisCache.getAgentChannId(seat);
        EActionType first = userRedisCache.getRingType(seat);
        EventParam eventParam=createTeleEventParam(seat);
        eventParam.setTargetId(desc);

        if (EActionType.MONITOR.equals(first) && eslSupport.uuid_exists(seatChannelHost, seatChannelUUID)) {
            eslSupport.uuid_recv_dtmf(seatChannelHost, seatChannelUUID, "0");
            eventParam.setEventDesc("恢复到监听");
            client.sendEvent(StaticValue.onListenSuccess, eventParam);
            return;
        }
        if (!EActionType.NONE.equals(first)) {
            eventParam.setEventDesc("有其他请求，禁止监听");
            client.sendEvent(StaticValue.onListenFail, eventParam);
            return;
        }

        EMonitorType monitorType = userRedisCache.getMonitorType(desc);
        String monitoID = userRedisCache.getMonitorID(desc);

        if (!EMonitorType.NONE.equals(monitorType) && userRedisCache.agentExists(monitoID) && !monitoID.equals(seat)) {
            eventParam.setEventDesc("目标已经被监听");
            client.sendEvent(StaticValue.onListenFail,eventParam);
            return;
        }

        monitor_list.put(desc, seat);
        userRedisCache.setUserName(seat, desc);
        userRedisCache.setTargetName(seat, desc);
        userRedisCache.setRingType(seat, EActionType.MONITOR);
        userRedisCache.setMonitorType(desc, EMonitorType.LISTEN);
        userRedisCache.setMonitorID(desc, seat);
        //设置监听SessionId
        String sessionId = UUID.randomUUID().toString();
        userRedisCache.setSessionId(seat, sessionId);
        String sourceNumber = userRedisCache.getSourceName(desc);
        userRedisCache.setUserName(seat,sourceNumber); //号码显示问题...2019-5-8
        userRedisCache.setAgentAudioState(seat, ESeatMediaState.RING);

        client.sendEvent(StaticValue.onListenSuccess,eventParam);
        Integer iresult = StaticValue.RESULT_FAIL;
        String operation_result = "监听失败";
       // userRedisCache.setAgentAudioState(seat, ESeatMediaState.WAITCON);

        // 处理监听
        ESeatMediaState audio = userRedisCache.getAgentAudioState(desc);
        if (null != audio && audio.value() > ESeatMediaState.WAITANS.value()) {
            String descChannelHost = userRedisCache.getAgentChannHost(desc);
            String descChannelUUID = userRedisCache.getAgentChannId(desc);
            if (eslSupport.uuid_exists(descChannelHost, descChannelUUID)) {
                String monitChannelName = userRedisCache.getAgentChannelName(seat);
                if (null != monitChannelName) {
//					String requesterName = userRedisCache.getRequesterName(desc);
//					String requesterUUID = userRedisCache.getRequesterChannelUUID(desc);
//					userRedisCache.setRequesterName(seat, requesterName);
                    userRedisCache.setUserChannId(seat, descChannelUUID);

                    seatChannelHost = descChannelHost;
                    seatChannelUUID = UUID.randomUUID().toString();
                    userRedisCache.setAgentChannHost(seat, seatChannelHost);
                    userRedisCache.setAgentChannId(seat, seatChannelUUID);
                    //huangqb add for 2019-4-24 for

                    String param = "{origination_uuid=" + seatChannelUUID + "}{origination_caller_id_number="+ sourceNumber +"}{"+ StaticValue.SeatName + "=" + seat
                            + "}{"+ StaticValue.RingType+"="+ EActionType.MONITOR.name()+"}" + monitChannelName + " m:^:answer^eavesdrop:" + descChannelUUID
                            + " inline";

                    eslSupport.originate(descChannelHost, param);
                    iresult = StaticValue.RESULT_SUCCESS;
                    operation_result = "监听成功";
                }
            }
        }
        //增加监听部分的log...
        AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
        agentStateLogModel.setEventName("CUSTOM");
        agentStateLogModel.setEventSubclass("AGENT::LISTEN");
        agentStateLogModel.setResult(iresult);
        agentStateLogModel.setEventContent(operation_result);
        agentStateLogModel.setAgnetId(seat);
        agentStateLogModel.setAgentSignId(UUID.fromString(seat_list.get(seat)).toString());
        agentStateLogModel.setTargetId(desc);
        agentStateLogModel.setTargetSignId(UUID.fromString(seat_list.get(desc)).toString());
        agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
        agentStateLogModel.setOperTime(System.currentTimeMillis());
        agentStateLogModel.setTargetChatId(userRedisCache.getSessionId(seat));
        agentStateLogModel.setChatId(userRedisCache.getSourceChannId(seat));
        rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
    }

    @Override
    public void whisper(String seat,String sessionId) {
        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);
        EActionType first = userRedisCache.getRingType(seat);
        String seatChannelHost = userRedisCache.getAgentChannHost(seat);
        String seatChannelUUID = userRedisCache.getAgentChannId(seat);

        EventParam eventParam=createTeleEventParam(seat);
        if (!EActionType.MONITOR.equals(first)) {
            eventParam.setEventDesc("当前没有处于监听状态");
            client.sendEvent(StaticValue.onWhisperFail, eventParam);
            return;
        }

        if (!eslSupport.uuid_exists(seatChannelHost, seatChannelUUID)) {
            eventParam.setEventDesc("当前没有通话");
            client.sendEvent(StaticValue.onWhisperFail, eventParam);
            return;
        }
        eslSupport.uuid_recv_dtmf(seatChannelHost, seatChannelUUID, "2");

        String targetName = userRedisCache.getTargetName(seat);
        eventParam.setTargetId(targetName);
        client.sendEvent(StaticValue.onWhisperSuccess, eventParam);

    }

    @Override
    public void intercept(String seat,String target ,String sessionId) {
        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);
        EActionType first = userRedisCache.getRingType(seat);
        String seatChannelHost = userRedisCache.getAgentChannHost(seat);
        String seatChannelUUID = userRedisCache.getAgentChannId(seat);

        LOG.debug("\n\n =============huangqb add for intercept===================" +
                "\n seat:" + seat +
                "\n target:" + target +
                "\n sessionId:" + sessionId + "\n\n");

        EventParam eventParam=createTeleEventParam(seat);
        if (!EActionType.MONITOR.equals(first)) {
            eventParam.setEventDesc("当前没有处于监听状态");
            client.sendEvent(StaticValue.onInterceptFail, eventParam);
            return;
        }

        if (!eslSupport.uuid_exists(seatChannelHost, seatChannelUUID)) {
            eventParam.setEventDesc("当前没有通话");
            client.sendEvent(StaticValue.onInterceptFail, eventParam);
            return;
        }
        userRedisCache.setRingType(seat,EActionType.INTERCEPT);//改为拦截
        String otherSeatName = userRedisCache.getTargetName(seat);
        String otherHost = userRedisCache.getAgentChannHost(otherSeatName);
        String otherUUID = userRedisCache.getAgentChannId(otherSeatName);
        String user_uuid = eslSupport.uuid_getvar(otherHost, otherUUID, "bridge_uuid");
        userRedisCache.setUserChannId(otherSeatName,userRedisCache.getUserChannId(seat)); //huangqb add for 2019-5-31
        //重新设置ai_target_agent 通道变量，在放音时能够正确播放当前服务的坐席名称...
        eslSupport.uuid_setvar(otherHost,user_uuid,"ai_target_agent",seat);
        eslSupport.uuid_bridge(otherHost, user_uuid, seatChannelUUID);
        eslSupport.uuid_recv_dtmf(seatChannelHost, seatChannelUUID, "1");

        String targetName = userRedisCache.getTargetName(seat);
        eventParam.setTargetId(targetName);
        client.sendEvent(StaticValue.onInterceptSuccess, eventParam);

    }

    @Async
    @Override
    public void insert(String seat,String sessionId,String targetId,String originalUserId) {
        // TODO 向监听发送DTFM
        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);
        EActionType first = userRedisCache.getRingType(seat);
        String seatChannelHost = userRedisCache.getAgentChannHost(seat);
        String seatChannelUUID = userRedisCache.getAgentChannId(seat);

        EventParam eventParam=createTeleEventParam(seat);

        if (!EActionType.MONITOR.equals(first)) {
            eventParam.setEventDesc("当前没有处于监听状态");
            client.sendEvent(StaticValue.onInsertFail, eventParam);
            return;
        }

        if (!eslSupport.uuid_exists(seatChannelHost, seatChannelUUID)) {
            eventParam.setEventDesc("当前没有通话");
            client.sendEvent(StaticValue.onInsertFail,eventParam );
            return;
        }
        eslSupport.uuid_recv_dtmf(seatChannelHost, seatChannelUUID, "3");
        String targetName = userRedisCache.getTargetName(seat);

        eventParam.setTargetId(targetName);
        client.sendEvent(StaticValue.onInsertSuccess,eventParam);

    }

    /**
     * 强制签出操作
     *
     * @param targetAgent
     */
    @Async
    @Override
    public void forceSignOut(String targetAgent, String currentAgent) {
        UUID uuid = UUID.fromString(seat_list.get(currentAgent));
        SocketIOClient client = server.getClient(uuid);

        UUID userUUID = userRedisCache.getAgentUUID(targetAgent);
        EventParam eventParam=new EventParam(currentAgent, EAccessType.ACCESS_TYPE_AGENT);
        eventParam.setTargetId(targetAgent);
        // 判断是否签入
        if (null == userUUID) {
            eventParam.setEventDesc("强制签出失败，座席已签出");
            client.sendEvent(StaticValue.onForceSignOutFail,eventParam );

        } else {

//			if (seat_list.containsKey(targetAgent)) {
//				SocketIOClient targetClient = server.getClient(userUUID);
//				eventParam.setEventDesc("强制签出失败，座席已签出");
//				targetClient.sendEvent(StaticValue.onLogoutSuccess, targetAgent);
//				seat_list.remove(targetAgent);
//				targetClient.disconnect();
//			} else {
            for (String host : eslSupport.getInstanceList()) {
                // 发送ESL事件到目标座席所在的CC
//					EventCommandObject forceRequest = new EventCommandObject();
//					forceRequest.setHost(host);
//					forceRequest.setEventName("CUSTOM");
//					forceRequest.setEventSubClass("cc::qc_action");
//					forceRequest.putHeader("target_uuid", userUUID.toString());
//					forceRequest.putHeader("target_agent", targetAgent);
//					forceRequest.putHeader("source_name", currentAgent);
//					forceRequest.putHeader("response_type", "forceSignOut");
                String core_uuid = userRedisCache.getAgentServiceUUID(targetAgent);
//					eslSupport.sendEventCommand(forceRequest);
//					rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(forceRequest));
                EventForCC obj = new EventForCC();
                obj.setEventName("CUSTOM");
                obj.setEventSubClass("cc::qc_action");
                obj.setHost(host);
                obj.setFreeSWITCH_Hostname(host);
                obj.setSource_name(currentAgent);
                obj.setTarget_uuid(userUUID.toString());
                obj.setTarget_agent(targetAgent);
                obj.setResponse_type("forceSignOut");
                rabbitmqSender.sendDataToCC(host,"cc::qc_action","",JSONObject.toJSONString(obj));
                break;
            }
//			}
            //日志输出
            AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
            agentStateLogModel.setEventName("CUSTOM");
            agentStateLogModel.setEventSubclass("AGENT::FORCESIGNOUT");
            agentStateLogModel.setResult(StaticValue.RESULT_SUCCESS);
            agentStateLogModel.setEventContent("强制签出成功");
            agentStateLogModel.setAgnetId(currentAgent);
            agentStateLogModel.setAgentSignId(uuid.toString());
            agentStateLogModel.setTargetId(targetAgent);
            agentStateLogModel.setTargetSignId(userUUID.toString());
            agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
            agentStateLogModel.setOperTime(System.currentTimeMillis());
//			rabbitmqSender.sendDataToQueue(agentStateLogModel);
            rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
            System.out.println("AGENT::FORCESIGNOUT=========++++++++++"+ JSONObject.toJSONString(agentStateLogModel));
            eventParam.setEventDesc("强制签出成功");
            client.sendEvent(StaticValue.onForceSignOutSuccess, eventParam);
        }

    }

    @Override
    public void signOut(String targetAgent, String currentAgent) {

    }

    /**
     * 强制示闲操作
     *
     * @param targetAgent
     */
    @Override
    public void forceSayFree(String targetAgent, String currentAgent) {
        UUID uuid = UUID.fromString(seat_list.get(currentAgent));
        SocketIOClient client = server.getClient(uuid);
        EventParam eventParam=new EventParam(currentAgent, EAccessType.ACCESS_TYPE_AGENT);
        eventParam.setTargetId(targetAgent);
        String userUUID = userRedisCache.getAgentUUID(targetAgent).toString();
        for (String host : eslSupport.getInstanceList()) {

            // 判断目标座席是否签入
            if (null == userUUID || "".equals(userUUID)) {
                eventParam.setEventDesc("强制示闲失败，座席已签出");
                client.sendEvent(StaticValue.onForceSayFreeFail, eventParam);

            } else {
                // 发送ESL事件到目标座席所在的CC
//				EventCommandObject forceRequest = new EventCommandObject();
//				forceRequest.setHost(host);
//				forceRequest.setEventName("CUSTOM");
//				forceRequest.setEventSubClass("cc::qc_action");
//				forceRequest.putHeader("target_agent", targetAgent);
//				forceRequest.putHeader("source_name", currentAgent);
//				forceRequest.putHeader("response_type", "forceSayFree");
//				String core_uuid = userRedisCache.getAgentServiceUUID(targetAgent);
//				eslSupport.sendEventCommand(forceRequest);
//				rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(forceRequest));
                EventForCC obj = new EventForCC();
                obj.setEventName("CUSTOM");
                obj.setEventSubClass("cc::qc_action");
                obj.setHost(host);
                obj.setFreeSWITCH_Hostname(host);
                obj.setSource_name(currentAgent);
                obj.setTarget_agent(targetAgent);
                obj.setResponse_type("forceSayFree");
                rabbitmqSender.sendDataToCC(host,"cc::qc_action","",JSONObject.toJSONString(obj));
                userRedisCache.setAgentState(targetAgent, ESeatState.free);
                eventParam.setEventDesc("强制示闲成功");
                client.sendEvent(StaticValue.onForceSayFreeSuccess, eventParam);
                //日志输出
                AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
                agentStateLogModel.setEventName("CUSTOM");
                agentStateLogModel.setEventSubclass("AGENT::FORCEFREE");
                agentStateLogModel.setResult(StaticValue.RESULT_SUCCESS);
                agentStateLogModel.setEventContent("强制示闲成功");
                agentStateLogModel.setAgnetId(currentAgent);
                agentStateLogModel.setAgentSignId(uuid.toString());
                agentStateLogModel.setTargetId(targetAgent);
                agentStateLogModel.setTargetSignId(userUUID);
                agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
                agentStateLogModel.setOperTime(System.currentTimeMillis());
//				rabbitmqSender.sendDataToQueue(agentStateLogModel);
                rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
                //System.out.println("AGENT::FORCEFREE=========++++++++++"+ JSONObject.toJSONString(agentStateLogModel));
            }
            break;
        }
    }

    /**
     * 强制示忙操作
     *
     * @param targetAgent
     */
    @Override
    public void forceSayBusy(String targetAgent, String currentAgent) {
        UUID uuid = UUID.fromString(seat_list.get(currentAgent));
        SocketIOClient client = server.getClient(uuid);

        String userUUID = userRedisCache.getAgentUUID(targetAgent).toString();
        EventParam eventParam=new EventParam(currentAgent, EAccessType.ACCESS_TYPE_AGENT);
        eventParam.setTargetId(targetAgent);

        for (String host : eslSupport.getInstanceList()) {
            // 判断目标座席是否签入
            if (null == userUUID || "".equals(userUUID)) {
                eventParam.setEventDesc("强制示忙失败，座席已签出");
                client.sendEvent(StaticValue.onForceSayBusyFail, eventParam);

            } else {
                // 发送ESL事件到目标座席所在的CC
//				EventCommandObject forceRequest = new EventCommandObject();
//				forceRequest.setHost(host);
//				forceRequest.setEventName("CUSTOM");
//				forceRequest.setEventSubClass("cc::qc_action");
//				forceRequest.putHeader("target_agent", targetAgent);
//				forceRequest.putHeader("source_name", currentAgent);
//				forceRequest.putHeader("response_type", "forceSayBusy");
//				eslSupport.sendEventCommand(forceRequest);
//				rabbitmqSender.sendDataToFS(JSONObject.toJSONString(forceRequest));
                EventForCC obj = new EventForCC();
                obj.setEventName("CUSTOM");
                obj.setEventSubClass("cc::qc_action");
                obj.setHost(host);
                obj.setFreeSWITCH_Hostname(host);
                obj.setSource_name(currentAgent);
                obj.setTarget_agent(targetAgent);
                obj.setResponse_type("forceSayBusy");
                rabbitmqSender.sendDataToCC(host,"cc::qc_action","",JSONObject.toJSONString(obj));
                userRedisCache.setAgentState(targetAgent, ESeatState.busy);
                eventParam.setEventDesc("强制示忙成功");
                client.sendEvent(StaticValue.onForceSayBusySuccess, eventParam);

                //日志输出
                AgentStateLogModel agentStateLogModel = new AgentStateLogModel();
                agentStateLogModel.setEventName("CUSTOM");
                agentStateLogModel.setEventSubclass("AGENT::FORCEBUSY");
                agentStateLogModel.setResult(StaticValue.RESULT_SUCCESS);
                agentStateLogModel.setEventContent("强制示忙成功");
                agentStateLogModel.setAgnetId(currentAgent);
                agentStateLogModel.setAgentSignId(uuid.toString());
                agentStateLogModel.setTargetId(targetAgent);
                agentStateLogModel.setTargetSignId(userUUID);
                agentStateLogModel.setOperType(StaticValue.OPERATOR_TYPE_SELF);
                agentStateLogModel.setOperTime(System.currentTimeMillis());
//				rabbitmqSender.sendDataToQueue(agentStateLogModel);
                rabbitmqSender.sendDataToLog(JSONObject.toJSONString(agentStateLogModel));
                System.out.println("AGENT::FORCEBUSY=========++++++++++"+ JSONObject.toJSONString(agentStateLogModel));

            }
            break;
        }
    }

    /**
     * 强制挂机操作
     *
     * @param targetAgent
     */
    @Override
    public void forceRelease(String targetAgent,String targetSessionId, String currentAgent) {
        UUID uuid = UUID.fromString(seat_list.get(currentAgent));
        SocketIOClient client = server.getClient(uuid);

        String userUUID = userRedisCache.getAgentUUID(targetAgent).toString();

        EventParam eventParam=new EventParam(currentAgent, EAccessType.ACCESS_TYPE_AGENT);
        eventParam.setTargetId(targetAgent);
        // 判断目标座席是否签入
        if (null == userUUID || "".equals(userUUID)) {
            eventParam.setEventDesc("强制挂机失败，座席已签出");
            client.sendEvent(StaticValue.onForceReleaseFail, eventParam);
        } else {
            String userChannleUUID = userRedisCache.getAgentChannId(targetAgent);
            String userChannleHost = userRedisCache.getAgentChannHost(targetAgent);
            if (eslSupport.uuid_exists(userChannleHost, userChannleUUID)){
                eslSupport.uuid_kill(userChannleHost, userChannleUUID);
                eventParam.setEventDesc("强制挂机成功");
                client.sendEvent(StaticValue.onForceReleaseSuccess,eventParam);
            }else{
                eventParam.setEventDesc("强制挂机失败，坐席不在通话中");
                client.sendEvent(StaticValue.onForceReleaseFail, eventParam);
            }

//			for (String host : eslSupport.getInstanceList()) {
//				// 发送ESL事件到目标座席所在的CC
//				EventCommandObject forceRequest = new EventCommandObject();
//				forceRequest.setHost(host);
//				forceRequest.setEventName("CUSTOM");
//				forceRequest.setEventSubClass("cc::qc_action");
//				forceRequest.putHeader("target_agent", targetAgent);
//				forceRequest.putHeader("source_name", currentAgent);
//				forceRequest.putHeader("response_type", "forceRelease");
//				eslSupport.sendEventCommand(forceRequest);
//				break;
//			}

        }
    }

    @Async
    @Override
    public void hold(String currentAgent,String sessionId, boolean hold) {
        UUID uuid = UUID.fromString(seat_list.get(currentAgent));
        SocketIOClient client = server.getClient(uuid);
        String seatChannelHost = userRedisCache.getAgentChannHost(currentAgent);
        String seatChannelUUID = userRedisCache.getAgentChannId(currentAgent);

        EventParam eventParam=new EventParam(currentAgent, EAccessType.ACCESS_TYPE_AGENT);


        if (!eslSupport.uuid_exists(seatChannelHost, seatChannelUUID)) {
            eventParam.setEventDesc("服务对象不存在");
            client.sendEvent(StaticValue.onHoldFail, eventParam);
            return;
        }
        eventParam.setValue(hold?"1":"0");
        eslSupport.uuid_hold(seatChannelHost, seatChannelUUID, hold);
        client.sendEvent(StaticValue.onHoldSuccess, eventParam);

    }

    public boolean doCustomHelpReq(SocketIOClient client, Map<String, String> eventHeaders) {
        if (!eventHeaders.containsKey("target_agent"))
            return false;
        String hostName = eventHeaders.get("FreeSWITCH-Hostname");
        String target_agent = eventHeaders.get("target_agent");
        String source_name = eventHeaders.get("source_name");
        String source_uuid = eventHeaders.get("source_uuid");
        String action = eventHeaders.get("action");
        String session_id = eventHeaders.get("session_id");
        String core_uuid = eventHeaders.get("Core-UUID");


        PhoneCustomInfo info=new PhoneCustomInfo(target_agent, source_uuid, session_id);
        info.setRingType(EActionType.HELP_SEAT);
        info.setAgentRequestType(EActionType.NONE);
        info.setMediaState(ESeatMediaState.RING);
        info.setUserAccount(source_name);
        info.setHostName(hostName);



        EActionType localRingType = userRedisCache.getRingType(target_agent);
        String localSessionId=userRedisCache.getSessionId(target_agent);

        EventParam eventParam=new EventParam(target_agent, EAccessType.ACCESS_TYPE_TELE);
        eventParam.setRingType(EActionType.HELP_SEAT);
        eventParam.setUserAccount(source_name);
        eventParam.setUserName(source_name);
        eventParam.setSessionId(session_id);

        LOG.debug(String.format("doCustomTransReq action:%s,session_id:%s,target_agent:%s,source_name:%s",
                action,session_id,target_agent,source_name));
        LOG.debug(String.format("doCustomTransReq localRingType:%s,localSessionId:%s",localRingType,localSessionId));


        if ("cancel".equalsIgnoreCase(action) && EActionType.HELP_SEAT.equals(localRingType)) {
            if (session_id.equals(localSessionId)) {
                setSeatToIdle(target_agent);
            }
        } else if (EActionType.NONE.equals(localRingType)) {

            userRedisCache.setSessionId(target_agent, session_id);
            userRedisCache.setUserName(target_agent, source_name);

            userRedisCache.setSourceHost(target_agent, hostName);
            userRedisCache.setAgentServiceUUID(target_agent, core_uuid);
            userRedisCache.setSourceChannId(target_agent, source_uuid);
            userRedisCache.setSourceName(target_agent, source_name);




            userRedisCache.setRingType(target_agent, EActionType.HELP_SEAT);
            userRedisCache.setAgentRequestType(target_agent, EActionType.NONE);
            userRedisCache.setAgentAudioState(target_agent, ESeatMediaState.RING);
            client.sendEvent(StaticValue.onRequestSeat, eventParam);
            task.offer(eventHeaders, otherRequestTimeOut, TimeUnit.SECONDS, this);
            userRedisCache.setPhoneRequest(target_agent, session_id, info);
            return true;
        } else {
            // 如果当前坐席状态为忙 就直接回复BUSY 进行拒绝服务
//			EventCommandObject obj = new EventCommandObject();
//			obj.setHost(hostName);
//			obj.setEventName("CUSTOM");
//			obj.setEventSubClass("cc::help_resp");
//			obj.putHeader("response_type", "busy");
//			obj.putHeader("target_agent", source_name);
//			obj.putHeader("source_name", target_agent);
//			obj.putHeader("session_id", session_id);
            //eslSupport.sendEventCommand(obj);
//			rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
            EventForCC obj = new EventForCC();
            obj.setEventName("CUSTOM");
            obj.setEventSubClass("cc::help_resp");
            obj.setHost(hostName);
            obj.setFreeSWITCH_Hostname(hostName);
            obj.setSource_name(source_name);
            obj.setSession_id(session_id);
            obj.setTarget_agent(target_agent);
            obj.setResponse_type("busy");
            rabbitmqSender.sendDataToCC(hostName,"cc::help_resp","",JSONObject.toJSONString(obj));
        }
        return false;
    }

    public void doCustomHelpResp(SocketIOClient client, Map<String, String> event) {

        String target_agent = event.get("target_agent");
        String source_name = event.get("source_name");
        String response_type = event.get("response_type");
        String session_id = event.get("session_id");

        EActionType second = userRedisCache.getAgentRequestType(target_agent);
        String name = userRedisCache.getTargetName(target_agent);
        if ((source_name.equals(name))) {
            if (EActionType.HELP_SEAT.equals(second)) {
                if ("busy".equals(response_type)) {
                    userRedisCache.setAgentRequestType(target_agent, EActionType.NONE);
                    userRedisCache.setTargetChannId(target_agent, null);
                    userRedisCache.setTargetHost(target_agent, null);
                    userRedisCache.setTargetName(target_agent, null);
                    EventParam eventParam=new EventParam(target_agent, EAccessType.ACCESS_TYPE_TELE);
                    eventParam.setRingType(EActionType.HELP_SEAT);
                    eventParam.setUserAccount(source_name);
                    eventParam.setUserName(source_name);
                    client.sendEvent(StaticValue.onHelpToSeatFail, eventParam);
                } else if ("ok".equals(response_type)) {
                    String seatB_UUID = event.get("seatB_UUID");
                    String seat = event.get("target_agent");
                    userRedisCache.setTargetChannId(seat, seatB_UUID);

                    EventParam eventParam=new EventParam(target_agent, EAccessType.ACCESS_TYPE_TELE);
                    eventParam.setRingType(EActionType.HELP_SEAT);
                    eventParam.setUserAccount(source_name);
                    eventParam.setUserName(source_name);
                    client.sendEvent(StaticValue.onHelpToSeatSuccess, eventParam);
                }
            }

        }
    }

    public boolean doCustomServiceReq(SocketIOClient client, Map<String, String> eventHeaders) {

        String hostName = eventHeaders.get("FreeSWITCH-Hostname");
        String agentId = eventHeaders.get("target_agent");
        String unique_id = eventHeaders.get("unique_id");
        String session_id = eventHeaders.get("session_id");
        String action = eventHeaders.get("action");
        String core_uuid = eventHeaders.get("Core-UUID");
        String userAccount = eslSupport.uuid_getvar(hostName, unique_id, "ani");

        EventParam eventParam=new EventParam(agentId, EAccessType.ACCESS_TYPE_TELE);
        eventParam.setRingType(EActionType.SERVICE);
        eventParam.setSessionId(session_id);
        eventParam.setUserAccount(userAccount);
        eventParam.setUserName(userAccount);

        PhoneCustomInfo old =userRedisCache.getPhoneRequest(agentId, session_id);
        PhoneCustomInfo info=new PhoneCustomInfo(agentId, unique_id, session_id);
        info.setRingType(EActionType.SERVICE);
        info.setAgentRequestType(EActionType.NONE);
        info.setMediaState(ESeatMediaState.RING);
        info.setUserAccount(userAccount);
        info.setHostName(hostName);
        ESeatMediaState phoneState = userRedisCache.getAgentAudioState(agentId);
        EActionType ringType = userRedisCache.getRingType(agentId);
        ESeatState state = userRedisCache.getAgentState(agentId);
        if ("cancel".equalsIgnoreCase(action)) {
            String old_session_id = userRedisCache.getSessionId(agentId);
            if (session_id.equals(old_session_id)) {
                setSeatToIdle(agentId);
            }
        } else if (action == null && ESeatMediaState.IDLE.equals(phoneState) && EActionType.NONE.equals(ringType)
                && ESeatState.free.equals(state)) {

            //增加判断软终端是否在线2019-5-13
            //LOG.debug("--------->  reghost:" + hostName + "<------------->agentId: "+ agentId + "\n\n");
            if (!eslSupport.sofia_get_user_register_status(hostName,agentId))
            {
                EventParam eventParamTmp=createTeleEventParam(agentId);
                eventParamTmp.setEventDesc("坐席软终端不在线");
                client.sendEvent(StaticValue.onSoftPhoneUNRegSuccessful, eventParamTmp);
            }
            //end of 2019-5-13

            userRedisCache.setSessionId(agentId, session_id);

            userRedisCache.setUserChannId(agentId, unique_id);
            userRedisCache.setUserChannHost(agentId, hostName);
            userRedisCache.setUserName(agentId, userAccount);

            userRedisCache.setSourceHost(agentId, hostName);
            userRedisCache.setAgentServiceUUID(agentId, core_uuid);
            userRedisCache.setSourceChannId(agentId, unique_id);
            userRedisCache.setSourceName(agentId, userAccount);

            userRedisCache.setRingType(agentId, EActionType.SERVICE);
            userRedisCache.setAgentRequestType(agentId, EActionType.NONE);
            userRedisCache.setAgentAudioState(agentId, ESeatMediaState.RING);

            client.sendEvent(StaticValue.onRequestSeat, eventParam);
            task.offer(eventHeaders, seatRequestTimeOut, TimeUnit.SECONDS, this);
            userRedisCache.setPhoneRequest(agentId, session_id, info);
            return true;
        } else {
            // 如果当前坐席状态为忙 就直接回复BUSY 进行拒绝服务
//			EventCommandObject obj = new EventCommandObject();
//			obj.setHost(hostName);
//			obj.setEventName("CUSTOM");
//			obj.setEventSubClass("cc::resp");
//			obj.setUniqueId(unique_id);
//			obj.putHeader("response_type", "busy");
//			obj.putHeader("session_id", session_id);
            //eslSupport.sendEventCommand(obj);
//			rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
//            EventForCC obj = new EventForCC();
//            obj.setEventName("CUSTOM");
//            obj.setEventSubClass("cc::resp");
//            obj.setHost(hostName);
//            obj.setFreeSWITCH_Hostname(hostName);
//            obj.setUnique_id(unique_id);
//            obj.setSession_id(session_id);
//            obj.setResponse_type("busy");
//            rabbitmqSender.sendDataToCC(hostName,"cc::resp",unique_id,JSONObject.toJSONString(obj));
            EventForCC obj = new EventForCC();
            obj.setEventName("CUSTOM");
            obj.setEventSubClass("cc::resp");
            obj.setHost(hostName);
            obj.setFreeSWITCH_Hostname(hostName);
            obj.setResponse_type("busy");
            obj.setUnique_id(unique_id);
            obj.setSource_uuid(session_id);
            obj.setSession_id(session_id);
            obj.setLocal_unique_id(unique_id);
            rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));

        }
        return false;
    }

    public void doCustomTransReq(SocketIOClient client, Map<String, String> event) {

        String hostName = event.get("FreeSWITCH-Hostname");
        String session_id = event.get("session_id");
        String target_agent = event.get("target_agent");
        String unique_id = event.get("unique_id");
        String source_name = event.get("source_name");
        String source_uuid = event.get("source_uuid");
        String action = event.get("action");
        String core_uuid = event.get("Core-UUID");





       LOG.debug("------------>进入doCustomTransReq 函数处理<----------------------\n\n");

        EActionType ringType = userRedisCache.getRingType(target_agent);
        ESeatMediaState phoneState = userRedisCache.getAgentAudioState(target_agent);
        String localSessionId=userRedisCache.getSessionId(target_agent);
        LOG.debug(String.format("doCustomTransReq action:%s,session_id:%s,target_agent:%s,source_name:%s",
                action,session_id,target_agent,source_name));
        LOG.debug(String.format("doCustomTransReq localRingType:%s,localSessionId:%s",ringType,localSessionId));



        if ("cancel".equalsIgnoreCase(action)
                && EActionType.TRUNS_SEAT.equals(ringType)
        ) {

            if (session_id.equals(localSessionId )) {
                setSeatToIdle(target_agent);
            }
            return;
        }
        ESeatState state = userRedisCache.getAgentState(target_agent);
        if (action == null && ESeatMediaState.IDLE.equals(phoneState) && EActionType.NONE.equals(ringType)
                && ESeatState.free.equals(state)) {
            String userName = eslSupport.uuid_getvar(hostName, unique_id, "ani");
            userRedisCache.setSessionId(target_agent, session_id);

            // 记录用户的信道信息
            userRedisCache.setUserChannId(target_agent, unique_id);
            userRedisCache.setUserChannHost(target_agent, hostName);
            userRedisCache.setUserName(target_agent, userName);



            // 记录源坐席的信道信息
            userRedisCache.setSourceHost(target_agent, hostName);
            userRedisCache.setAgentServiceUUID(target_agent, core_uuid);
            userRedisCache.setSourceChannId(target_agent, source_uuid);
            userRedisCache.setSourceName(target_agent, source_name);

            userRedisCache.setRingType(target_agent, EActionType.TRUNS_SEAT);
            userRedisCache.setAgentRequestType(target_agent, EActionType.NONE);
            userRedisCache.setAgentAudioState(target_agent, ESeatMediaState.RING);





            EventParam eventParam=new EventParam(target_agent, EAccessType.ACCESS_TYPE_TELE);
            eventParam.setRingType(EActionType.TRUNS_SEAT);
            eventParam.setUserAccount(source_name);
            eventParam.setUserName(source_name);
            eventParam.setSessionId(session_id);


            PhoneCustomInfo info=new PhoneCustomInfo(target_agent, unique_id, session_id);
            info.setRingType(EActionType.TRUNS_SEAT);
            info.setAgentRequestType(EActionType.NONE);
            info.setMediaState(ESeatMediaState.RING);
            info.setUserAccount(userName);
            info.setHostName(hostName);
            userRedisCache.setPhoneRequest(target_agent, session_id, info);
            client.sendEvent(StaticValue.onRequestSeat, eventParam);
            task.offer(event, seatRequestTimeOut, TimeUnit.SECONDS, this);
        } else {
            // 如果当前坐席状态为忙 就直接回复BUSY 进行拒绝服务
//			EventCommandObject obj = new EventCommandObject();
//			obj.setHost(hostName);
//			obj.setEventName("CUSTOM");
//			obj.setEventSubClass("cc::trans_resp");
//			obj.putHeader("response_type", "busy");
//			obj.putHeader("unique_id", unique_id);
//			obj.putHeader("target_agent", source_name);
//			obj.putHeader("source_name", target_agent);
//			obj.putHeader("session_id", session_id);
            //eslSupport.sendEventCommand(obj);
//			rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
            EventForCC obj = new EventForCC();
            obj.setEventName("CUSTOM");
            obj.setEventSubClass("cc::trans_resp");
            obj.setHost(hostName);
            obj.setFreeSWITCH_Hostname(hostName);
            obj.setSource_name(source_name);
            obj.setUnique_id(unique_id);
            obj.setSession_id(session_id);
            obj.setTarget_agent(target_agent);
            obj.setResponse_type("busy");
            rabbitmqSender.sendDataToCC(hostName,"cc::trans_resp",unique_id,JSONObject.toJSONString(obj));
        }
    }

    public void doCustomTransResp(SocketIOClient client, Map<String, String> event) {
        String target_agent = event.get("target_agent");
        String source_name = event.get("source_name");
        String response_type = event.get("response_type");
        String session_id = event.get("session_id");

        LOG.debug("------------>doCustomTransResp 函数处理<----------------------\n\n");


        EActionType second = userRedisCache.getAgentRequestType(target_agent);
        String name = userRedisCache.getTargetName(target_agent);
        if ((source_name.equals(name))) {
            if (EActionType.TRUNS_SEAT.equals(second)) {
                if ("busy".equals(response_type)) {
                    LOG.debug("------------>busy走这里<-----------------------\n\n");

                    userRedisCache.setAgentRequestType(target_agent, EActionType.NONE);
                    userRedisCache.setTargetChannId(target_agent, null);
                    userRedisCache.setTargetHost(target_agent, null);
                    userRedisCache.setTargetName(target_agent, null);



                    EventParam eventParam=new EventParam(target_agent, EAccessType.ACCESS_TYPE_TELE);
                    eventParam.setRingType(EActionType.TRUNS_SEAT);
                    eventParam.setUserAccount(source_name);
                    eventParam.setUserName(source_name);
                    eventParam.setSessionId(userRedisCache.getSessionId(target_agent));
                    client.sendEvent(StaticValue.onTransToSeatFail, eventParam);

                } else if ("ok".equals(response_type)) {
                    LOG.debug("------------>ok走这里<-----------------------\n\n");
                    EventParam eventParam=new EventParam(target_agent, EAccessType.ACCESS_TYPE_TELE);
                    eventParam.setRingType(EActionType.TRUNS_SEAT);
                    eventParam.setUserAccount(source_name);
                    eventParam.setUserName(source_name);
                    eventParam.setSessionId(userRedisCache.getSessionId(target_agent));
                    client.sendEvent(StaticValue.onTransToSeatSuccess,eventParam);
                    setSeatToIdle(target_agent);
                }
            }

        }

    }

    public void doCustomCallReq(SocketIOClient client, Map<String, String> event) {
        String hostName = event.get("FreeSWITCH-Hostname");
        String target_agent = event.get("target_agent");
        String source_name = event.get("source_name");
        String action = event.get("action");
        String source_uuid = event.get("source_uuid");
        String session_id = event.get("session_id");
        String core_uuid = event.get("Core-UUID");



        PhoneCustomInfo old =userRedisCache.getPhoneRequest(target_agent, session_id);
        PhoneCustomInfo info=new PhoneCustomInfo(target_agent, source_uuid, session_id);
        info.setRingType(EActionType.INNER_CALL);
        info.setAgentRequestType(EActionType.NONE);
        info.setMediaState(ESeatMediaState.RING);
        info.setUserAccount(source_name);
        info.setHostName(hostName);

        ESeatMediaState phoneState = userRedisCache.getAgentAudioState(target_agent);
        EActionType first = userRedisCache.getRingType(target_agent);
        String uuid = userRedisCache.getUserChannId(target_agent);
        ESeatState state = userRedisCache.getAgentState(target_agent);

        if ("cancel".equalsIgnoreCase(action)) {
            if (source_uuid.equals(uuid)) {
                // 收到发起方的取消事件
                setSeatToIdle(target_agent);
            }
        } else if ((action == null) && ESeatMediaState.IDLE.equals(phoneState) && ESeatState.free.equals(state)
                && EActionType.NONE.equals(first)) {
            userRedisCache.setSessionId(target_agent, session_id);
            userRedisCache.setUserChannHost(target_agent, hostName);
            userRedisCache.setUserChannId(target_agent, source_uuid);
            userRedisCache.setUserName(target_agent, source_name);


            // 记录源坐席的信道信息
            userRedisCache.setSourceHost(target_agent, hostName);
            userRedisCache.setAgentServiceUUID(target_agent, core_uuid);
            userRedisCache.setSourceChannId(target_agent, source_uuid);
            userRedisCache.setSourceName(target_agent, source_name);

            userRedisCache.setRingType(target_agent, EActionType.INNER_CALL);
            userRedisCache.setAgentRequestType(target_agent, EActionType.NONE);
            userRedisCache.setAgentAudioState(target_agent, ESeatMediaState.RING);

            EventParam eventParam=new EventParam(target_agent, EAccessType.ACCESS_TYPE_TELE);
            eventParam.setRingType(EActionType.INNER_CALL);
            eventParam.setUserAccount(source_name);
            eventParam.setUserName(source_name);
            eventParam.setSessionId(session_id);


            client.sendEvent(StaticValue.onRequestSeat, eventParam);
            task.offer(event, otherRequestTimeOut, TimeUnit.SECONDS, this);
            userRedisCache.setPhoneRequest(target_agent, session_id, info);
        } else {
            // 如果当前坐席状态为忙 就直接回复BUSY 进行拒绝服务
//			EventCommandObject obj = new EventCommandObject();
//			obj.setHost(hostName);
//			obj.setEventName("CUSTOM");
//			obj.setEventSubClass("cc::call_resp");
//			obj.putHeader("response_type", "busy");
//			obj.putHeader("target_agent", source_name);
//			obj.putHeader("session_id", source_name);
            //eslSupport.sendEventCommand(obj);
//			rabbitmqSender.sendDataToFS(core_uuid,JSONObject.toJSONString(obj));
            EventForCC obj = new EventForCC();
            obj.setEventName("CUSTOM");
            obj.setEventSubClass("cc::call_resp");
            obj.setHost(hostName);
            obj.setFreeSWITCH_Hostname(hostName);
            obj.setSource_name(source_name);
            obj.setSession_id(session_id);
            obj.setTarget_agent(target_agent);
            obj.setResponse_type("busy");
            rabbitmqSender.sendDataToCC(hostName,"cc::call_resp","",JSONObject.toJSONString(obj));
        }
    }

    public void doCustomCallResp(SocketIOClient client, Map<String, String> event) {
        String target_agent = event.get("target_agent");
        String hostName = event.get("FreeSWITCH-Hostname");
        String response_type = event.get("response_type");
        String source_name = event.get("source_name");
        String source_uuid = event.get("source_uuid");
        String session_id = event.get("session_id");

        EActionType first = userRedisCache.getRingType(target_agent);

        if (EActionType.INNER_CALL.equals(first)) {
            if ("ok".equals(response_type)) {
                userRedisCache.setUserChannHost(target_agent, hostName);
                userRedisCache.setUserChannId(target_agent, source_uuid);
                userRedisCache.setUserName(target_agent, source_name);

                EventParam eventParam=new EventParam(target_agent, EAccessType.ACCESS_TYPE_TELE);
                eventParam.setRingType(first);
                eventParam.setUserAccount(source_name);
                eventParam.setUserName(source_name);
                eventParam.setSessionId(userRedisCache.getSessionId(target_agent));
                client.sendEvent(StaticValue.onCallSeatSuccess, eventParam);
            } else {
                EventParam eventParam=new EventParam(target_agent, EAccessType.ACCESS_TYPE_TELE);
                eventParam.setRingType(first);
                eventParam.setUserAccount(source_name);
                eventParam.setUserName(source_name);
                eventParam.setSessionId(userRedisCache.getSessionId(target_agent));
                client.sendEvent(StaticValue.onCallSeatFail, eventParam);
                releaseINNER(target_agent,session_id);
            }
        }
    }

    /**
     * 质检操作
     *
     * @param client
     * @param event
     */
    public void doCustomQcAction(SocketIOClient client, Map<String, String> event) {
        String target_agent = event.get("target_agent");
        String target_session = event.get("target_session");
        String response_type = event.get("response_type");
        if ("forceSignOut".equals(response_type)) {

            EventParam eventParam=new EventParam(target_agent, EAccessType.ACCESS_TYPE_AGENT);
            client.sendEvent(StaticValue.onLogoutSuccess, eventParam);
            client.disconnect();
        } else if ("forceSayFree".equals(response_type)) {
            userRedisCache.setAgentState(target_agent, ESeatState.free);

        } else if ("forceSayBusy".equals(response_type)) {
            userRedisCache.setAgentState(target_agent, ESeatState.busy);

        } else if ("forceRelease".equals(response_type)) {
            release(target_agent,target_session,0);

        }
    }

    // @Async
    @Override
    public boolean doCustomRequestEvent(Map<String, String> eventHeader) {

        String target_agent = eventHeader.get("target_agent");
        if (!eventHeader.containsKey("target_agent") || !seat_list.containsKey(target_agent)) {
            LOG.debug("\n\nhuangqb add for test in doCustomRequestEvent ,target_agent: " + target_agent + "\n\n");
            return false;
        }

        UUID uuid = userRedisCache.getAgentUUID(target_agent);
        if (uuid == null) {
            LOG.debug("\n\n huangqb add for test in doCustomRequestEvent,uuid is null\n\n");
            return false;
        }
        SocketIOClient client = server.getClient(uuid);
        if (client == null) {
            LOG.debug("\n\nhuangqb add for test in doCustomRequestEvent,client is null\n\n");
            return false;
        }
        //LOG.debug("\n\nhuangqb add for test in doCustomRequestEvent: eventheader context:" + eventHeader + "\n\n");

        if (eventHeader.get("Event-Subclass").equals("cc::req")) {
            doCustomServiceReq(client, eventHeader);
        } else if (eventHeader.get("Event-Subclass").equals("cc::help_req")) {
            doCustomHelpReq(client, eventHeader);
        } else if (eventHeader.get("Event-Subclass").equals("cc::help_resp")) {
            doCustomHelpResp(client, eventHeader);
        } else if (eventHeader.get("Event-Subclass").equals("cc::trans_req")) {
            doCustomTransReq(client, eventHeader);
        } else if (eventHeader.get("Event-Subclass").equals("cc::trans_resp")) {
            doCustomTransResp(client, eventHeader);
        } else if (eventHeader.get("Event-Subclass").equals("cc::call_req")) {
            doCustomCallReq(client, eventHeader);
        } else if (eventHeader.get("Event-Subclass").equals("cc::call_resp")) {
            doCustomCallResp(client, eventHeader);
        } else if (eventHeader.get("Event-Subclass").equals("cc::qc_action")) {
            doCustomQcAction(client, eventHeader);
        }
        return true;
    }

    /*******************************************************************************
     huangqb modify 2019-4-11 for bug
     bug描述：
     当坐席的话机不在线时，用户电话转人工后仍可以收到振铃，
     坐席点击应答后提示坐席正忙，自动挂断，挂断后又是振铃，一直这样循环
     功能：
     获取坐席软终端的状态，实时更新状态，在呼叫坐席时，需要进一步判断软终端。
     ********************************************************************************/
    @Override
    public boolean doCustomRegisterUnRegisterEvent(Map<String,String> event)
    {
        String seat = "";
        if (event.get("Event-Subclass").equals("sofia::unregister")) {
            seat = event.get("username");
            //前端需要判断一下，当前上报的坐席是否签入，是否是自己的坐席。
            if ((userRedisCache.getOnlineAgent().contains(seat))&&(userRedisCache.getAgentUUID(seat)!=null))
            {
                UUID uuid = userRedisCache.getAgentUUID(seat);
                SocketIOClient client = server.getClient(uuid);
                EventParam eventParam=createTeleEventParam(seat);
                eventParam.setEventDesc("坐席软终端不在线");
                client.sendEvent(StaticValue.onSoftPhoneUNRegSuccessful, eventParam);
            }
        }
        return true;
    }

    @Override
    public void doCustomTimeOutEvent(Map<String, String> event) {

        String target_agent = event.get("target_agent");
        EActionType first = userRedisCache.getRingType(target_agent);
        ESeatMediaState state = userRedisCache.getAgentAudioState(target_agent);
        String uuid = userRedisCache.getUserChannId(target_agent);
        String hostName = event.get("FreeSWITCH-Hostname");
        if (!event.containsKey("target_agent") || !seat_list.containsKey(target_agent)) {
            return;
        }
        UUID clientuuid = userRedisCache.getAgentUUID(target_agent);
        if (clientuuid == null) {
            return;
        }
        SocketIOClient client = server.getClient(clientuuid);
        if (client == null)
            return;

        if (EActionType.SERVICE.equals(first) && event.get("Event-Subclass").equals("cc::req")) {
            String unique_id = event.get("unique_id");
            String session_id = event.get("session_id");
            String cur_session_id = userRedisCache.getSessionId(target_agent);
            if (null != session_id && session_id.equals(cur_session_id) && unique_id.equals(uuid)
                    && ESeatMediaState.RING.equals(state)) {
                LOG.debug("SERVICE Time out");
                // 收到内部的超时事件
//				String requesterName = eslSupport.uuid_getvar(hostName, unique_id, "ani");
//				EventParam eventParam=new EventParam(target_agent,EAccessType.ACCESS_TYPE_TELE);
//				eventParam.setRingType(ESeatActionType.SERVICE);
//				eventParam.setUserAccount(requesterName);	
//				eventParam.setSessionId(session_id);
//				eventParam.setEventDesc("请求超时");
//				client.sendEvent(StaticValue.onRequestCancel, eventParam);
                releaseSERVICE(target_agent,session_id);
                //增加重新排队功能...

            }
        } else if (EActionType.HELP_SEAT.equals(first) && event.get("Event-Subclass").equals("cc::help_req")) {
            LOG.debug("HELP_SEAT Time out");

            String source_uuid = event.get("source_uuid");
            String source_name = event.get("source_name");
            String unique_id = event.get("unique_id");
            String session_id = event.get("session_id");
            if (source_uuid.equals(userRedisCache.getSourceChannId(target_agent))
                    && ESeatMediaState.RING.equals(state)) {
//				EventParam eventParam=new EventParam(target_agent,EAccessType.ACCESS_TYPE_TELE);
//				eventParam.setRingType(ESeatActionType.HELP_SEAT);
//				eventParam.setUserAccount(source_name);	
//				eventParam.setSessionId(session_id);
//				eventParam.setEventDesc("请求超时");
//				client.sendEvent(StaticValue.onRequestCancel, eventParam);
                releaseHELP_SEAT(target_agent,session_id);
            }
        } else if (EActionType.TRUNS_SEAT.equals(first) && event.get("Event-Subclass").equals("cc::trans_req")) {
            LOG.debug("TRUNS_SEAT Time out");
            String unique_id = event.get("unique_id");
            String session_id = event.get("session_id");
//			String source_name = event.get("source_name");
//			String source_uuid = event.get("source_uuid");
            if (unique_id.equals(uuid) && ESeatMediaState.RING.equals(state)) {
//				String requesterName = eslSupport.uuid_getvar(hostName, unique_id, "ani");
//				EventParam eventParam=new EventParam(target_agent,EAccessType.ACCESS_TYPE_TELE);
//				eventParam.setRingType(ESeatActionType.TRUNS_SEAT);
//				eventParam.setUserAccount(requesterName);	
//				eventParam.setSessionId(session_id);
//				eventParam.setEventDesc("请求超时");
//				client.sendEvent(StaticValue.onRequestCancel, eventParam);
                LOG.debug(" ------------>if (unique_id.equals(uuid) && ESeatMediaState.RING.equals(state))<---------\n\n");
                releaseTRUNS_SEAT(target_agent,session_id);
            }

        } else if (EActionType.INNER_CALL.equals(first) && event.get("Event-Subclass").equals("cc::call_req")) {
            LOG.debug("INNER Time out");
            String source_name = event.get("source_name");
            String source_uuid = event.get("source_uuid");
            String session_id = event.get("session_id");
            if (source_uuid.equals(uuid) && ESeatMediaState.RING.equals(state)) {

//				EventParam eventParam=new EventParam(target_agent,EAccessType.ACCESS_TYPE_TELE);
//				eventParam.setRingType(ESeatActionType.INNER_CALL);
//				eventParam.setUserAccount(source_name);	
//				eventParam.setSessionId(session_id);
//				eventParam.setEventDesc("请求超时");
//				client.sendEvent(StaticValue.onRequestCancel, eventParam);
                releaseINNER(target_agent,session_id);
            }
        }
    }

    @Override
    public void doBridgeStateChange(Map<String,String> event) {
        String channelName = event.get("Channel-Name");
        String hostName = event.get("FreeSWITCH-Hostname");
        String channelUUID = event.get("Unique-ID");
        String seatName = event.get("variable_"+ StaticValue.SeatName);
        if (null != channelName) {
            LOG.debug(seatName + "====" + event.get("Event-Name") + "---" + channelUUID + "---- " + channelName + ":"
                    + hostName);

        }
        //LOG.debug("\n\n huangqb add for test...in doBridgeStateChange :event context:" + event + "\n\n");
        if (event.get("Event-Name").equals("CHANNEL_BRIDGE")) {
            String uuid_a = event.get("Bridge-A-Unique-ID");
            String uuid_b = event.get("Bridge-B-Unique-ID");
            String seat_no_a = eslSupport.uuid_getvar(hostName, uuid_a, ""+ StaticValue.SeatName+"");
            String seat_no_b = eslSupport.uuid_getvar(hostName, uuid_b, ""+ StaticValue.SeatName+"");

            if (seat_list.containsKey(seat_no_a)) {
                LOG.debug("A---" + event.get("Event-Name") + "|" + seat_no_a + "|" + seat_no_b + "|"
                        + event.get("Bridge-A-Unique-ID") + ":"
                        + event.get("Bridge-B-Unique-ID"));

                UUID uuid = UUID.fromString(seat_list.get(seat_no_a));
                SocketIOClient client = server.getClient(uuid);
                EActionType second = userRedisCache.getAgentRequestType(seat_no_a);
                if (EActionType.TRUNS_AUTO.equals(second)) {
                    userRedisCache.setAgentRequestType(seat_no_a, EActionType.NONE);
                    EventParam eventParam=new EventParam(seat_no_a, EAccessType.ACCESS_TYPE_TELE);
                    eventParam.setSessionId(userRedisCache.getSessionId(seat_no_a));


                    client.sendEvent(StaticValue.onTransToAutoReturn,eventParam);
                    String followData = eslSupport.uuid_getvar(hostName, uuid_b, "follow_data");
                    eventParam.setValue(followData);
                    client.sendEvent(StaticValue.onFollowData, eventParam);
                }

                String file_seat = recordPath + userRedisCache.getUserName(seat_no_a) + "_" + seat_no_a + "_"
                        + System.currentTimeMillis() + ".wav";
               // eslSupport.uuid_record(hostName, uuid_a, true, file_seat); //delete for record twice...
                userRedisCache.setAgentAudioState(seat_no_a, ESeatMediaState.TALK);

//				String file_user = sessionPath + uuid_a + ".wav";
//				eslSupport.uuid_setvar(hostName, uuid_b, "RECORD_READ_ONLY", "true");
//				eslSupport.uuid_record(hostName, uuid_b, true, file_user);
//				eslSupport.uuid_setvar(hostName, uuid_a, "AI_RECORD_SINGLE", uuid_a);
//				eslSupport.uuid_setvar(hostName, uuid_b, "RECORD_READ_ONLY", "");
            }
            if (seat_list.containsKey(seat_no_b)) {
                LOG.debug("B---" + event.get("Event-Name") + "|" + seat_no_a + "|" + seat_no_b + "|"
                        + event.get("Bridge-A-Unique-ID") + ":"
                        + event.get("Bridge-B-Unique-ID"));

                UUID uuid = UUID.fromString(seat_list.get(seat_no_b));
                SocketIOClient client = server.getClient(uuid);
                EActionType second = userRedisCache.getAgentRequestType(seat_no_b);
                if (EActionType.TRUNS_AUTO.equals(second)) {
                    userRedisCache.setAgentRequestType(seat_no_b, EActionType.NONE);

                    EventParam eventParam=new EventParam(seat_no_b, EAccessType.ACCESS_TYPE_TELE);
                    eventParam.setSessionId(userRedisCache.getSessionId(seat_no_b));

                    client.sendEvent(StaticValue.onTransToAutoReturn, eventParam);
                    String followData = eslSupport.uuid_getvar(hostName, uuid_a, "follow_data");
                    eventParam.setValue(followData);
                    client.sendEvent(StaticValue.onFollowData, eventParam);
                }

                String file_seat = recordPath + userRedisCache.getUserName(seat_no_b) + "_" + seat_no_b + "_"
                        + System.currentTimeMillis() + ".wav";
               // eslSupport.uuid_record(hostName, uuid_b, true, file_seat);
                userRedisCache.setAgentAudioState(seat_no_b, ESeatMediaState.TALK);
//				String file_user = sessionPath + uuid_b + ".wav";
//				eslSupport.uuid_setvar(hostName, uuid_a, "RECORD_READ_ONLY", "true");
//				eslSupport.uuid_record(hostName, uuid_a, true, file_user);
//				eslSupport.uuid_setvar(hostName, uuid_a, "AI_RECORD_SINGLE", uuid_b);
//				eslSupport.uuid_setvar(hostName, uuid_a, "RECORD_READ_ONLY", "");

            }
        } else if (event.get("Event-Name").equals("CHANNEL_UNBRIDGE")) {

            String uuid_a = event.get("Bridge-A-Unique-ID");
            String uuid_b = event.get("Bridge-B-Unique-ID");
            String seat_no_a = eslSupport.uuid_getvar(hostName, uuid_a, ""+ StaticValue.SeatName+"");
            String seat_no_b = eslSupport.uuid_getvar(hostName, uuid_b, ""+ StaticValue.SeatName+"");

            if (seat_list.containsKey(seat_no_a)) {
                LOG.debug("A---" + event.get("Event-Name") + "|" + seat_no_a + "|" + seat_no_b + "|"
                        + event.get("Bridge-A-Unique-ID") + ":"
                        + event.get("Bridge-B-Unique-ID"));

                userRedisCache.setAgentAudioState(seat_no_a, ESeatMediaState.WAITRET);
            }
            if (seat_list.containsKey(seat_no_b)) {
                LOG.debug("B---" + event.get("Event-Name") + "|" + seat_no_a + "|" + seat_no_b + "|"
                        + event.get("Bridge-A-Unique-ID") + ":"
                        + event.get("Bridge-B-Unique-ID"));

                userRedisCache.setAgentAudioState(seat_no_b, ESeatMediaState.WAITRET);
            }
        }
    }

    @Override
    @Async
    public void doSeatStateChange(Map<String,String> event) {
        String channelName = event.get("Channel-Name");
        String hostName = event.get("FreeSWITCH-Hostname");
        String channelUUID = event.get("Unique-ID");
        String seat_name = event.get("variable_"+ StaticValue.SeatName);


        String ir_session_id = event.get("variable_ir_session_id");
        String ir_target_agent = event.get("variable_ir_target_agent");
        String session_id = event.get("variable_session_id");
        // String hangup_cause = event.get("Hangup-Cause");
        String transfer_to_auto_return = event.get("variable_transfer_to_auto_return");

        //LOG.debug(transfer_to_auto_return + ":" + seat_name + "====" + event.get("Event-Name") + "---" + channelUUID
        //		+ "---- " + channelName + ":" + hostName);

        //LOG.debug("\n\nhuangqb add for test in doSeatStateChange event context: \n" + event + "\n\n");

        if (transfer_to_auto_return != null && transfer_to_auto_return.length() > 0
                && event.get("Event-Name").equals("CHANNEL_DESTROY")) {
            // 如果为在IVR处理过程中的信道挂机，则等待此信道返回的信道也要被KILL
            if (seat_list.containsKey(eslSupport.uuid_getvar(hostName, transfer_to_auto_return, StaticValue.SeatName))){
                eslSupport.uuid_kill(hostName, transfer_to_auto_return);
            }

        }

        if (null != ir_target_agent && ir_target_agent.length() > 0 && seat_list.containsKey(ir_target_agent)) {
            //监控用户信道的时间，因为在用户从排队系统获取到坐席后脚本设置ir_target_agent与ir_session_id
            if (ir_session_id.equals(userRedisCache.getSessionId(ir_target_agent))
                    &&event.get("Event-Name").equals("CHANNEL_DESTROY")
                    && EActionType.SERVICE.equals(userRedisCache.getRingType(ir_target_agent))){
                userRedisCache.setAgentAudioState(ir_target_agent, ESeatMediaState.IDLE);
                userRedisCache.setRingType(ir_target_agent, EActionType.NONE);
                userRedisCache.setAgentRequestType(ir_target_agent, EActionType.NONE);;
            }
        }

        if (null != seat_name && seat_name.length() > 0 && seat_list.containsKey(seat_name)) {
            // ESeatAudioState
            // curAudioState=userRedisCache.getSeatAudioState(seat_name);
            EActionType first = userRedisCache.getRingType(seat_name);
            UUID uuid = UUID.fromString(seat_list.get(seat_name));
            if (uuid != null) {

                SocketIOClient client = server.getClient(uuid);
                if (event.get("Event-Name").equals("CHANNEL_DESTROY")) {
                    channel_seat_map.remove(channelUUID);
                } else if (event.get("Event-Name").equals("CHANNEL_HANGUP")) {
                    String oldChannelUUID = userRedisCache.getAgentChannId(seat_name);
                    if (channelUUID.equals(oldChannelUUID)) {
                        // 处于监控状态时，坐席挂机不清里状态


//						if (ESeatActionType.MONITOR.equals(first)) {
//							userRedisCache.setSeatAudioState(seat_name, ESeatMediaState.WAITCON);
//							userRedisCache.setRequesterChannelUUID(seat_name, null);
//						} else {
                        userRedisCache.setAgentAudioState(seat_name, ESeatMediaState.IDLE);
                        userRedisCache.setRingType(seat_name, EActionType.NONE);
                        userRedisCache.setAgentRequestType(seat_name, EActionType.NONE);;
                        if (EActionType.SERVICE.equals(first)) {
                            String chatUUID = userRedisCache.getSessionId(seat_name);
                            String reqUUID = userRedisCache.getUserChannId(seat_name);
                            router.releaseCall(seat_name, reqUUID, chatUUID);
                        }
//						}

                        userRedisCache.setAgentChannHost(seat_name, null);
                        userRedisCache.setAgentChannId(seat_name, null);
                        if (client!=null){
                            EventParam eventParam =createTeleEventParam(seat_name);
                            client.sendEvent(StaticValue.onReleaseChannel,eventParam);
                        }
                    }
                } else if (event.get("Event-Name").equals("CHANNEL_ANSWER")) {

                    String oldChannelUUID = userRedisCache.getAgentChannId(seat_name);
                    if (null == oldChannelUUID) {
                        if (client!=null){
                            EventParam eventParam =createTeleEventParam(seat_name);
                            client.sendEvent(StaticValue.onCreateChannel,eventParam);
                        }
                    }
                    if (channelUUID.equals(oldChannelUUID)) {
                        if (EActionType.MONITOR.equals(first)) {
                            userRedisCache.setAgentAudioState(seat_name, ESeatMediaState.TALK);
                        }else{
                            userRedisCache.setAgentAudioState(seat_name, ESeatMediaState.ANSWER);
                        }
                    }
                } else if (event.get("Event-Name").equals("CHANNEL_CREATE")) {
                    channel_seat_map.put(channelUUID, seat_name);
                    String oldChannelUUID = userRedisCache.getAgentChannId(seat_name);
                    if (null != oldChannelUUID && !channelUUID.equals(oldChannelUUID)) {
                        eslSupport.uuid_kill(hostName, channelUUID);
                    }
                }
            }
        }
        String help_source = event.get("variable_"+ StaticValue.HelpSource);
        if (event.get("Event-Name").equals("CHANNEL_DESTROY")) {
            if (null != help_source && help_source.length() > 0 && seat_list.containsKey(help_source)) {

                EActionType second = userRedisCache.getAgentRequestType(help_source);
                if (EActionType.HELP_OUTSITE.equals(second) || EActionType.HELP_SEAT.equals(second)) {
                    // 检测到被求助人挂机机后，清理求助发起方的数据
                    if (userRedisCache.getTargetChannId(help_source).equals(channelUUID)) {
                        // 求助发起方记录的目标信道UUID必须和被求助人的信道UUID相同
                        userRedisCache.setAgentRequestType(help_source, EActionType.NONE);
                        userRedisCache.setTargetChannId(help_source, null);
                        userRedisCache.setTargetHost(help_source, null);
                        userRedisCache.setTargetName(help_source, null);

                    }
                }
            }
        }

    }

    @Override
    public void setFollowData(String seat,String sessionId, String date) {


        String seatChannelHost = userRedisCache.getAgentChannHost(seat);
        String seatChannelUUID = userRedisCache.getAgentChannId(seat);
        String reqChannelUUID = eslSupport.uuid_getvar(seatChannelHost, seatChannelUUID, "bridge_uuid");

        if (!eslSupport.uuid_exists(seatChannelHost, reqChannelUUID)) {
            return;
        }

        if (!eslSupport.uuid_exists(seatChannelHost, seatChannelUUID)) {
            return;
        }
        eslSupport.uuid_setvar(seatChannelHost, reqChannelUUID, "follow_data", date);

    }

    @Override
    public String getFollowData(String seat,String sessionId) {

        String seatChannelHost = userRedisCache.getAgentChannHost(seat);
        String seatChannelUUID = userRedisCache.getAgentChannId(seat);
        String reqChannelUUID = eslSupport.uuid_getvar(seatChannelHost, seatChannelUUID, "bridge_uuid");

        if (!eslSupport.uuid_exists(seatChannelHost, reqChannelUUID)) {
            return "";
        }

        if (!eslSupport.uuid_exists(seatChannelHost, seatChannelUUID)) {
            return "";
        }
        return eslSupport.uuid_getvar(seatChannelHost, reqChannelUUID, "follow_data");
    }

    @Override
    public void setChannelData(String seat,String sessionId, String key, String date) {


        String seatChannelHost = userRedisCache.getAgentChannHost(seat);
        String seatChannelUUID = userRedisCache.getAgentChannId(seat);
        String reqChannelUUID = eslSupport.uuid_getvar(seatChannelHost, seatChannelUUID, "bridge_uuid");

        if (!eslSupport.uuid_exists(seatChannelHost, reqChannelUUID)) {
            return;
        }

        if (!eslSupport.uuid_exists(seatChannelHost, seatChannelUUID)) {
            return;
        }
        eslSupport.uuid_setvar(seatChannelHost, reqChannelUUID, key, date);

    }

    @Override
    public String getChannelData(String seat,String sessionId, String key) {
        String seatChannelHost = userRedisCache.getAgentChannHost(seat);
        String seatChannelUUID = userRedisCache.getAgentChannId(seat);
        String reqChannelUUID = eslSupport.uuid_getvar(seatChannelHost, seatChannelUUID, "bridge_uuid");

        if (!eslSupport.uuid_exists(seatChannelHost, reqChannelUUID)) {
            return "";
        }

        if (!eslSupport.uuid_exists(seatChannelHost, seatChannelUUID)) {
            return "";
        }
        return eslSupport.uuid_getvar(seatChannelHost, reqChannelUUID, key);
    }

    @Override
    public void startRecordSingle(String seat,String sessionId, String file) {
        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);

        String seatChannelHost = userRedisCache.getAgentChannHost(seat);
        String seatChannelUUID = userRedisCache.getAgentChannId(seat);
        String reqChannelUUID = eslSupport.uuid_getvar(seatChannelHost, seatChannelUUID, "bridge_uuid");


        EventParam eventParam=createTeleEventParam(seat);

        if (!eslSupport.uuid_exists(seatChannelHost, reqChannelUUID)) {
            eventParam.setEventDesc("服务的对象不存在");
            client.sendEvent(StaticValue.onStopRecordSingleFail,eventParam);
            return;
        }

        if (!eslSupport.uuid_exists(seatChannelHost, seatChannelUUID)) {
            eventParam.setEventDesc("坐席通话不存在");
            client.sendEvent(StaticValue.onStopRecordSingleFail,eventParam );
            return;
        }
        String tmp = eslSupport.uuid_getvar(seatChannelHost, reqChannelUUID, "AI_RECORD_SINGLE");
        if (tmp.isEmpty()) {
            eslSupport.uuid_setvar(seatChannelHost, reqChannelUUID, "RECORD_READ_ONLY", "true");
          //  eslSupport.uuid_record(seatChannelHost, reqChannelUUID, true, sessionPath + file + ".wav");
            eslSupport.uuid_setvar(seatChannelHost, reqChannelUUID, "AI_RECORD_SINGLE", file);
            eslSupport.uuid_setvar(seatChannelHost, reqChannelUUID, "RECORD_READ_ONLY", "");
            eventParam.setValue(file);
            client.sendEvent(StaticValue.onStartRecordSingleSuccess, eventParam);
        } else {
            eventParam.setEventDesc("信道正在单声道录音");
            client.sendEvent(StaticValue.onStartRecordSingleFail, eventParam);
        }
    }

    @Override
    public void stopRecordSingle(String seat,String sessionId) {
        UUID uuid = UUID.fromString(seat_list.get(seat));
        SocketIOClient client = server.getClient(uuid);

        String seatChannelHost = userRedisCache.getAgentChannHost(seat);
        String seatChannelUUID = userRedisCache.getAgentChannId(seat);
        String reqChannelUUID = eslSupport.uuid_getvar(seatChannelHost, seatChannelUUID, "bridge_uuid");



        EventParam eventParam=createTeleEventParam(seat);;

        if (!eslSupport.uuid_exists(seatChannelHost, reqChannelUUID)) {
            eventParam.setEventDesc("服务的对象不存在");
            client.sendEvent(StaticValue.onStopRecordSingleFail,eventParam);
            return;
        }

        if (!eslSupport.uuid_exists(seatChannelHost, seatChannelUUID)) {
            eventParam.setEventDesc("坐席通话不存在");
            client.sendEvent(StaticValue.onStopRecordSingleFail,eventParam );
            return;
        }
        String file = eslSupport.uuid_getvar(seatChannelHost, reqChannelUUID, "AI_RECORD_SINGLE");
        if (file.isEmpty()) {
            eventParam.setEventDesc( "信道没有单声道录音");
            client.sendEvent(StaticValue.onStopRecordSingleFail,eventParam);
            return;
        } else {
         //   eslSupport.uuid_record(seatChannelHost, reqChannelUUID, false, sessionPath + file + ".wav");
            eslSupport.uuid_setvar(seatChannelHost, reqChannelUUID, "AI_RECORD_SINGLE", "");
            eventParam.setValue(file);
            client.sendEvent(StaticValue.onStopRecordSingleSuccess, eventParam);
        }
    }
}
