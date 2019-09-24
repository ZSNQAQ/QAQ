package com.asiainfo.cs.common.util;

import com.fasterxml.jackson.annotation.JsonValue;

public interface StaticValue {
	public static final String RSLTVAL="RSLTVAL";
	public static final String RSLTMSG="RSLTMSG";
	//前端方法名
	public static final String setSkills="setSkills";
	public static final String login="login";
	public static final String logout="logout";
	public static final String setSeatState="setSeatState";
	public static final String transferToSeat="transferToSeat";
	public static final String setStateOnHangup="setStateOnHangup";
	public static final String transferToAuto="transferToAuto";
	public static final String transferToOutsite="transferToOutsite";
	public static final String transferToQueue="transferToQueue";
	public static final String transferToGroupQueue="transferToGroupQueue";
	public static final String helpToQueue="helpToQueue";
	public static final String helpToSeat="helpToSeat";
	public static final String helpToOutsite="helpToOutsite";
	public static final String callSeat="callSeat";
	public static final String callOutsite="callOutsite";
	public static final String agentSendInfo="agentSendInfo";
	public static final String listen="listen";
	public static final String whisper="whisper";
	public static final String insert="insert";
	public static final String intercept="intercept";
	public static final String release="release";
	public static final String answer="answer";
	public static final String cancel="cancel";
	public static final String forceSignOut="forceSignOut";
	public static final String forceSayFree="forceSayFree";
	public static final String forceSayBusy="forceSayBusy";
	public static final String forceRelease="forceRelease";
	public static final String hold="hold";
	public static final String getOnlineSeat="getOnlineSeat";
	public static final String getSeatInfo="getSeatInfo";
	public static final String getOnlineSeatInfo="getOnlineSeatInfo";
	public static final String getSeatChatInfo="getSeatChatInfo";
	public static final String setFollowData="setFollowData";
	public static final String getFollowData="getFollowData";
	public static final String getChannelData="getChannelData";
	public static final String setChannelData="setChannelData";

	public static final String answerMore="answerMore";
	public static final String doRoutingAction="doRoutingAction";
	public static final String OriginalUserId="OriginalUserId";
	public static final String OriginalNumber="OriginalNumber";//huangqb add for Original number 2019-4-15

	//事件名
	public static final String onQueueInfo="onQueueInfo";
	

	
	public static final String onSetSkillsFail="onSetSkillsFail";
	public static final String onSetSkillsSuccess="onSetSkillsSuccess";

	public static final String onLoginFail="onLoginFail";
	public static final String onLoginSuccess="onLoginSuccess";

	public static final String onLogoutFail="onLogoutFail";
	public static final String onLogoutSuccess="onLogoutSuccess";
	
	
	public static final String onSeatStateChange="onSeatStateChange";
	
	public static final String onReleaseChannel="onReleaseChannel";
	public static final String onMediaStateChange="onMediaStateChange";
	public static final String onCreateChannel="onCreateChannel";
	
	
	public static final String onRing="onRing";
	public static final String onWaitConnection="onWaitConnection";
	public static final String onWaitAnswer="onWaitAnswer";
	public static final String onAnswer="onAnswer";
	public static final String onTalk="onTalk";
	public static final String onWaitReturn="onWaitReturn";	
	public static final String onClose="onClose";
	
	
	public static final String onAnswerFail="onAnswerFail";
	public static final String onAnswerSuccess="onAnswerSuccess";

	public static final String onReleaseFail="onReleaseFail";
	public static final String onReleaseSuccess="onReleaseSuccess";
	
	
	
	public static final String onRequestCancel="onRequestCancel";
	public static final String onRequestCancelFail="onRequestCancelFail";
	public static final String onRequestCancelSuccess="onRequestCancelSuccess";
	
	public static final String onRequestSeat="onRequestSeat";

	public static final String onTransToSeatFail="onTransToSeatFail";
	public static final String onTransToSeatProcess="onTransToSeatProcess";
	public static final String onTransToSeatSuccess="onTransToSeatSuccess";
	
	public static final String onTransToAutoFail="onTransToAutoFail";
	public static final String onTransToAutoSuccess="onTransToAutoSuccess";
	public static final String onTransToAutoReturn="onTransToAutoReturn";
	

