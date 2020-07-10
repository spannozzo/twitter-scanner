package org.acme.dto;

public class Token {
	String secret="";
	String token="";
	
	public String getSecret() {
		return secret;
	}
	public void setSecret(String secret) {
		this.secret = secret;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public Token(String secret, String token) {
		super();
		this.secret = secret;
		this.token = token;
	}
	
	
	public Token() {
		// TODO Auto-generated constructor stub
	}
	public boolean isEmpty() {
		return secret.isEmpty() || token.isEmpty();
	}
	
}
