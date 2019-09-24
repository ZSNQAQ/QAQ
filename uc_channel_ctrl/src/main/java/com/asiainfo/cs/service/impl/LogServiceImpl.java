package com.asiainfo.cs.service.impl;

import com.asiainfo.cs.service.LogService;
import org.aspectj.lang.JoinPoint;

import java.text.SimpleDateFormat;
import java.util.Date;


public class LogServiceImpl implements LogService {
	public void log() {
		System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " *******Log*********");
	}
	public void logArg(JoinPoint point) {
		StringBuffer sb = new StringBuffer();
		// 获取连接点所在的目标对象
		Object obj = point.getTarget();
		// 获取连接点的方法签名对象
		String method = point.getSignature().getName();
		// 获取连接点方法运行时的入参列表
		Object[] args = point.getArgs();
		sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append(" ");
		sb.append(obj.toString().substring(0, obj.toString().indexOf('@')));
		sb.append(".").append(method).append(" ");
		sb.append("Args:[");
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				Object o = args[i];
				sb.append(o);
				if (i < args.length - 1) {
					sb.append(",");
				}
			}
		}
		sb.append("]");
		System.out.println(sb.toString());
	}
	public void logArgAndReturn(JoinPoint point, Object returnObj) {
		StringBuffer sb = new StringBuffer();
		// 获取连接点所在的目标对象
		Object obj = point.getTarget();
		// 获取连接点的方法签名对象
		String method = point.getSignature().getName();
		// 获取连接点方法运行时的入参列表
		Object[] args = point.getArgs();
		sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append(" ");
		sb.append(obj.toString().substring(0, obj.toString().indexOf('@')));
		sb.append(".").append(method).append(" ");
		sb.append("Args:[");
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				Object o = args[i];
				sb.append(o);
				if (i < args.length - 1) {
					sb.append(",");
				}
			}
		}
		sb.append("]").append(" ");
		sb.append("Ret:[").append(returnObj).append("]");
		System.out.println(sb.toString());
	}
}