	public static final String onTransToQueueFail="onTransToQueueFail";
	public static final String onTransToQueueProcess="onTransToQueueProcess";
	public static final String onTransToQueueSuccess="onTransToQueueSuccess";

	
	public static final String onTransToOutFail="onTransToOutFail";
	public static final String onTransToOutProcess="onTransToOutProcess";
	public static final String onTransToOutSuccess="onTransToOutSuccess";
	
	public static final String onHelpToSeatFail="onHelpToSeatFail";
	public static final String onHelpToSeatProcess="onHelpToSeatProcess";
	public static final String onHelpToSeatSuccess="onHelpToSeatSuccess";
	
	
	
	public static final String onHelpToQueueFail="onHelpToQueueFail";
	public static final String onHelpToQueueProcess="onHelpToQueueProcess";
	public static final String onHelpToQueueSuccess="onHelpToQueueSuccess";
	
	
	public static final String onHelpToOutsiteFail="onHelpToOutsiteFail";
	public static final String onHelpToOutsiteProcess="onHelpToOutsiteProcess";
	public static final String onHelpToOutsiteSuccess="onHelpToOutsiteSuccess";
	
	
	public static final String onCallOutsiteFail="onCallOutsiteFail";
	public static final String onCallOutsiteProcess="onCallOutsiteProcess";
	public static final String onCallOutsiteSuccess="onCallOutsiteSuccess";
	
	
	public static final String onCallSeatFail="onCallSeatFail";
	public static final String onCallSeatProcess="onCallSeatProcess";
	public static final String onCallSeatSuccess="onCallSeatSuccess";
	
	public static final String onListenSuccess="onListenSuccess";
	public static final String onListenFail="onListenFail";
	public static final String onWhisperFail="onWhisperFail";
	public static final String onWhisperSuccess="onWhisperSuccess";
	public static final String onInterceptFail="onInterceptFail";
	public static final String onInterceptSuccess="onInterceptSuccess";
	public static final String onInsertFail="onInsertFail";
	public static final String onInsertSuccess="onInsertSuccess";
	public static final String onCleanMonitor="onCleanMonitor";
	
	
	
	public static final String onForceReleaseFail="onForceReleaseFail";
	public static final String onForceReleaseSuccess="onForceReleaseSuccess";
	public static final String onForceSignOutFail="onForceSignOutFail";
	public static final String onForceSignOutSuccess="onForceSignOutSuccess";
	public static final String onForceSayFreeSuccess="onForceSayFreeSuccess";
	public static final String onForceSayFreeFail="onForceSayFreeFail";
	public static final String onForceSayBusySuccess="onForceSayBusySuccess";
	public static final String onForceSayBusyFail="onForceSayBusyFail";
	public static final String onSendInfoSuccess="onSendInfoSuccess";
	
	public static final String onHoldSuccess="onHoldSuccess";
	public static final String onHoldFail="onHoldFail";

	public static final String onFollowData="onFollowData";
	//huangqb add for softphone not online
	public static final String onSoftPhoneRegSuccessful="onSoftPhoneRegSuccessful"; //软终端注册成功，上报一次。
	public static final String onSoftPhoneUNRegSuccessful="onSoftPhoneUNRegSuccessful";//软终端离线通知，上报一次。
	//end of huangqb add 2019-5-5

//	public static final String onGetChannelDataFail="onGetChannelDataFail";
//	public static final String onSetChannelDataFail="onSetChannelDataFail";
//	public static final String onGetFollowDataFail="onGetFollowDataFail";
//	public static final String onSetFollowDataFail="onSetFollowDataFail";
//	public static final String onSetFollowDataSuccess="onSetFollowDataSuccess";
	
	public static final String onStartRecordSingleFail="onStartRecordSingleFail";
	public static final String onStartRecordSingleSuccess="onStartRecordSingleSuccess";
	public static final String onStopRecordSingleFail="onStopRecordSingleFail";
	public static final String onStopRecordSingleSuccess="onStopRecordSingleSuccess";

	
	
