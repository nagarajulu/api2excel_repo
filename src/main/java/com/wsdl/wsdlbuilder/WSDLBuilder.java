package com.wsdl.wsdlbuilder;

import javax.wsdl.Definition;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.PortType;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.xml.namespace.QName;

public class WSDLBuilder {

	public static void main(String[] args) {
		WSDLBuilder builder = new WSDLBuilder();
		try {
			builder.buildWSDL();
		} catch (WSDLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void buildWSDL() throws WSDLException {
		WSDLFactory factory = WSDLFactory.newInstance();
		Definition def = factory.newDefinition();
		String tns = "urn:xmltoday-delayed-quotes";
		String xsd = "http://www.w3.org/2001/XMLSchema";
		Part part1 = def.createPart();
		Part part2 = def.createPart();
		Message msg1 = def.createMessage();
		Message msg2 = def.createMessage();
		Input input = def.createInput();
		Output output = def.createOutput();
		Operation operation = def.createOperation();
		PortType portType = def.createPortType();
		def.setQName(new QName(tns, "StockQuoteService"));
		def.setTargetNamespace(tns);
		def.addNamespace("tns", tns);
		def.addNamespace("xsd", xsd);
		part1.setName("symbol");
		part1.setTypeName(new QName(xsd, "string"));
		msg1.setQName(new QName(tns, "getQuoteInput"));
		msg1.addPart(part1);
		msg1.setUndefined(false);
		def.addMessage(msg1);
		part2.setName("quote");
		part2.setTypeName(new QName(xsd, "float"));
		msg2.setQName(new QName(tns, "getQuoteOutput"));
		msg2.addPart(part2);
		msg2.setUndefined(false);
		def.addMessage(msg2);
		input.setMessage(msg1);
		output.setMessage(msg2);
		operation.setName("getQuote");
		operation.setInput(input);
		operation.setOutput(output);
		operation.setUndefined(false);
		portType.setQName(new QName(tns, "GetQuote"));
		portType.addOperation(operation);
		portType.setUndefined(false);
		def.addPortType(portType);
	}
}
