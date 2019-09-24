package com.asiainfo.cs.service;

import org.aspectj.lang.JoinPoint;

public interface LogService {
	public void log();
	public void logArg(JoinPoint point);
	public void logArgAndReturn(JoinPoint point, Object returnObj);
}
