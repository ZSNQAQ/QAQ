package com.asiainfo.cs.config;

import com.asiainfo.cs.common.util.StaticValue;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;
import com.corundumstudio.socketio.store.RedissonStoreFactory;
import com.corundumstudio.socketio.store.StoreFactory;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@EnableRabbit
@Configuration
public class AppConfig {


	private static final Logger LOG = LoggerFactory.getLogger(AppConfig.class);
	private static String uuid=UUID.randomUUID().toString();
	static {
		try {
			uuid=InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//private static final String uuid="TEST";
	@Autowired
    Environment env;
	Redisson redisson;

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(new Jackson2JsonMessageConverter());
		return template;
	}
	//
	@Bean
	public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setMessageConverter(new Jackson2JsonMessageConverter());
		return factory;
	}
//


	@Bean(name="service_uuid")
	public String getServiceUUID(){
		return uuid;
	}
	
	
	@Bean(name="rpcQueue")
	public org.apache.activemq.command.ActiveMQQueue getRpcQueue(){
		return new org.apache.activemq.command.ActiveMQQueue("SEAT_RPC:"+uuid);
	}
	
	
	@Bean(name="seatEventMessageQueue")
	public org.apache.activemq.command.ActiveMQQueue getSeatEventMessageQueue(){
		return new org.apache.activemq.command.ActiveMQQueue("SEAT_EVENT:"+uuid);
	}
	
	@PostConstruct()
	public void init(){
	
		String address = env.getProperty("bean.redisson.address", "localhost:6379");
		String password = env.getProperty("bean.redisson.password", "");
		int db = Integer.valueOf(env.getProperty("bean.redisson.db", "0"));
		
		int poolsize=Integer.valueOf(env.getProperty("bean.redisson.pool","2"));
		int minIdle=Integer.valueOf(env.getProperty("bean.redisson.pool","1"));
		
		
		Config config = new Config();
		SingleServerConfig baseConfig=config.useSingleServer();
		baseConfig.setPassword(password.isEmpty() ? null : password).setAddress(address).setDatabase(db);
		baseConfig.setConnectionMinimumIdleSize(minIdle);
		baseConfig.setConnectionPoolSize(poolsize);
		baseConfig.setSubscriptionConnectionMinimumIdleSize(minIdle);
		baseConfig.setSubscriptionConnectionPoolSize(poolsize);
		config.setCodec(StringCodec.INSTANCE);
		redisson = (Redisson) Redisson.create(config);
	}

	@Bean(name="redisson")
	public Redisson getRedisson() {

		return redisson;

	}

	@Bean(name="channel_seat_map")
	public Map<String,String> getChannelSeatMap(){
		return redisson.getMap("cc:channel_seat_map");
	}

	@Bean(name="ir_routing_runtime")
	public Map<String,String> getRoutintRuntime(){
		return redisson.getMap(StaticValue.ROUTING_RUNTIME);
	}



//	@Bean(name="server_list")
//	public RMapCache<String,String> getServerList(){
//		RMapCache<String,String> map =redisson.getMapCache("cc:server_list");
//		try {
//			map.put(uuid, InetAddress.getLocalHost().getHostName(), 28, TimeUnit.SECONDS);
//		} catch (UnknownHostException e) {
//			map.put(uuid, uuid, 28, TimeUnit.SECONDS);
//		}
//		return map;
//	}
	@Bean(name="seat_list")
	public RMap<String,String> getServerSeatList(){
		RMap<String,String> map;
		map =redisson.getMap("cc:seat_list:"+uuid);
		map.clear();
		//map.put(uuid, uuid);
		map.expireAsync(28, TimeUnit.SECONDS);
		return map;
	}


//	@Bean(name="eventQueue")
//	public DelayQueue<DelayItem<EslEvent>> getEventQueue() {
//		return new DelayQueue<DelayItem<EslEvent>>();
//	}

	//监听列表
	@Bean(name="monitor_list")
	public RMap<String,String> getMonitorList() {
		return redisson.getMap("cc:monitor_list");
	}


    @Bean
    public SpringAnnotationScanner springAnnotationScanner(SocketIOServer socketServer) {
        return new SpringAnnotationScanner(socketServer);
    }

