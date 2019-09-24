package com.asiainfo.cs.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;

public class StringMap extends HashMap<String, String> {
	private static final long serialVersionUID = -5440578750644124196L;
	public StringMap(String jsonStr){
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			StringMap dataMap = objectMapper.readValue(jsonStr, StringMap.class);
			this.putAll(dataMap);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public StringMap(){

	}

}
