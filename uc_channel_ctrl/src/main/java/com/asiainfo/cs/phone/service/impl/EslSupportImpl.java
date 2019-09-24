package com.asiainfo.cs.phone.service.impl;

import com.asiainfo.cs.phone.entity.FreeswitchInfo;
import com.asiainfo.cs.phone.service.EslSupport;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


@Component
@Lazy
public class EslSupportImpl implements EslSupport {
	private static final Logger LOG = LoggerFactory.getLogger(EslSupportImpl.class);

	final private Map<String, FreeswitchInfo> fsInfo=new HashMap<String, FreeswitchInfo>();

	@Override
	public void updateFreeSwitch(Map<String, String> event) {
//		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("sys.properties");
//		Properties p = new Properties();
//		String port = p.getProperty("fs_port");
		FreeswitchInfo freeswitchInfo= new FreeswitchInfo();
		freeswitchInfo.setCore_uuid(event.get("Core-UUID"));
		String ip = event.get("FreeSWITCH-IPv4");
		String host = event.get("FreeSWITCH-Hostname");
		freeswitchInfo.setHost(host);

		freeswitchInfo.setHttpapi("http://"+ip+":"+"8090"+"/api/");
		freeswitchInfo.setLastHeart(System.currentTimeMillis());
		fsInfo.put(host,freeswitchInfo);
		//System.out.println("111111=="+ JSONObject.toJSONString(fsInfo));
	}

	@Override
	public Set<String> getInstanceList() {
		return fsInfo.keySet();
	}

	@Override
	public String sendHttpApiCommand(String host, String cmd, String args) {
		String http = fsInfo.get(host).getHttpapi();
		LOG.debug("http:" + http + "\n\n");
        System.out.println("http======="+http);

		try {
			String param=URLEncoder.encode(args, "utf-8");
			URL url = new URL(http +cmd + "?" + param.replace("+", "%20"));
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.connect();
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;
			StringBuffer sb = new StringBuffer();
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			reader.close();
			connection.disconnect();

			String result=sb.toString();
			LOG.debug("sendHttpApiCommand REQ:["+http +cmd + "?" +args+"]RESP:"+result);
			return result;
		} catch (IOException e) {
			return null;
		}
	}
	@Override
	public String uuid_getvar(String host, String channel_uuid, String key) {
		if (null==host||null==channel_uuid)
			return null;
		String args=channel_uuid+" "+key;
		String result= sendHttpApiCommand(host,"uuid_getvar",args);
		if (null==result) return "";
		if ("_undef_".equals(result)) return "";
		if (result.startsWith("-ERR"))  return "";
		 return result;
	}
	
	
	
	@Override
	public String uuid_setvar(String host, String channel_uuid, String key, String value) {
		if (null==host||null==channel_uuid)
			return null;
		String args=channel_uuid+" "+key+" "+value;
		String result=  sendHttpApiCommand(host,"uuid_setvar",args);
		 return result;
	}

	@Override
	public boolean uuid_exists(String host, String channel_uuid) {
		if(host==null||channel_uuid==null){
			return false;
		}
		String args=channel_uuid;
		if (sendHttpApiCommand(host,"uuid_exists",args).equalsIgnoreCase("true"))
			return true;
		else
			return false;
	}


	@Override
	public boolean uuid_kill(String host, String channel_uuid) {
		if (null==host||null==channel_uuid)
			return false;
		if (sendHttpApiCommand(host,"uuid_kill",channel_uuid).startsWith("+OK"))
			return true;
		else
			return false;
	}
	
	
	public boolean uuid_transfer(String host, String uuid,String desc){
		if (null==host||null==uuid)
			return false;
		String args=String.format("%s %s", uuid,desc);
		if (sendHttpApiCommand(host,"uuid_transfer",args).startsWith("+OK"))
			return true;
		else
			return false;
	}
	
	@Override
	public boolean uuid_dual_transfer(String host, String uuid,String descA,String descB){
		if (null==host||null==uuid)
			return false;
		String args=String.format("%s %s %s", uuid,descA,descB);
		if (sendHttpApiCommand(host,"uuid_dual_transfer",args).startsWith("+OK"))
			return true;
		else
			return false;
	}
	@Override
	public boolean uuid_bridge(String host, String channel_a, String channel_b){
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		String args=String.format("%s %s", channel_a,channel_b);
		String result=sendHttpApiCommand(host,"uuid_bridge",args);
		if (result.startsWith("+OK"))
			return true;
		else
			return false;
	}

	@Override
	public String originate(String host, String param) {
		if (null==host)
			return null;
		return sendHttpApiCommand(host,"originate",param);
	}

	@Override
	public String bgapi(String host, String param) {
		if (null==host)
			return null;
		return sendHttpApiCommand(host,"bgapi",param);
	}

	@Override
	public boolean uuid_recv_dtmf(String host, String channel_uuid, String data) {
		if (null==host||null==channel_uuid)
			return false;
		String args=String.format("%s %s", channel_uuid,data);
		if (sendHttpApiCommand(host,"uuid_recv_dtmf",args).startsWith("+OK"))
			return true;
		else
			return false;
	}

	@Override
	public boolean uuid_send_dtmf(String host, String channel_uuid, String data) {
		if (null==host||null==channel_uuid)
			return false;
		String args=String.format("%s %s", channel_uuid,data);
		if (sendHttpApiCommand(host,"uuid_send_dtmf",args).startsWith("+OK"))
			return true;
		else
			return false;
	}

