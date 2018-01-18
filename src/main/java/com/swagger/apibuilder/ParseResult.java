package com.swagger.apibuilder;

import java.util.List;

public class ParseResult {
	UIMessage uiMsg;
	public UIMessage getUiMsg() {
		return uiMsg;
	}
	public void setUiMsg(UIMessage uiMsg) {
		this.uiMsg = uiMsg;
	}
	public List<String> getURI() {
		return URI;
	}
	public void setURI(List<String> uRI) {
		URI = uRI;
	}
	List<String> URI;
	
}
