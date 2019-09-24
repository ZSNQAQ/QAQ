package com.asiainfo.cs.log.entity;

import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;

public class AgentStateLogModel implements Serializable {

    private static final long serialVersionUID = 1L;

    //事件名称
    @JSONField(name="Event-Name")
    private String eventName;
    //子事件名称
    @JSONField(name="Event-Subclass")
    private String eventSubclass;
    //坐席工号ID
    @JSONField(name ="agent_id")
    private String agnetId;
    //目标坐席工号ID
    @JSONField(name ="target_id")
    private String targetId;
    //目标坐席签入工号ID
    @JSONField(name="target_signId")
    private String targetSignId;
    //目标坐席会话ID
    @JSONField(name="target_sessionId")
    private String targetChatId;
    //坐席工号ID
    @JSONField(name ="param")
    private String param;
    //坐席链接ID
    @JSONField(name="agent_signId")
    private String agentSignId;
    //当前坐席会话ID
    @JSONField(name="variable_session_Id")
    private String chatId;
    //事件类型
    private Integer eventCode;
    //事件类型
    private String eventContent;
    //操作结果 成功:1 失败:2
    private Integer result;
    //操作时间
    @JSONField(name="Event-Date-Timestamp")
    private Long operTime;
    //操作方式  自己:1 质检人:2
    @JSONField(name ="isQuality")
    private Integer operType;
    //强制操作人ID
    private String  qualityID;
    //签入ip
    private String  ip;
    //签入技能组
    private String skillId;
    //成功转或失败转 huangqb@2019-5-5 add...
    private String transtype;
    //签入分机号
    @JSONField(name="Core-UUID")
    private String  coreUUID;

    private int  count;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getEventSubclass() {
        return eventSubclass;
    }

    public void setEventSubclass(String eventSubclass) {
        this.eventSubclass = eventSubclass;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Integer getEventCode() {
        return eventCode;
    }

    public void setEventCode(Integer eventCode) {
        this.eventCode = eventCode;
    }

    public Integer getResult() {
        return result;
    }

    public void setResult(Integer result) {
        this.result = result;
    }

    public Long getOperTime() {
        return operTime;
    }

    public void setOperTime(Long operTime) {
        this.operTime = operTime;
    }

    public Integer getOperType() {
        return operType;
    }

    public void setOperType(Integer operType) {
        this.operType = operType;
    }

    public String getQualityID() {
        return qualityID;
    }

    public void setQualityID(String qualityID) {
        this.qualityID = qualityID;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getAgnetId() {
        return agnetId;
    }

    public void setAgnetId(String agnetId) {
        this.agnetId = agnetId;
    }

    public String getAgentSignId() {
        return agentSignId;
    }

    public void setAgentSignId(String agentSignId) {
        this.agentSignId = agentSignId;
    }

    public String getSkillId() {
        return skillId;
    }

    public void setSkillId(String skillId) {
        this.skillId = skillId;
    }

    public String getEventContent() {
        return eventContent;
    }

    public void setEventContent(String eventContent) {
        this.eventContent = eventContent;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public String getCoreUUID() {
        return coreUUID;
    }

    public void setCoreUUID(String coreUUID) {
        this.coreUUID = coreUUID;
    }

    public String getTargetSignId() {
        return targetSignId;
    }

    public void setTargetSignId(String targetSignId) {
        this.targetSignId = targetSignId;
    }

    public String getTargetChatId() {
        return targetChatId;
    }

    public void setTargetChatId(String targetChatId) {
        this.targetChatId = targetChatId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }
    public String getTranstype() {
        return transtype;
    }

    public void setTranstype(String transtype) {
        this.transtype = transtype;
    }
}