	//IR KEY
	public static String IR_PREFIX =  "ir:";
    public static String ROUTING_RUNTIME = IR_PREFIX + "routing:runtime";
    public static String SKILL_INFO = IR_PREFIX + "skill:queueinfo";
    public static String CC_PREFIX =  "cc:";
    
    
	//Redis Map Key
	public static final String OnlineUUID="OnlineUUID";
	public static final String Target="Target";
	public static final String TargetSessionId="TargetSessionId";
	
	public static final String Type="Type";	
	public static final String ServiceUUID="ServiceUUID";
	public static final String LoginTimestamp="LoginTimestamp";
	
	//坐席ID
	public static final String SeatName="SeatName";
	//坐席的用户状态状态
	public static final String SeatState="SeatState";
	//坐席挂机后状态设置标志
	public static final String stateOnRelease="stateOnRelease";
	//坐席所在的CC主机
	public static final String SeatHost="SeatHost";
	//坐席的SOCKETIO的唯一标示
	public static final String SeatUUID="SeatUUID";
	//坐席的信道名
	public static final String ChannelName="ChannelName";
	//当前活动的坐席信道ID
	public static final String ChannelUUID="ChannelUUID";
	//当前活动的坐席信道所在的Freeswitch主机名
	public static final String ChannelHost="ChannelHost";
	//坐席挂机后状态
	public static final String StateOnHangup="StateOnHangup";
	//坐席IP
	public static final String AgentIP="AgentIP";
	//坐席登录用户名
	public static final String AgentLoginName="AgentLoginName";
	//请求者的信道ID
	public static final String RequesterChannelUUID="RequesterChannelUUID";
	//请求者信道所在的Freeswitch主机名
	public static final String RequesterHostName="RequesterHostName";
	//请求者的Name
	public static final String RequesterName="RequesterName";
	
	
	//目标信道ID
	public static final String TargetChannelUUID="TargetChannelUUID";
	//目标信道所在的Freeswitch主机名
	public static final String TargetHostName="TargetHostName";
	//目标的ID
	public static final String TargetName="TargetName";

	
	//源信道ID
	public static final String SourceChannelUUID="SourceChannelUUID";
	//源信道所在的Freeswitch主机名
	public static final String SourceHostName="SourceHostName";
	//源的ID
	public static final String SourceName="SourceName";
	
	//对话ID
	public static final String SessionId="SessionId";
	//坐席向外发出的请求的标识
	public static final String SeatRequestId="SeatRequestId";

	public static final String Data="Data";
	public static final String Key="Key";
	public static final String Value="Value";
	public static final String AccessType="AccessType";
	public static final String Info="Info";
	
	public static final String HelpSource="HelpSource";
	
	public static final String InitialRdnis="InitialRdnis";
	public static final String InitialAni="InitialAni";
	public static final String SignedSkills="SignedSkills";
	
	
	public enum EMonitorType {
		NONE(0,"正常"),LISTEN(1,"监听"),WHISPER(0,"耳语"),INTERCEPT(0,"拦截"),INSERT(0,"插话");
		private int _value;
		private String _desc;
		private EMonitorType(int value,String desc) {
			_value = value;
			_desc=desc;
		}

		public int value() {
			return _value;
		}
		public String desc(){
			return _desc;
		}
	}
	public static final String MonitorType="MonitorType";

	public static final String MonitorID="MonitorID";
	/*	
	 * 坐席话机状态
	 * 振铃 RING（坐席没有确认，坐席信道没有建立）
	 * 等待连接 WAITCON(坐席确认，坐席信道没有建立）
	 * 等待应答 WAITANS(坐席确认，坐席信道建立但没有应答)
	 * 已应答 ANSWER(坐席确认，坐席信道应答,但没有桥接其他信道)，
	 * 已桥接 TALK(坐席确认，坐席信道应答，且桥接)，
	 * 等待桥接 WAITRET
	 * 空闲 IDEA
	 */
	public enum ESeatMediaState {
		IDLE(-1,"空闲"),
		BLOCK(0,"阻塞"),
		RING(1,"振铃"), 
		WAITCON(2,"等待连接"), 
		WAITANS(3,"等待应答"), 
		ANSWER(4,"已应答"), 
		TALK(5,"已桥接"), 
		WAITRET(6,"等待桥接");
		private int _value;
		private String _desc;
		private ESeatMediaState(int value,String desc) {
			_value = value;
			_desc=desc;
		}

