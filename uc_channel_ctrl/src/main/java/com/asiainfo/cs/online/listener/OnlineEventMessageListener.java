package com.asiainfo.cs.online.listener;

import com.alibaba.fastjson.JSON;
import com.asiainfo.cs.common.util.OcsNotify;
import com.asiainfo.cs.online.service.OnlineCustomNotify;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

@Service
public class OnlineEventMessageListener implements ChannelAwareMessageListener {
	@Resource(name = "onlineChannelControImpl")
	private OnlineCustomNotify notifyProcess;
	private static Logger LOG = LoggerFactory.getLogger(OnlineEventMessageListener.class);

	@Override
	public void onMessage(Message message, Channel channel) throws Exception {
		try{
			OcsNotify ocsNotify= JSON.parseObject(message.getBody(), OcsNotify.class);
			LOG.debug("access返回ocsNotify:"+ocsNotify.toString());
			notifyProcess.doCustomRequestEvent(ocsNotify);
			channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
		}catch (Exception e){
			OcsNotify ocsNotify= JSON.parseObject(message.getBody(), OcsNotify.class);
			int c=ocsNotify.getCount();
			System.out.println("第几次发送=-----========="+c);
			if(!StringUtils.isEmpty(c)){
				if(c>=5){
					System.out.println("进入死信队列=-----========="+c);
					channel.basicNack(message.getMessageProperties().getDeliveryTag(), false,false);
				}else {
					c+=1;
					ocsNotify.setCount(c);
					//手动进行应答
					channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
					//重新发送消息到队尾
					channel.basicPublish(message.getMessageProperties().getReceivedExchange(),
							message.getMessageProperties().getReceivedRoutingKey(), MessageProperties.TEXT_PLAIN,
							JSON.toJSONBytes(ocsNotify));
				}
			}else{
				System.out.println("没count直接进入死信队列=-----=========");
				channel.basicNack(message.getMessageProperties().getDeliveryTag(), false,false);
			}

		}

	}
}

