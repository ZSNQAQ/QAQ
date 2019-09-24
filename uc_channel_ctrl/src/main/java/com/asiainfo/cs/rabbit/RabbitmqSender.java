package com.asiainfo.cs.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

@Component
public class RabbitmqSender {
    private static final Logger LOG = LoggerFactory.getLogger(RabbitmqSender.class);
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public static final String FS_COMMANDS = "FS.COMMANDS";
    public static final String FS_EVENTS = "FS.EVENTS";
    public static final String 	QUEUE_CC_TO_LOG = "QUEUE_CC_TO_LOG";
    public static final String CC_LOG_EXCHANGE = "cc_log_exchange";

    /**
     * 发送消息
     * @param message  消息
     */
   public void sendDataToFS(String routekey,String message) {
        String uuid= UUID.randomUUID().toString();
        CorrelationData correlationId = new CorrelationData(uuid);
        //System.out.println("sendDataToFS1111==="+ message);
       Message msg = MessageBuilder.withBody(message.getBytes())
               .setContentType("text/json")
               .setContentEncoding("utf-8")
               .setMessageId(UUID.randomUUID()+"")
               .build();
        rabbitTemplate.convertAndSend(FS_COMMANDS, "cmd."+routekey,
              msg, correlationId);
   }
    /**
     * 发送消息
     * @param message  消息
     */
    public void sendDataToCC(String hostName,String subClass,String uniqueId,String message) {
        String uuid= UUID.randomUUID().toString();
        CorrelationData correlationId = new CorrelationData(uuid);
        System.out.println("sendDataToFS1111==="+ message);
        Message msg = MessageBuilder.withBody(message.getBytes())
                .setContentType("text/json")
                .setContentEncoding("utf-8")
                .setMessageId(UUID.randomUUID()+"")
                .build();
        rabbitTemplate.convertAndSend(FS_EVENTS, "FreeSWITCH."+hostName+".CUSTOM."+subClass+uniqueId,
                msg, correlationId);
    }
    /**
     * 发送消息
     * @param message  消息
     */
    public void sendDataToLog(String message) {
//        String uuid= UUID.randomUUID().toString();
//        CorrelationData correlationId = new CorrelationData(uuid);
//        //System.out.println("sendDataToLog222==="+ message);
//        rabbitTemplate.convertAndSend(CC_LOG_EXCHANGE, QUEUE_CC_TO_LOG,
//                message, correlationId);

        Message msg = null;
        try {
            msg = MessageBuilder.withBody(message.getBytes("utf-8"))
                    .setContentType("text/json")
                    .setContentEncoding("utf-8")
                    .setMessageId(UUID.randomUUID()+"")
                    .build();
        } catch (UnsupportedEncodingException e) {
            LOG.error(String.valueOf(e));
        }
        String uuid=UUID.randomUUID().toString();
        CorrelationData correlationId = new CorrelationData(uuid);
        rabbitTemplate.convertAndSend(CC_LOG_EXCHANGE, QUEUE_CC_TO_LOG,
                msg, correlationId);
    }
}
