package com.asiainfo.cs.online.listener;

import com.asiainfo.cs.common.util.NotifyPacketProto;
import com.asiainfo.cs.online.service.OnlineCustomNotify;
import com.asiainfo.cs.service.UserRedisCache;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.listener.SessionAwareMessageListener;

import javax.annotation.Resource;
import javax.jms.*;

public class OnlineEventSwitchListener  implements SessionAwareMessageListener<BytesMessage>  {
	//private static final Map<String,Destination> dest=new HashMap<String,Destination>();
	private static final Logger LOG = LoggerFactory.getLogger(OnlineEventSwitchListener.class);
	
	@Resource(name = "onlineChannelControImpl")
	private OnlineCustomNotify notifyProcess;
	@Autowired
	private UserRedisCache userRedisCache;
	@Override
	public void onMessage(BytesMessage message, Session session) throws JMSException {
		BytesMessage bytesMsg = (BytesMessage) message;
		try {
			int len = (int) bytesMsg.getBodyLength();
			byte[] data = new byte[len];	
			bytesMsg.readBytes(data);
			NotifyPacketProto.NotifyPacket notify =NotifyPacketProto.NotifyPacket.parseFrom(data);	
			//LOG.debug("Receive Message:"+notify.toString());
			String receiver=notify.getReceiver();
			
			if (userRedisCache.agentExists(receiver)){
				String serviceUUID=userRedisCache.getAgentServiceUUID(receiver);	
				Destination destination=new org.apache.activemq.command.ActiveMQQueue("SEAT_EVENT:"+serviceUUID);
				MessageProducer producer = session.createProducer(destination);    
			    producer.send(message);  
			}else{
				LOG.debug("Drop Message:"+notify.toString());
				//throw new RuntimeException("Agent Is Not Exists");
			}
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