		public int value() {
			return _value;
		}
		public String desc(){
			return _desc;
		}
	}
	
	public enum ESeatState {
		free(-1,"空闲"),acw(0,"整理"), busy(1,"忙"),keep(2,"不变");
		private int _value;
		private String _desc;
		private ESeatState(int value,String desc) {
			_value = value;
			_desc=desc;
		}

		public int value() {
			return _value;
		}
		public String desc(){
			return _desc;
		}
	}
	
	public static final String SeatAudioState="SeatAudioState";

	/*
	 * 坐席的请求类型
	 * 用户排队 SERVICE(统一接入)
	 * 挂起转自助服务 AUTO(将用户转移到IVR，并等待用户返回）
	 * 成功转人工服务 SEAT(将用户成功转其他人工服务,转移成功后释放当前坐席)
	 * 求助 HELP(挂起用户，求助到其他坐席)
	 * 
	 * 内部呼叫 INNER(在无服务状况下 呼叫其他坐席）
	 * 挂起 HOLD(暂时挂起用户与坐席)
	 * 质检呼叫 MONITOR
	 * 外部呼叫 OUTSITE(在无服务状况下 外部呼叫）
	 * 无请求 NONE
	 * 
	 */
	//SERVICE 1
	//TRUNS_QUEUE 2
	//TRUNS_SEAT 3

	public enum EActionType {
		NONE(0,"无振铃"),
		SERVICE(1,"统一接入振铃"),
		TRUNS_QUEUE(2,"转接队列振铃"),
		TRUNS_SEAT(3,"转接坐席振铃"),
		TRUNS_AUTO(4,"转移到自动"),
		TRUNS_OUTSITE(5,"转移到外部"),
		HELP_SEAT(6,"内部求助振铃"),
		HELP_OUTSITE(7,"求助到外部"),
		HELP_QUEUE(8,"求助到队列"), 
		INNER_CALL(9,"内部呼叫振铃"),
		DIRECT_CALL(10,"直接呼叫"),
		MONITOR(11,"监控振铃"),
		INSERT_CHAT(12,"插话振铃"),
		INTERCEPT(13,"拦截振铃");

		public static EActionType valueOf(int value){
			switch (value) {
			case 0: return NONE;
			case 1: return SERVICE;
			case 2: return TRUNS_QUEUE;
			case 3: return TRUNS_SEAT;
			case 4: return TRUNS_AUTO;
			case 5: return TRUNS_OUTSITE;
			case 6: return HELP_SEAT;
			case 7: return HELP_OUTSITE;
			case 8: return HELP_QUEUE;
			case 9: return INNER_CALL;
			case 10: return DIRECT_CALL;
			case 11: return MONITOR;
			case 12: return	INSERT_CHAT;
			case 13: return INTERCEPT;
			default:
				return null;
			}
		}
		private int _value;
		private String _desc;
		private EActionType(int value,String desc) {
			_value = value;
			_desc=desc;
		}

		public int value() {
			return _value;
		}
		public String desc(){
			return _desc;
		}
		
	}
	//RINGTYPE//接收到的请求 类型包括 SERVICE、TRUNS_SEAT、TRUNS_QUEUE、HELP_SEAT、INNER_CALL、MONITOR、CALL_OUTSITE
	public static final String SeatFirstAction="SeatFirstAction";
	public static final String RingType="RingType";
	 
	//坐席服务中进行的动作 TRUNS_AUTO、TRUNS_SEAT、TRUNS_QUEUE 、TRUNS_OUTSITE , HELP_SEAT，INNER,HELP_OUTSITE,、HOLD、NONE
	public static final String SeatSecondAction="SeatSecondAction";
	
	/*
	 * 坐席的请求方向
	 * 向外 OUT
	 * 向内 IN
	 */
	public enum ESeatRequestDirection{
		IN,OUT,NONE
	}
	public static final String SeatRequestDirection="SeatRequestDirection";
	
	

