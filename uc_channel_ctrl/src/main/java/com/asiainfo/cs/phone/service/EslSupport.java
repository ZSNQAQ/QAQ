package com.asiainfo.cs.phone.service;


import java.util.Map;
import java.util.Set;

public interface EslSupport {
	public void updateFreeSwitch(Map<String, String> map);
	public Set<String> getInstanceList();
	public String sendHttpApiCommand(String host, String cmd, String args);
	public String uuid_getvar(String host, String channel_uuid, String key);
	public String uuid_setvar(String host, String channel_uuid, String key, String value);
	public boolean uuid_exists(String host, String channel_uuid);
	public boolean uuid_kill(String host, String channel_uuid) ;
	public boolean uuid_transfer(String host, String uuid, String desc);
	public boolean uuid_dual_transfer(String host, String uuid, String descA, String descB);
	public boolean uuid_bridge(String host, String channel_a, String channel_b);
	public boolean uuid_recv_dtmf(String host, String channel, String data);
	public boolean uuid_send_dtmf(String host, String channel, String data);
	public boolean uuid_broadcast(String host, String channel, String data);
	public boolean uuid_record(String host, String channel, boolean start, String data);
	public boolean uuid_hold(String host, String channel_uuid, boolean hold);
	public boolean uuid_att_xfer(String host, String channel_uuid, String value);
	public String originate(String host, String param);
	public String bgapi(String host, String param);
	public boolean sofia_get_user_register_status(String host, String param); //增加sofia查询注册用户状态...
	public boolean uuid_play_tts_voice(String host, String channel_uuid, String ctxt);//增加baidu  tts放音调用...
	public boolean uuid_break(String host, String channel_uuid);//停止当前放音....

}
