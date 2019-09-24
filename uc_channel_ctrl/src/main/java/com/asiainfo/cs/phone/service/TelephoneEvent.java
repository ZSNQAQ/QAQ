package com.asiainfo.cs.phone.service;


import java.util.Map;

public interface TelephoneEvent {

	public void doSeatStateChange(Map<String, String> event);
	public void doBridgeStateChange(Map<String, String> event);
}
