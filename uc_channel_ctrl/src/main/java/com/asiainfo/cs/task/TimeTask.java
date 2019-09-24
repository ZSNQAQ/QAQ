package com.asiainfo.cs.task;

import com.asiainfo.cs.common.util.StaticValue;
import com.asiainfo.cs.common.util.StaticValue.EMonitorType;
import com.asiainfo.cs.phone.service.ChannelEventTimeOut;
import com.asiainfo.cs.service.UserRedisCache;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.redisson.api.RMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class TimeTask {
	private static final Logger LOG = LoggerFactory.getLogger(TimeTask.class);
	DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	@Resource(name="seat_list")
	private RMap<String,String> seat_list;


	@Resource(name="ir_routing_runtime")
	private Map<String,String> routingRuntime;
	
	@Autowired
	private UserRedisCache userRedisCache;
	@Autowired
	private SocketIOServer server;
	final Timer timer = new HashedWheelTimer(100, TimeUnit.MILLISECONDS, 16);
	
	private TimerTask publishQueueinfo=new TimerTask() {
        public void run(Timeout timeout) throws Exception {
        	publishQueueInfo();
        }
    };
	
	private TimerTask serviceExpire=new TimerTask() {
        public void run(Timeout timeout) throws Exception {
        	serviceExpire();
        }
    };
    
    
	private TimerTask redisUserInfoExpire=new TimerTask() {
        public void run(Timeout timeout) throws Exception {
        	redisUserInfoExpire();
        }
    };
    
	@PostConstruct
	private void init(){
		 timer.newTimeout(serviceExpire,5, TimeUnit.SECONDS);
		 timer.newTimeout(redisUserInfoExpire,5, TimeUnit.SECONDS);
		 timer.newTimeout(publishQueueinfo,10, TimeUnit.SECONDS);
	}
	//@Scheduled(cron = "0/10 * * * * ?")
	public void publishQueueInfo(){
		try {
			server.getBroadcastOperations().sendEvent(StaticValue.onQueueInfo,routingRuntime.get(StaticValue.SKILL_INFO));
		} finally {
			timer.newTimeout(publishQueueinfo,10, TimeUnit.SECONDS);
		}
	}
	//@Scheduled(cron = "0/14 * * * * ?")
	public void serviceExpire() {
		try {
			seat_list.expireAsync(28, TimeUnit.SECONDS);
		} finally {
			timer.newTimeout(serviceExpire,14, TimeUnit.SECONDS);
		}
	}
	
	
	//@Scheduled(cron = "0/5 * * * * ?")
	public void redisUserInfoExpire() {
		try {
			for (final SocketIOClient client : server.getAllClients()) {
					if (client.has(StaticValue.SeatName)) {
						String seat = client.get(StaticValue.SeatName);
						if (seat != null) {
							userRedisCache.setAgentInfoExpire(seat);//延期
							if (!EMonitorType.NONE.equals(userRedisCache.getMonitorType(seat))
									&&!userRedisCache.agentExists(userRedisCache.getMonitorID(seat))
									){
								//如果班长不存在了，就将当前做的监控类型设置为未监控
								userRedisCache.setMonitorType(seat, EMonitorType.NONE);
							}
					
						}
					}
			}
		} finally {
			timer.newTimeout(redisUserInfoExpire,5, TimeUnit.SECONDS);
		}

	}


//	@Resource(name="eventQueue")
//    private DelayQueue<DelayItem<EslEvent>> queue;
//	@Autowired
//	private ChannelEvent eventProcess;
//	@Scheduled(cron = "0/1 * * * * ?")
//	public void checkEventTimeOut() {
//		try {
//			DelayItem<EslEvent> item;
//			while ((item=queue.poll())!=null) {
//				EslEvent event=item.getItem();
//				eventProcess.doCustomTimeOutEvent(event);
//				LOG.debug("Time Out:"+event.getEventHeaders().get("Event-Subclass")+event.toString());
//			}
//		} catch(Throwable ex) {
//			LOG.error(ex.getMessage());
//		}
//	}
	
	public void offer(final Map<String,String> event,long delay, TimeUnit unit,final ChannelEventTimeOut handle){
		 timer.newTimeout(new TimerTask() {
	        public void run(Timeout timeout) throws Exception {
	        	handle.doCustomTimeOutEvent(event);
	        }
	    },delay,unit);
	}

}
