package com.project.merauke;

public class CommentData {

	String id;
	String name;
	String comment;
	
	public CommentData(String userId, String username, String userComment) {
		id = userId;
		name = username;
		comment = userComment;
	}
	
	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getComment() {
		return comment;
	}
}
