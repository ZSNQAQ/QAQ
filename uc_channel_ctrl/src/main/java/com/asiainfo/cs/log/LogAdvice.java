package com.asiainfo.cs.log;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class LogAdvice {
		private static final Logger LOG = LoggerFactory.getLogger(LogAdvice.class);
       /**
       * 在方法开始前纪录
       * @param jp
       */
       @Before("com.asiainfo.cs.log.LogPointcut.inServiceLayer()" )
       public void logInfo(JoinPoint jp) {
            String className = jp.getThis().toString();
            String methodName = jp.getSignature().getName();   //获得方法名
            Object[] args = jp.getArgs();  //获得参数列表
            
            
//            System. out.println("=====================================" );
//            System. out.println("====位于：" +className);
//            System. out.println("====调用" +methodName+"方法-开始！");
//             if(args.length <=0){
//                  System. out.println("====" +methodName+"方法没有参数");
//            } else{
//                   for(int i=0; i<args.length; i++){
//                  System. out.println("====参数  " +(i+1)+"："+args[i]);
//              }
//            }
//            System. out.println("=====================================" );
            String result="\r\n=====================================\r\n";
            result+="className:"+className+" methodName:"+methodName+" params:";
            for(int i=0; i<args.length; i++){
            	result+="["+args[i]+"]";
            }
            result+="\r\n=====================================";
            LOG.debug(result);

      }
      
       /**
       * 在方法结束后纪录
       * @param jp
       */
       @After("com.asiainfo.cs.log.LogPointcut.inServiceLayer()" )
       public void logInfoAfter(JoinPoint jp) {
//            System. out.println("=====================================" );
//            System. out.println("====" +jp.getSignature().getName()+"方法-结束！");
//            System. out.println("=====================================" );
      }
      
}
