package com.asiainfo.cs.online.service.impl;

import com.asiainfo.cs.online.service.OnlineCustomService;
import com.asiainfo.cs.service.UserRedisCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OnlineCustomServiceImpl implements OnlineCustomService {
	private static final Logger LOG = LoggerFactory.getLogger(OnlineCustomServiceImpl.class);
	@Resource(name="restTemplate")
    private RestTemplate template;
	@Resource(name="queueName")
    private String routerkey;

	@Autowired
	private UserRedisCache userRedisCache;
	
//	#var url = window.ocsBusiServer + "/business/com.asiainfo.cs.ocs.core.web.OcsAgentOperAction?action=" + method;
//	#            data += "&ipAddress=10.21.38.22&operatorId=" + agentId;
//	#method=innerTrans       operatorId chatId   targetId(SeatID||QueueID)   operType(QUEUE_TRANS/AGENT_TRANS)
//	#method=releaseCall      operatorId chatId   chatState(0)
//	#method=answer           operatorId chatId   customerId   ringType
//	#method=signIn           operatorId uuid 
//	#method=signOut           operatorId   
	@Value("${ocs.host}")
	private String baseurl;
	@Override
	public Map<?,?> answer(String agentId, String chatId, String customerId,int ringType,int accessType) {
//		String ip=userRedisCache.getAgentIP(agentId);
		String ip=routerkey;
		String url=baseurl+"answer?ipAddress={ip}&operatorId={agentId}&chatId={chatId}&customerId={customerId}&ringType={ringType}&accessType={accessType}";
		System.out.println("应答========ipAddress:"+ip+",agentId:"+agentId+",customerId:"+customerId+",ringType:"+ringType);
		LOG.debug(url);
		Map<?,?> result = new HashMap<>();
		try {
			result=template.getForObject(url, Map.class,ip,agentId,chatId,customerId,ringType,accessType);
			LOG.debug(result.toString());
			return result;
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return result;

	}
//	public static final int ENQUEUE_TYPE_ACCESS      = 1;   //统一接入
//	public static final int ENQUEUE_TYPE_QUEUE_TRANS = 2;   //转接队列
//	public static final int ENQUEUE_TYPE_AGENT_TRANS = 3;   //转接座席
	@Override
	public Map<?,?> innerTrans(String accessType,String agentId, String chatId, String targetId, int operType) {
		String ip=routerkey;
		String url=baseurl+"innerTrans?accessType={accessType}&ipAddress={ip}&operatorId={agentId}&chatId={chatId}&targetId={targetId}&operType={ringType}";
		LOG.debug(url);
		Map<?,?> result = new HashMap<>();
		try {
			result=template.getForObject(url, Map.class,accessType,ip,agentId,chatId,targetId,operType);
			LOG.debug(result.toString());
			return result;
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return result;
	}

	@Override
	public Map<?,?> releaseCall(String agentId, String chatId, int ringType,int accessType) {
//		String ip=userRedisCache.getAgentIP(agentId);
		String ip=routerkey;
		String url=baseurl+"releaseCall?ipAddress={ip}&agentId={agentId}&chatId={chatId}&ringType={ringType}&accessType={accessType}";
		LOG.debug(url);
		Map<?,?> result = new HashMap<>();
		try {
			result=template.getForObject(url, Map.class,ip,agentId,chatId,ringType,accessType);
			LOG.debug(result.toString());
			return result;
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return result;
	}

	@Override
	public Map<?,?> signIn(String agentId,String onlineMediaUUID,String signId) {
//		String ip=userRedisCache.getAgentIP(agentId);
		String ip=routerkey;
		String url=baseurl+"signIn?ipAddress={ip}&agentId={agentId}&onlineMediaUUID={onlineMediaUUID}&signId={signId}";
		LOG.debug(url);
		Map<?,?> result = new HashMap<>();
		try {
			 result=template.getForObject(url,Map.class,ip,agentId,onlineMediaUUID,signId);
			LOG.debug(result.toString());
			return result;
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return result;
	}

	@Override
	public Map<?,?> signOut(String agentId) {
		String ip=routerkey;
		String url=baseurl+"signOut?ipAddress={ip}&agentId={agentId}";
		LOG.debug(url);
		Map<?,?> result = new HashMap<>();
		try {
			result=template.getForObject(url, Map.class,ip,agentId);
			LOG.debug(result.toString());
			return result;
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return result;
	}
	@Override
	public Map<?,?> forceSignOut(String agentId,List<String> chatIdlist) {
		Map<String, Object> hashMap = new HashMap<String, Object>();
		String ip=routerkey;
		hashMap.put("ipAddress",ip);
		hashMap.put("agentId",agentId);
		hashMap.put("chatIdlist",chatIdlist);
		String url=baseurl+"forceSignOut";
		Map<?,?> result = new HashMap<>();
		try {
			result = template.postForObject(url, hashMap, Map.class);
			LOG.debug(result.toString());
			return result;
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return result;
	}
	@Override
	public  Map<?,?> listenChat(String agentId, String targetAgent, String targetChatId,String originalUserId) {
		String ip=routerkey;
		String url=baseurl+"listen?ipAddress="+ip+"&chatId="+targetAgent+"&agentId="+agentId+"&targetAgent="+targetChatId+"&OriginalUserId="+originalUserId+"";
		LOG.debug(url);
		Map<?,?> result = new HashMap<>();
		try {
			result=template.getForObject(url, Map.class,ip,targetChatId,agentId,targetAgent);
			LOG.debug(result.toString());
			return result;
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return result;
	}
	@Override
	public Map<?,?> insertChat(String agentId, String chatId, String targetAgent,String originalUserId) {
		Map<?,?> result = new HashMap<>();
		String ip=routerkey;
		String url=baseurl+"insertChat?ipAddress="+ip+"&chatId="+chatId+"&agentId="+agentId+"&targetAgent="+targetAgent+"&OriginalUserId="+originalUserId+"";
		LOG.debug(url);
		try {
			result=template.getForObject(url, Map.class,ip,chatId,agentId,targetAgent);
			LOG.debug(result.toString());
			return result;
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return result;
	}
	@Override
	public Map<?,?> interceptChat(String agentId, String chatId, String targetAgent) {
		String ip=routerkey;
		String url=baseurl+"interceptChat?ipAddress={ip}&chatId={chatId}&agentId={agentId}&targetAgent={targetAgent}";
		LOG.debug(url);
		Map<?,?> result = new HashMap<>();
		try {
			result=template.getForObject(url, Map.class,ip,chatId,agentId,targetAgent);
			LOG.debug(result.toString());
			return result;
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return result;
	}
	
	
	@Override
	public String whisperChat(String agentId, String chatId, String targetAgent) {
		String result="<?xml version ='1.0' encoding = 'GBK'?><UD><p n=\"RSLTVAL\">2</p><p n=\"RSLTMSG\"></p></UD>";
		String url=baseurl+"action=whisperChat&chatId={chatId}&agentId={agentId}&targetAgent={targetAgent}";
		LOG.debug(url);
		try {
			result=template.getForObject(url, String.class,chatId,agentId,targetAgent);
			LOG.debug(result.toString());
			return result;
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return result;
	}
	@Override
	public String getChatRelation(String chatId) {
		String result="<?xml version ='1.0' encoding = 'GBK'?><UD><p n=\"RSLTVAL\">2</p><p n=\"RSLTMSG\"></p></UD>";
		String url=baseurl+"action=getChatRelation&chatId={chatId}";
		LOG.debug(url);
		try {
			result=template.getForObject(url, String.class,chatId);
			LOG.debug(result.toString());
			return result;
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return result;
	}
	@Override
	public String getChatList(String agentId) {
		String result="<?xml version ='1.0' encoding = 'GBK'?><UD><p n=\"RSLTVAL\">2</p><p n=\"RSLTMSG\"></p></UD>";
		String url=baseurl+"action=getChatList&agentId={agentId}";
		LOG.debug(url);
		try {
			result=template.getForObject(url, String.class,agentId);
			LOG.debug(result.toString());
			return result;
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return result;
	}
	@Override
	public Map<?,?> forceReleaseCall(String agentId,String targetChatId,String currentAgent,int ringType) {
		String ip=routerkey;
		String url=baseurl+"forceReleaseCall?ipAddress={ip}&agentId={agentId}&targetChatId={targetChatId}&currentAgent={currentAgent}&ringType={chatState}";
		LOG.debug(url);
		Map<?,?> result = new HashMap<>();
		try {
			result=template.getForObject(url, Map.class,ip,agentId,targetChatId,currentAgent,ringType);
			LOG.debug(result.toString());
			return result;
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return result;
	}

//	@Override
//	public String transferToAuto(String agentId, String chatId, String customerId,int ringType){
//		String ip=userRedisCache.getAgentIP(agentId);
//		String result="<?xml version ='1.0' encoding = 'GBK'?><UD><p n=\"RSLTVAL\">2</p><p n=\"RSLTMSG\"></p></UD>";
//		String url=baseurl+"action=doPwdVerify&ipAddress={ip}&operatorId={agentId}&chatId={chatId}&customerId={customerId}&ringType={ringType}";
//		LOG.debug(url);
//		try {
//			result=template.getForObject(url, String.class,ip,agentId,chatId,customerId,ringType);
//			LOG.debug(result);
//			return result;
//		} catch (RestClientException e) {
//			LOG.error(e.toString());
//		}
//		return result;
//
//
//
//	}

}
