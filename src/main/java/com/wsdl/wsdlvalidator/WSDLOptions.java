package com.wsdl.wsdlvalidator;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class WSDLOptions {

	@Option(required = false, name = "-s", aliases = "--schemas", usage = "<Extract Schema Only>")
	private boolean schemasOnly;

	@Option(required = false, name = "-w", aliases = "--wsdls", usage = "<Input WSDLs Directory> (processes all wsdls in directory)")
	private String wsdlsDirectory;
	
	@Option(required = true, name = "-ss", aliases = "--wsdls", usage = "<Input WSDLs Directory> (processes all wsdls in directory)")
	private String samplesDirectory;
	
	public String getSamplesDirectory() {
		return samplesDirectory;
	}

	public void setSamplesDirectory(String samplesDirectory) {
		this.samplesDirectory = samplesDirectory;
	}

	@Option(required = true, name = "-f", aliases = "--file", usage = "<Input WSDL File>")
	private String wsdlsFile;

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

	@Argument
	private List<String> arguments = new ArrayList<String>();

	public List<String> getArguments() {
		return arguments;
	}

	public void setArguments(List<String> arguments) {
		this.arguments = arguments;
	}

}
