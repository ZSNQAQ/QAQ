package com.asiainfo.cs.log.impl;

import com.asiainfo.cs.log.CommonLog;
import com.asiainfo.cs.log.entity.CommonLogEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class CommonLogImpl implements CommonLog {

	@Value("${log.redis.keyname:common_log}")
	private String keyname;
	
	@Autowired
	RedisTemplate redisTemplate;
	
	@PostConstruct
	void init(){
		
	
	}
	@Async
	@Override
	public void log(CommonLogEntity log) {
		BoundListOperations listRedisTemplate = redisTemplate.boundListOps(keyname);
		listRedisTemplate.leftPush(log);

	}

}
