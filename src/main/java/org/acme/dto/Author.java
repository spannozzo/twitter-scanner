package org.acme.dto;

import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

public class Author implements Comparable<Author>{
	String id;
	
	@SerializedName(value = "created_at")
	Date createdAt;
	String name;
	
	@SerializedName(value = "screen_name")
	String screenName;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Date getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getScreenName() {
		return screenName;
	}
	public void setScreenName(String screenName) {
		this.screenName = screenName;
	}
	
	@Override
	public String toString() {
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create(); 
		return gson.toJson(this);
	}
	@Override
	public int compareTo(Author a) {
		if(this.getCreatedAt().before(a.getCreatedAt())) {
			return -1;
		}
		
		return 1;
	}
	
}
