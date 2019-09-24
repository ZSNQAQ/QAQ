package com.asiainfo.cs.common.util;

import java.util.HashMap;
import java.util.Map;

public class ResultObject {
	private String result="+OK";
	private String response="";
	private int errorCode=0;
	private String errorDesc="";
	private Map<String,String> param =new HashMap<String,String>();

	
	
	public ResultObject(String result,String response){
		this.result=result;
		this.response=response;
	}
	
	public ResultObject(int errorCode,String errorDesc){
		this.errorCode=errorCode;
		this.errorDesc=errorDesc;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	public int getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
	public String getErrorDesc() {
		return errorDesc;
	}
	public void setErrorDesc(String errorDesc) {
		this.errorDesc = errorDesc;
	}
	public String getResponse() {
		return response;
	}
	public void setReponse(String response) {
		this.response = response;
	}
	public Map<String,String> getParam() {
		return param;
	}
	public void setParam(Map<String,String> param) {
		this.param = param;
	}

}