	@Bean
	public SocketIOServer getSocketIOServer() {



		com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();

		 String os = System.getProperty("os.name");
		 LOG.info("============OS:"+os);

		String epoll = env.getProperty("socketio.netty.UseLinuxNativeEpoll", "false");
		if (epoll.equalsIgnoreCase("true")&&os.equalsIgnoreCase("Linux")){
			config.setUseLinuxNativeEpoll(true);
		}


		String TcpNoDelay = env.getProperty("socketio.netty.TcpNoDelay", "true");
		if (TcpNoDelay.equalsIgnoreCase("true")){
			config.getSocketConfig().setTcpNoDelay(true);
		}else{
			config.getSocketConfig().setTcpNoDelay(false);
		}

		String SoLinger = env.getProperty("socketio.netty.SoLinger", "5");
		if (SoLinger.matches("\\d+"))
			config.getSocketConfig().setSoLinger(Integer.parseInt(SoLinger));

		String TcpKeepAlive = env.getProperty("socketio.netty.TcpKeepAlive", "false");
		if (TcpKeepAlive.equalsIgnoreCase("true")){
			config.getSocketConfig().setTcpKeepAlive(true);
		}else{
			config.getSocketConfig().setTcpKeepAlive(false);
		}

		String ReuseAddress = env.getProperty("socketio.netty.ReuseAddress", "false");
		if (ReuseAddress.equalsIgnoreCase("true")){
			config.getSocketConfig().setReuseAddress(true);
		}else{
			config.getSocketConfig().setReuseAddress(false);
		}
		String PingInterval = env.getProperty("socketio.config.PingInterval", "5000");
		if (PingInterval.matches("\\d+"))
			config.setPingInterval(Integer.parseInt(PingInterval));

		String PingTimeout = env.getProperty("socketio.config.PingTimeout", "20000");
		if (PingTimeout.matches("\\d+"))
			config.setPingTimeout(Integer.parseInt(PingTimeout));

		config.setHostname(env.getProperty("socketio.config.HostName", "0.0.0.0"));

		String Port = env.getProperty("socketio.config.Port", "9092");
		if (PingTimeout.matches("\\d+"))
			config.setPort(Integer.parseInt(Port));
		else
			config.setPort(9092);

		String context = env.getProperty("socketio.config.context", "/socket.io");
		config.setContext(context);


		StoreFactory clientStoreFactory =null;
		//StoreFactory clientStoreFactory =new MemoryStoreFactory();
		if (env.getProperty("socketio.config.StoreFactory", "memory").equalsIgnoreCase("redisson")){
			String address = env.getProperty("socketio.redisson.address", "localhost:6379");
			String password = env.getProperty("socketio.redisson.password", "");
			int db = Integer.valueOf(env.getProperty("socketio.redisson.db", "1"));
			int poolsize=Integer.valueOf(env.getProperty("socketio.redisson.pool","2"));
			int minIdle=Integer.valueOf(env.getProperty("socketio.redisson.pool","1"));

			Config redisson_config = new Config();
			SingleServerConfig singleConfig=redisson_config.useSingleServer();
			singleConfig.setPassword(password.isEmpty() ? null : password)
				.setAddress(address)
				.setDatabase(db);

			singleConfig.setConnectionMinimumIdleSize(minIdle);
			singleConfig.setConnectionPoolSize(poolsize);

			//redisson_config.useClusterServers().addNodeAddress(address).setDatabase(db).setPassword(password.isEmpty() ? null : password);
			RedissonClient redisson = Redisson.create(redisson_config);
			clientStoreFactory = new RedissonStoreFactory(redisson);
		}

		if (null!=clientStoreFactory)
			config.setStoreFactory(clientStoreFactory);
//		config.setAuthorizationListener(new AuthorizationListenerImpl());

		return new SocketIOServer(config);
	}
	
//	private class AuthorizationListenerImpl implements AuthorizationListener {
//
//	    @Override
//	    public boolean isAuthorized(HandshakeData data) {
//	    	LOG.debug("isAuthorized");
//	        return true;
//	    }
//
//	}

}
