package org.acme.dto;

import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

public class Message implements Comparable<Message>{
	String id;
	
	@SerializedName(value = "created_at")
	Date createdAt;
	String text;
	
	@SerializedName(value = "user")
	Author author;
	
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
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public Author getAuthor() {
		return author;
	}
	public void setAuthor(Author author) {
		this.author = author;
	}
	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create(); 
		return gson.toJson(this);
	}
	@Override
	public int compareTo(Message m) {
		if(getCreatedAt().before(m.getCreatedAt())) {
			return -1;
		}
		return 1;
	}
	public boolean cannotBeMapped() {
		if (id==null || author==null) {
			return true;
		}
		return false;
	}
}