	@Override
	public boolean uuid_broadcast(String host, String channel_uuid, String data) {
		if (null==host||null==channel_uuid)
			return false;
		String args=String.format("%s %s both", channel_uuid,data);
		if (sendHttpApiCommand(host,"uuid_broadcast",args).startsWith("+OK"))
			return true;
		else
			return false;
	}

	@Override
	public boolean uuid_hold(String host, String channel_uuid, boolean hold) {
		if (null == host || null == channel_uuid)
			return false;
		String args = "";
		if (hold)
			args = String.format("%s", channel_uuid);
		else
			args = String.format("off %s", channel_uuid);
		if (sendHttpApiCommand(host, "uuid_hold", args).startsWith("+OK"))
			return true;
		else
			return false;
	}
	@Override
	public boolean uuid_att_xfer(String host, String channel_uuid, String value){
		if (null == host || null == channel_uuid)
			return false;
		String args = "";
		args = String.format("%s %s", channel_uuid,value);
		if (sendHttpApiCommand(host, "uuid_att_xfer", args).startsWith("+OK"))
			return true;
		else
			return false;
	}
	@Override
	public boolean uuid_record(String host, String channel_uuid,boolean start, String data) {
		if (null == host || null == channel_uuid)
			return false;
	
		if (null==data||data.isEmpty())
			return false;
		if (start){
			if (sendHttpApiCommand(host, "uuid_record", channel_uuid+" start "+data).startsWith("+OK"))
				return true;
			else
				return false;
		}else{
			if (sendHttpApiCommand(host, "uuid_record", channel_uuid+" stop "+data).startsWith("+OK"))
				return true;
			else
				return false;
		}
			
	}
    /**
	 * 功能描述：给定用户注册号码，查出当前用户在freeswitch上的注册状态。
	 * 版本：v0.1
	 * 作者：huangqb3
	 * 最后更新日期:2019-5-6
    * */
	public boolean sofia_get_user_register_status(String host, String param)
	{
		String rexml = sendHttpApiCommand(host,"sofia","xmlstatus profile internal reg " + param);
		if (!rexml.isEmpty())
		{
			//LOG.debug("result:\n" + rexml + "\n\n");
			Document doc = null;
			try {
				doc = DocumentHelper.parseText(rexml); // 将字符串转为XML
				Element rootElt = doc.getRootElement(); // 获取根节点
				//System.out.println("根节点：" + rootElt.getName()); // 拿到根节点的名称
                //LOG.debug("\n\n--------->root:" + rootElt.getName()+"<---------------" + "\n\n");
				Iterator iter = rootElt.elementIterator("registrations");
				if (!iter.hasNext())
				{
					LOG.debug("user: "+ param + " is not register...\n\n");
					return false;
				}
				else
				{
					Element regEle = (Element) iter.next();
					Iterator regIt = regEle.elementIterator("registration");
					//LOG.debug("\n\n--------->register:" + regEle.getName()+"<---------------" + "\n\n");
					if (!regIt.hasNext())
					{
						LOG.debug("user: "+ param + " is not register...\n\n");
						return false;
					}
					while(regIt.hasNext())
					{
						Element recordEle = (Element) regIt.next();
						String re_status = recordEle.elementTextTrim("status");
						String ping_status = recordEle.elementTextTrim("ping-status");
						//LOG.debug("user: "+ param + " is register," +
						//		   "re_status:"+ re_status +
						//		   "ping_status:" + ping_status + "\n\n");
						return true;
					}
				}
				//Iterator iter1 = rootElt.elementIterator("row1");
			} catch (Exception e) {
				LOG.debug("sofia xmlstatus profile internal reg " + param + "Exec failed\n");
				return false;
			}
		}
		return false;
	}

//增加baidu 调用
public boolean uuid_play_tts_voice(String host,String channel_uuid,String ctxt)
{
	//String baidu_tts="http://tsn.baidu.com/text2audio?tex=%s&lan=zh&cuid=freeswitch&ctp=1&per=0&tok=%s&aue=5&.r8";
	if (null==host || channel_uuid==null)
	{
		LOG.debug("uuid_play_tts_voice error\n");
		return false;
	}

	String tok = sendHttpApiCommand(host,"global_getvar","tok");
	if (tok.equals(""))
	{
		LOG.debug("get tok error...");
	}
	String baidu_tts = "http://tsn.baidu.com/text2audio?tex=" +ctxt+
			           "&lan=zh&cuid=freeswitch&ctp=1&per=0&tok="+tok + "&aue=5&.r8";
	if (uuid_broadcast(host,channel_uuid,baidu_tts))
	{
		LOG.debug("----------->tts 放音成功<-------------------\n\n");
		return true;
	}
  LOG.debug("---------------->tts 防疫失败<------------------------\n\n");
  return  false;
}

//停止当前通道的放音....
public boolean uuid_break(String host,String channel_uuid)
{
	if (null == host || channel_uuid == null)
	{
		LOG.debug("uuid_break error,host or channel_uuid is null\n");
		return false;
	}
	String result = sendHttpApiCommand(host,"uuid_break",channel_uuid);
	if (result.startsWith("+OK"))
		return true;
	else
	    return false;
}

}
