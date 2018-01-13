package com.swagger.apibuilder;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class ISDOptions {

	@Option(required = false, name = "-s", aliases = "--schemas", usage = "<Extract Schema Only>")
	private boolean schemasOnly;

	@Option(required = false, name = "-j", aliases = "--json", usage = "<Input JSONs Directory> (processes all wsdls in directory)")
	private String jsonDirectory;
	
	@Option(required = false, name = "-f", aliases = "--file", usage = "<Input JSON File>")
	private String jsonFile;

	public String getJsonDirectory() {
		return jsonDirectory;
	}

	public void setJsonDirectory(String jsonDirectory) {
		this.jsonDirectory = jsonDirectory;
	}

	public String getJsonFile() {
		return jsonFile;
	}

	public void setJsonFile(String jsonFile) {
		this.jsonFile = jsonFile;
	}


	@Option(required = false, name = "-i", aliases = "--isds", usage = "<Output ISDs Directory> (default=./isds)")
	private String isdsDirectory = "isds";

	@Option(required = false, name = "-t", aliases = "--tab", usage = "<tabIndent> (default=5)")
	private int tab = 5;

	
	public boolean isFetchInsertOnly() {
		return schemasOnly;
	}

	public void setFetchInsertOnly(boolean fetchInsertOnly) {
		this.schemasOnly = fetchInsertOnly;
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
