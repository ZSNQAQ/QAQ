package com.asiainfo.cs.log.entity;

import java.io.Serializable;

public class CommonLogEntity implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
    private String uniqueId;
    private String sessionId;
    private String signId;
    private String agentId;
    private String targetId;
    private int busiType;
    private int resultType;
    private String resultContent;
    private Long dateTime;
    private String remark1;
    private String remark2;
    private String remark3;

    public CommonLogEntity(){
    	dateTime=System.currentTimeMillis();
    }
    
    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSignId() {
        return signId;
    }

    public void setSignId(String signId) {
        this.signId = signId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public int getBusiType() {
        return busiType;
    }

    public void setBusiType(int busiType) {
        this.busiType = busiType;
    }

    public int getResultType() {
        return resultType;
    }

    public void setResultType(int resultType) {
        this.resultType = resultType;
    }

    public String getResultContent() {
        return resultContent;
    }

    public void setResultContent(String resultContent) {
        this.resultContent = resultContent;
    }

    public Long getDateTime() {
        return dateTime;
    }

    public void setDateTime(Long dateTime) {
        this.dateTime = dateTime;
    }

    public String getRemark1() {
        return remark1;
    }

    public void setRemark1(String remark1) {
        this.remark1 = remark1;
    }

    public String getRemark2() {
        return remark2;
    }

    public void setRemark2(String remark2) {
        this.remark2 = remark2;
    }

    public String getRemark3() {
        return remark3;
    }

    public void setRemark3(String remark3) {
        this.remark3 = remark3;
    }

    @Override
    public String toString() {
        return "CommonLog{" +
                "uniqueId='" + uniqueId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", signId='" + signId + '\'' +
                ", agentId='" + agentId + '\'' +
                ", targetId='" + targetId + '\'' +
                ", busiType=" + busiType +
                ", resultType=" + resultType +
                ", resultContent='" + resultContent + '\'' +
                ", dateTime=" + dateTime +
                ", remark1='" + remark1 + '\'' +
                ", remark2='" + remark2 + '\'' +
                ", remark3='" + remark3 + '\'' +
                '}';
    }
}