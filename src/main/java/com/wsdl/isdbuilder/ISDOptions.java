package com.wsdl.isdbuilder;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class ISDOptions {

	@Option(required = false, name = "-s", aliases = "--schemas", usage = "<Extract Schema Only>")
	private boolean schemasOnly;

	@Option(required = false, name = "-w", aliases = "--wsdls", usage = "<Input WSDLs Directory> (processes all wsdls in directory)")
	private String wsdlsDirectory;
	
	@Option(required = false, name = "-f", aliases = "--file", usage = "<Input WSDL File>")
	private String wsdlsFile;

	@Option(required = false, name = "-i", aliases = "--isds", usage = "<Output ISDs Directory> (default=./isds)")
	private String isdsDirectory = "isds";

	@Option(required = false, name = "-t", aliases = "--tab", usage = "<tabIndent> (default=5)")
	private int tab = 5;

	public String getWsdlsFile() {
		return wsdlsFile;
	}

	public void setWsdlsFile(String wsdlsFile) {
		this.wsdlsFile = wsdlsFile;
	}

	public boolean isFetchInsertOnly() {
		return schemasOnly;
	}

	public void setFetchInsertOnly(boolean fetchInsertOnly) {
		this.schemasOnly = fetchInsertOnly;
	}

	public String getWSDLsDirectory() {
		return wsdlsDirectory;
	}

	public void setWSDLsDirectory(String wsdlsDirectory) {
		this.wsdlsDirectory = wsdlsDirectory;
	}

	public String getISDsDirectory() {
		return isdsDirectory;
	}

	public void setISDsDirectory(String isdsDirectory) {
		this.isdsDirectory = isdsDirectory;
	}

	public int getTab() {
		return tab;
	}

	public void setTab(int tab) {
		this.tab = tab;
	}


	@Argument
	private List<String> arguments = new ArrayList<String>();

	public List<String> getArguments() {
		return arguments;
	}

	public void setArguments(List<String> arguments) {
		this.arguments = arguments;
	}

}
