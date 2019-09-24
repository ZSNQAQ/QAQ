package com.asiainfo.cs.common.util;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;


public class PublicUtils {
	private static final SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
	private static final Logger LOG = LoggerFactory.getLogger(PublicUtils.class);
	
//	@Resource(name="restTemplate")
//    public void setTemplate(RestTemplate template){
//        PublicUtils.template = template;
//    }
//    private static RestTemplate template; 
//	public static String httpGetData(String httpurl) {
//		
//		
//		try {
//			String result=template.getForObject(httpurl, String.class);
//			LOG.debug("httpGetData REQ:["+httpurl+"]RESP:"+result);
//			return result;
//		} catch (HttpClientErrorException e) {
//			LOG.error("httpGetData REQ:["+httpurl+"]ERROR:"+e.toString());
//			return null;
//		}
//	}
	public static String getDateTime(){
		return dateFormater.format(new Date());
	}
	
	public static StringMap getAppFrameResult(String xml) {
		StringMap result=new StringMap();
		try {
			SAXReader sax = new SAXReader();// 创建一个SAXReader对象
			Document document = sax.read(new ByteArrayInputStream(xml.getBytes("GBK")));

			Element root = document.getRootElement();// 获取根节点
			for (Iterator it = root.elementIterator(); it.hasNext();) {
				  Element item = (Element) it.next();
				  if (item.getName().equalsIgnoreCase("p")){
					  result.put(item.attributeValue("n"), item.getText());
				  }

			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	public static void main(String[] args) {
		Map<String,String> result=getAppFrameResult("<?xml version ='1.0' encoding = 'GBK'?><UD><p n=\"RSLTVAL\">2</p><p n=\"RSLTMSG\">错误数据</p></UD>");
		System.out.println(result);
	}
}
