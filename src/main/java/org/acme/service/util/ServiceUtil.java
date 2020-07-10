package org.acme.service.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.enterprise.context.Dependent;

@Dependent
public class ServiceUtil {
	
	public String encodeValue(String value) {
		try {
			return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(ex.getCause());
		}
	}
	public String decodeValue(String value) {
		try {
			String decodedUrl= URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
			
			return decodedUrl;
			
			
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(ex.getCause());
		}
	}	
	
}
