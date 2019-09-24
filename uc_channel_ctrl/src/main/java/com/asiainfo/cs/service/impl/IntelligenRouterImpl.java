package com.asiainfo.cs.service.impl;

import com.asiainfo.cs.service.IntelligenRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IntelligenRouterImpl implements IntelligenRouter {
	private static final Logger LOG = LoggerFactory.getLogger(IntelligenRouterImpl.class);
	@Resource(name="restTemplate")
	private RestTemplate template;

	@Value("${ir.host}")
	private String baseurl;
	@Value("${oneframe.host}")
	private String oneframeurl;

	@Override
	public boolean setSkills(String agentId, String skills, String signId, String ip) {
		String url=baseurl+"AgentOperate/setSkills?agentId={agentId}&signedSkills={skills}&signId={signId}&ip={ip}";
		if (skills==null) skills="";
		LOG.debug("IntelligenRouter:"+url);
		try {
			Map<?,?>  result=template.getForObject(url, Map.class,agentId,skills,signId,ip);
			LOG.debug(result.toString());
			return Integer.valueOf(1).equals(result.get("resultVal"));
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return false;
	}

	@Override
	public boolean signIn(String agentId,String skills,String signId,String ip) {
		String url=baseurl+"AgentOperate/signIn?agentId={agentId}&signedSkills={skills}&signId={signId}&ip={ip}";
		if (skills==null) skills="";
		LOG.debug("IntelligenRouter:"+url);
		try {
			Map<?,?>  result=template.getForObject(url, Map.class,agentId,skills,signId,ip);
			LOG.debug(result.toString());
			return Integer.valueOf(1).equals(result.get("resultVal"));
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return false;
	}

	@Override
	public Map<?, ?> getUserName(String agentId) {
		String url=oneframeurl+"login/getPlatFormById?agentId={agentId}";
		LOG.debug("IntelligenRouter:"+url);
		Map<?,?> result = new HashMap<>();
		try {
			result=template.getForObject(url,Map.class,agentId);
			LOG.debug(result.toString());
			return result;
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return result;
	}

	@Override
	public boolean signOut(String agentId) {
		String url=baseurl+"AgentOperate/signOut?agentId={agentId}";
		LOG.debug("IntelligenRouter:"+url);
		try {
			Map<?, ?>  result=template.getForObject(url, Map.class,agentId);
			LOG.debug(	result.toString());
			return Integer.valueOf(1).equals(result.get("resultVal"));
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return false;
	}

	
	@Override
	public boolean answerSuccess(String agentId, String uniqueId,String sessionId) {
		String url=baseurl+"AgentOperate/answerSuccess?agentId={seat}&uniqueId={uniqueId}&sessionId={sessionId}";
		LOG.debug("IntelligenRouter:"+url);
		try {
			Map<?, ?>  result=template.getForObject(url, Map.class,agentId,uniqueId,sessionId);
			LOG.debug(	result.toString());
			return Integer.valueOf(1).equals(result.get("resultVal"));
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return false;
		
	}

	@Override
	public boolean releaseCall(String agentId, String uniqueId,String sessionId) {
		// TODO 修改地址
		String url=baseurl+"AgentOperate/releaseCall?agentId={agentId}&uniqueId={uniqueId}&sessionId={sessionId}";
		LOG.debug("IntelligenRouter:"+url);
		try {
			Map<?, ?>  result=template.getForObject(url, Map.class,agentId,uniqueId,sessionId);
			LOG.debug(	result.toString());
			return Integer.valueOf(1).equals(result.get("resultVal"));
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return false;
	}


	@Override
	public boolean answerMore(String agentId) {
		String url=baseurl+"AgentOperate/answerMore?agentId={agentId}";
		LOG.debug("IntelligenRouter:"+url);
		try {
			Map<?, ?> result=template.getForObject(url, Map.class,agentId);
			LOG.debug(	result.toString());
			return Integer.valueOf(1).equals(result.get("resultVal"));
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return false;
	}

	@Override
	public List<?> qryAuthorizedSkills(String agentId) {
		String url=baseurl+"PlatformAccount/qryAuthorizedSkills?seatId={agentId}";
		try {
			List<?> result=template.getForObject(url.toString(), List.class,agentId);
			return result;
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return null;
	}	
	@Override
	public List<?> qrySignedSkills(String agentId) {
		String url=baseurl+"PlatformAccount/qrySignedSkills?agentId={agentId}";
		try {
			List<?> result=template.getForObject(url.toString(), List.class,agentId);
			return result;
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return null;
	}

///routing/PlatformAccount/qryAuthorizedSkills?seatId=1000286
	@Override
	public String doRoutingAction(Map<String, String> param) {
		String url=baseurl+"/"+param.get("action")+"?true=true";
		StringBuffer sb=new StringBuffer(url);
		for(String key:param.keySet()){
			sb.append(String.format("&%s={%s}",key, key));
		}
		try {
			String result=template.getForObject(sb.toString(), String.class,param);
			LOG.debug(	result.toString());
			return result;
		} catch (RestClientException e) {
			LOG.error(e.toString());
		}
		return null;
	}



}
