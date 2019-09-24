package com.asiainfo.cs.phone.listener;

import com.alibaba.fastjson.JSON;
import com.asiainfo.cs.phone.service.ChannelEvent;
import com.asiainfo.cs.phone.service.EslSupport;
import com.asiainfo.cs.phone.service.TelephoneEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;

@Component

public class PhoneReceiver {

private static Logger loggor = LoggerFactory.getLogger(PhoneReceiver.class);
    @PostConstruct
    public void init(){
        loggor.debug("dfdfdfdfdfdfdfdfdfd");
    }

    @Resource(name="phoneChannelControImpl")
    private ChannelEvent channelEvent;

    @Resource(name = "seat_list")
    private Map<String, String> seat_list;

    @Autowired
    private TelephoneEvent telephoneEvent;
    @Autowired
    private EslSupport eslSupport;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(ignoreDeclarationExceptions="true",autoDelete = "true",durable="false",exclusive="false"),
            exchange = @Exchange(value = "FS.EVENTS",ignoreDeclarationExceptions="true",autoDelete="false",durable="true",type="topic"),
            key = "FreeSWITCH.#",ignoreDeclarationExceptions="true")
    )
    public void fshandler(Message message) {

      //loggor.debug("\n\nhuangqb add for test:get msg:\n"+ "msg body:\n" + new String(message.getBody())+ "\n\n");


        Map<String,String> event = (Map) JSON.parse(message.getBody());

        String eventName = event.get("Event-Name");
        if (!StringUtils.isEmpty(event.get("FreeSWITCH-IPv4"))) {
            eslSupport.updateFreeSwitch(event);
        }

        if (eventName.equals("CUSTOM")) {
            String target_agent = event.get("target_agent");
            if ((null!=target_agent)&&seat_list.containsKey(target_agent)) {
                channelEvent.doCustomRequestEvent(event);
            }
            //增加对freeswitch的unregister和register... TODO huangqb@2019-4-11 modify
            String event_subclass = event.get("Event-Subclass");
            if (event_subclass.equals("sofia::unregister")|| event_subclass.equals("sofia::register"))
            {
                //loggor.debug("\n\nhuangqb add for test ,get register event and got agent register status\n\n");
               // loggor.debug("\n\nhuangqb add for test:get msg:\n"+ "msg body:\n" + new String(message.getBody())+ "\n\n");
                channelEvent.doCustomRegisterUnRegisterEvent(event);
                //eslSupport.sofia_get_user_register_status("kvm81_20","9996005");
            }
            //end of huangqb modify...

        } else if (eventName.equals("CHANNEL_BRIDGE") || eventName.equals("CHANNEL_UNBRIDGE")) {
            telephoneEvent.doBridgeStateChange(event);
        } else if (eventName.equals("HEARTBEAT")){
//            eslSupport.updateFreeSwitch(event);
        }else {
            telephoneEvent.doSeatStateChange(event);
        }
    }
}

//@Service
//public class PhoneReceiver implements ChannelAwareMessageListener {
//
//    private Logger loggor = LoggerFactory.getLogger(PhoneReceiver.class);
//
//    @Resource(name="phoneChannelControImpl")
//    private ChannelEvent channelEvent;
//
//    @Resource(name = "seat_list")
//    private Map<String, String> seat_list;
//
//    @Autowired
//    private TelephoneEvent telephoneEvent;
//    @Autowired
//    private EslSupport eslSupport;
//
//    @Override
//    public void onMessage(Message message, Channel channel) {
//
//        System.out.println("111111111111111" + message);
//        byte[] body = message.getBody();
//        String bodyToString = new String(body);
//        Map<String,String> event = (Map) JSON.parse(bodyToString);
//
//        String eventName = event.get("Event-Name");
//        eslSupport.updateFreeSwitch(event);
//        if (eventName.equals("CUSTOM")) {
//            String target_agent = event.get("target_agent");
//            System.out.println("111111111111111target_agent" + target_agent);
//            System.out.println("seat_list" + JSONObject.toJSONString(seat_list));
//
//            if ((null!=target_agent)&&seat_list.containsKey(target_agent)) {
//                channelEvent.doCustomRequestEvent(event);
//            }
//        } else if (eventName.equals("CHANNEL_BRIDGE") || eventName.equals("CHANNEL_UNBRIDGE")) {
//            telephoneEvent.doBridgeStateChange(event);
//        } else if (eventName.equals("HEARTBEAT")){
////            eslSupport.updateFreeSwitch(event);
//        }else {
//            telephoneEvent.doSeatStateChange(event);
//        }
//    }
//
//}
