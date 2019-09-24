package com.asiainfo.cs.log;


import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;


@Component
@Aspect
public class LogPointcut {
	
	@Pointcut("execution(* com.asiainfo.cs.online.service.OnlineCustomService.*(..))  || execution(* com.asiainfo.cs.online.service.OnlineChannelControl.*(..))  || execution(* com.asiainfo.cs.service.IntelligenRouter.*(..))  || execution(* com.asiainfo.cs.service.ChannelControl.*(..))  ||  execution(* com.asiainfo.cs.service.SeatControl.*(..))" )
	//@Pointcut("execution(* ai.cmc.freeswitch.service.IntelligenRouter.*(..))" )
	public void inServiceLayer() { }
      
}