package com.swagger.apibuilder;

import java.util.List;

import com.apibuilder.storage.FileObj;

public class ParseResult {
	UIMessage uiMsg;
	public UIMessage getUiMsg() {
		return uiMsg;
	}
	public void setUiMsg(UIMessage uiMsg) {
		this.uiMsg = uiMsg;
	}
	List<FileObj> fileURIs;
	public List<FileObj> getFileURIs() {
		return fileURIs;
	}
	public void setFileURIs(List<FileObj> fileURIs) {
		this.fileURIs = fileURIs;
	}
	
}
