package com.asiainfo.cs.phone.entity;//package com.asiainfo.cs.phone.entity;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class EventCommandObject {
//	private String host;
//	private String eventName;
//	private String eventSubClass;
//	private String uniqueId;
//	private String body;
//	private Map<String,String> headers=new HashMap<String,String>();
//	private List<String> generic;
//
//	public String getHost() {
//		return host;
//	}
//	public String getEventName() {
//		return eventName;
//	}
//	public String getEventSubClass() {
//		return eventSubClass;
//	}
//	public String getUniqueId() {
//		return uniqueId;
//	}
//	public String getBody() {
//		return body;
//	}
//	public List<String> getGeneric() {
//		return generic;
//	}
//	public void setHost(String host) {
//		this.host = host;
//	}
//	public void setEventName(String eventName) {
//		this.eventName = eventName;
//	}
//	public void setEventSubClass(String eventSubClass) {
//		this.eventSubClass = eventSubClass;
//	}
//	public void setUniqueId(String uniqueId) {
//		this.uniqueId = uniqueId;
//	}
//	public void setBody(String body) {
//		this.body = body;
//	}
//	public void setGeneric(List<String> generic) {
//		this.generic = generic;
//	}
//	public Map<String,String> getHeaders() {
//		return headers;
//	}
//	public void setHeaders(Map<String,String> headers) {
//		this.headers.putAll(headers);
//	}
//	public String putHeader(String key,String value) {
//		return this.headers.put(key, value);
//	}
//	public String removeHeader(String key) {
//		return this.headers.remove(key);
//	}
//
//}
