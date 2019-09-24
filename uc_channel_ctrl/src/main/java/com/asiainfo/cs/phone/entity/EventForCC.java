package com.asiainfo.cs.phone.entity;

import com.alibaba.fastjson.annotation.JSONField;

public class EventForCC {
    @JSONField(name="Event-Name")
    private String eventName;
    private String host;
    @JSONField(name="Event-Subclass")
    private String eventSubClass;
    @JSONField(name="unique_id")
    private String unique_id;
    @JSONField(name="local_unique_id")
    private String local_unique_id;

    public String getLocal_unique_id() {
        return local_unique_id;
    }

    public void setLocal_unique_id(String local_unique_id) {
        this.local_unique_id = local_unique_id;
    }

    @JSONField(name="FreeSWITCH-Hostname")
    private String FreeSWITCH_Hostname;
    private String session_id;
    private String target_agent;
    private String target_uuid;
    private String source_name;
    private String source_uuid;
    private String type;
    private String response_type;
    private String action;
    private String seatB_UUID;
    @JSONField(name="SeatName")
    private String seatName;

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getEventSubClass() {
        return eventSubClass;
    }

    public void setEventSubClass(String eventSubClass) {
        this.eventSubClass = eventSubClass;
    }

    public String getUnique_id() {
        return unique_id;
    }

    public void setUnique_id(String unique_id) {
        this.unique_id = unique_id;
    }


    public String getSession_id() {
        return session_id;
    }

    public void setSession_id(String session_id) {
        this.session_id = session_id;
    }

    public String getTarget_agent() {
        return target_agent;
    }

    public void setTarget_agent(String target_agent) {
        this.target_agent = target_agent;
    }

    public String getSource_name() {
        return source_name;
    }

    public void setSource_name(String source_name) {
        this.source_name = source_name;
    }

    public String getSource_uuid() {
        return source_uuid;
    }

    public void setSource_uuid(String source_uuid) {
        this.source_uuid = source_uuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getResponse_type() {
        return response_type;
    }

    public void setResponse_type(String response_type) {
        this.response_type = response_type;
    }

    public String getFreeSWITCH_Hostname() {
        return FreeSWITCH_Hostname;
    }

    public void setFreeSWITCH_Hostname(String freeSWITCH_Hostname) {
        FreeSWITCH_Hostname = freeSWITCH_Hostname;
    }

    public String getTarget_uuid() {
        return target_uuid;
    }

    public void setTarget_uuid(String target_uuid) {
        this.target_uuid = target_uuid;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSeatB_UUID() {
        return seatB_UUID;
    }

    public void setSeatB_UUID(String seatB_UUID) {
        this.seatB_UUID = seatB_UUID;
    }

    public String getSeatName() {
        return seatName;
    }

    public void setSeatName(String seatName) {
        this.seatName = seatName;
    }
}
