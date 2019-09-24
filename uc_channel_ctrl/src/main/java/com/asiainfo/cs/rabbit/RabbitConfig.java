package com.asiainfo.cs.rabbit;

import com.asiainfo.cs.online.listener.OnlineEventMessageListener;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
public class RabbitConfig {

    private static final Logger logger = LoggerFactory.getLogger(RabbitConfig.class);
    @Bean
    public String queueName(){
        return "cc.access."+UUID.randomUUID().toString();
    }

    public static final String REGISTER_EXCHANGE_NAME = "cc_ocs_exchange";



    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        return factory;
    }

    //动态创建queue，命名为：hostName.queue1【192.168.1.1.queue1】,并返回数组queue名称
    @Bean
    public String mqMsgQueues(ConnectionFactory connectionFactory, String queueName) throws AmqpException, IOException {

        Channel channel=connectionFactory.createConnection().createChannel(false);
        //声明交换机
        channel.exchangeDeclare(REGISTER_EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        //声明队列  boolean durable 是否持久化 ,boolean exclusive 是否排外的 , boolean autoDelete 是否自动删除
        //死信队列
        Map<String, Object> arguments = new HashMap<>();
//        // 设置队列内容纳消息的最大长度
//        arguments.put("x-max-length", 5);
        // 设置队列内的消息过期时间为5秒
//        arguments.put("x-message-ttl", 5000);
        // 设置死信队列中重新发布路由到交换机的名称
        arguments.put("x-dead-letter-exchange", "delay.exchange");
        // 设置当路由至私信交换机时消息的routing key
        arguments.put("x-dead-letter-routing-key", "delay.key");
        channel.queueDeclare(queueName, false, true, true, arguments);
        //将交换器与队列通过,路由键绑定
        channel.queueBind(queueName, REGISTER_EXCHANGE_NAME, queueName,arguments);
        return queueName;
    }

    //创建监听器，监听队列
    @Bean
    public SimpleMessageListenerContainer mqMessageContainer(String queueName, OnlineEventMessageListener onlineEventMessageListener, ConnectionFactory connectionFactory) throws AmqpException, IOException {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueueNames(queueName);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        container.setMessageListener(onlineEventMessageListener);//监听处理类
        return container;
   }

}