package com.asiainfo.cs.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;

public class KeepSeatLiveExecutor extends TimerTask {
	private static final Logger LOG = LoggerFactory.getLogger(KeepSeatLiveExecutor.class);
	
	@Override
	public void run() {
		LOG.debug("KeepSeatLiveExecutor:run");
	}

}
