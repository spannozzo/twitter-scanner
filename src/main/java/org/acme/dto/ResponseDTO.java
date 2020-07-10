package org.acme.dto;

import java.util.List;
import java.util.Map;

public class ResponseDTO {
	Map<Author, List<Message>> values;
	String traceId;
	
	public ResponseDTO(Map<Author, List<Message>> values, String traceId) {
		super();
		this.values = values;
		this.traceId = traceId;
	}
	
	public Map<Author, List<Message>> getValues() {
		return values;
	}
	public void setValues(Map<Author, List<Message>> values) {
		this.values = values;
	}
	public String getTraceId() {
		return traceId;
	}
	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}
	
	
}