	public enum EAccessType {  
		ACCESS_TYPE_AGENT(0,"座席"),
		ACCESS_TYPE_TELE(1,"电话"),
		ACCESS_TYPE_WECHAT(2,"微信"),
		ACCESS_TYPE_WEB(3,"Web"),
		ACCESS_TYPE_WEB_MOBLE(4,"Web_Mobile"),
		ACCESS_TYPE_ANDROID(5,"APP"),
		ACCESS_TYPE_APP(6,"APP"),
		ACCESS_TYPE_WEIBO_SINA(7,"新浪微博"),
		ACCESS_TYPE_MAIL(8,"邮件"),
		ACCESS_TYPE_WEIBO_TENCENT(9,"腾讯微博"),
		ACCESS_TYPE_FETION(10,"飞信");
		private int _value;
		private String _desc;
		public static EAccessType valueOf(int value){
			switch (value) {
			case 0: return ACCESS_TYPE_AGENT;
			case 1: return ACCESS_TYPE_TELE;
			case 2: return ACCESS_TYPE_WECHAT;
			case 3: return ACCESS_TYPE_WEB;
			case 4: return ACCESS_TYPE_WEB_MOBLE;
			case 5: return ACCESS_TYPE_ANDROID;
			case 6: return ACCESS_TYPE_APP;
			case 7: return ACCESS_TYPE_WEIBO_SINA;
			case 8: return ACCESS_TYPE_MAIL;
			case 9: return ACCESS_TYPE_WEIBO_TENCENT;
			case 10: return ACCESS_TYPE_FETION;
			default:
				return null;
			}
			
		}
		private EAccessType(int value,String desc) {
			_value = value;
			_desc=desc;
		}
		@JsonValue  
	    public int getValue() {  
	        return _value;  
	    }  
	    public void setValue(int value) {  
	        this._value = value;  
	    } 
	    public int value(){
			return _value;
		}
		public String desc(){
			return _desc;
		}
	}

	
	
	public static int LOG_SIGNIN=301; //坐席签入
	public static int LOG_SIGNOUT=302; //坐席签出
	public static int LOG_SET_STATE=303; //坐席修改状态
	public static int LOG_FORCE_SIGNOUT=304; //坐席强制迁出其他坐席
	public static int LOG_FORCE_STATE=305; //坐席强制改变其他坐席状态（示闲、示忙）

	public static int LOG_ANSWER=311; //坐席应答
	public static int LOG_RELEASE=312; //坐席挂机
	public static int LOG_TRANS_TO_QUEUE=313; //坐席发起转接队列请求
	public static int LOG_TRANS_TO_SEAT=314; //坐席发起转接坐席请求
	public static int LOG_TRANS_TO_OUT=315; //坐席发起转接外部请求
	public static int LOG_TRANS_TO_AUTO=316; //坐席发起转接自动设备请求
	public static int LOG_HELP_TO_SEAT=317; //坐席发起求助到坐席请求
	public static int LOG_HELP_TO_OUT=318; //坐席发起求助到外部请求
	public static int LOG_INNER_CALL=319; //坐席发起呼叫其他坐席请求
	public static int LOG_DIRECT_CALL=320; //坐席发起呼叫外部请求
	public static int LOG_MONITOR=321; //坐席发起监听请求
	public static int LOG_WHISPER=321; //坐席对监听发起耳语请求
	public static int LOG_INTERCEPT=322; //坐席对监听发起拦截请求
	public static int LOG_INSERT=323; //坐席对监听发起插入请求
	public static int LOG_FORCE_RELEASE=324; //坐席对其他坐席发起强制挂机请求

	public static int LOG_RINGING=371; //坐席振铃请求（振铃原因，渠道SessionID，用户连接ID，坐席签入ID)
	public static int LOG_DEVICE_ANSWER=372; //坐席设备应答成功
	public static int LOG_DEVICE_TALKING=373; //坐席设备链接用户成功
	public static int LOG_DEVICE_WAITRET=374; //坐席设备等待用户返回
	public static int LOG_DEVICE_RELEASE=375; //坐席设备挂机


	/**
	 * 操作结果
	 */
	public static int RESULT_SUCCESS=1; //成功
	public static int RESULT_FAIL=2; //失败

	public static int OPERATOR_TYPE_SELF=1; //自己
	public static int OPERATOR_TYPE_OTHER=2; //他人

}
