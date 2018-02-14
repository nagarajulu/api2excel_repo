package com.apibuilder.util;

public class UIMessage {
	
	public enum MESSAGETYPE{
		INFO, ERROR
	}

	MESSAGETYPE messageType;
	public MESSAGETYPE getMessageType() {
		return messageType;
	}
	public void setMessageType(MESSAGETYPE messageType) {
		this.messageType = messageType;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	String message;
	
}
