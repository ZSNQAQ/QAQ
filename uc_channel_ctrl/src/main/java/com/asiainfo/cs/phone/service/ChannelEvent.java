package com.asiainfo.cs.phone.service;

import java.util.Map;

public interface ChannelEvent {

	public boolean doCustomRequestEvent(Map<String, String> event);
	public boolean doCustomRegisterUnRegisterEvent(Map<String, String> event); //huangqb add for check phone status..2019-4-11

}
