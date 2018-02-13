package com.swagger.apibuilder;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;


import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.*;
import org.codehaus.jackson.type.TypeReference;
import org.codehaus.jackson.util.DefaultPrettyPrinter;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.springframework.web.multipart.MultipartFile;

import com.apibuilder.storage.FileObj;
import com.apibuilder.storage.StorageService;
import com.swagger.apibuilder.UIMessage.MESSAGETYPE;


public class APIBuilder {
	
	private ISDOptions bldOptipons;

	/*public static void main(String[] args) {
		APIBuilder builder = new APIBuilder();
		try {
			if (!builder.parseCommandLineArg(args)) {
				return;
			}
			if (builder.bldOptipons.getJsonDirectory() != null) {
				List<String> fileNames = builder.scanDirectory(
						builder.bldOptipons.getJsonDirectory(), "*.json");
				if ((fileNames != null) && (fileNames.size() != 0)) {
					for (String fileName : fileNames) {
						builder.parseJSONFile(fileName, null, null);;
					}
				} else {
					System.out.println("No JSON found to convert");
				}
			} 
			else if (builder.bldOptipons.getJsonFile() != null)
			{
				//builder.parseWDSDL(builder.bldOptipons.getJsonFile());
			}
			else {
				//builder.parseWDSDL(args[0]);
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}*/
	
	/**
	 * Scan the directory for WSDL files.
	 * 
	 * @param directory
	 * @param filter
	 * @return
	 */
	public List<String> scanDirectory(String directory, String filter) {

		List<String> fileNames = new ArrayList<>();
		Path p = FileSystems.getDefault().getPath(directory);
		System.out.println("Scanning Directory:" + p.toString());
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
				Paths.get(directory), filter)) {
			for (Path path : directoryStream) {
				fileNames.add(path.toString());
			}
		} catch (IOException ex) {
		}
		return fileNames;
	}
	/**
	 * parseCommandLineArg
	 *
	 * @param args
	 * @return
	 */
	private boolean parseCommandLineArg(String[] args) {

		bldOptipons = new ISDOptions();
		CmdLineParser parser = new CmdLineParser(bldOptipons);

		parser.setUsageWidth(80);

		try {
			if (args.length == 0)
			{
				throw new CmdLineException("args required");
			}
			
			parser.parseArgument(args);
			return true;

		} catch (CmdLineException e) {

			System.err.println(e.getMessage());
			System.err
					.println("java -jar isdbuilder.jar [options...] arguments...");
			// print the list of available options
			parser.printUsage(System.err);
			System.err.println();
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public ParseResult parseJSONFile(String fileName, MultipartFile multipartFile, final StorageService storageService){
		final ParseResult pr=new ParseResult();
		final ObjectMapper om = new ObjectMapper() ;
		List<FileObj> apiUriList = new ArrayList<FileObj>();
		
		try {
			//final File jsonFileName = new File (fileName);
			final JsonNode inputDoc = om.readTree(multipartFile.getInputStream());
			//pretty(inputDoc);
			
			final String[] reqExcelTitle = { "Parameter Type", "Parameter Name", "DataType", "Cardinality", 
				      "Description", "XPath" };
			final String[] respExcelTitle = {"HTTP Status Code", "Parameter Type", "Parameter Name", "DataType", "Cardinality", 
				      "Description", "XPath" };
			final String[] apiInfoTitle ={"Title","Swagger Version","Description", "Full URI"};
			
			//Read swagger level information, common to all APIs
			final JsonNode swaggerInfo=inputDoc.path("info");
			
			final String swaggerTitle = swaggerInfo.isMissingNode()? "": swaggerInfo.path("title").getValueAsText();
			final String swaggerVersion = swaggerInfo.isMissingNode()? "":swaggerInfo.path("version").getValueAsText();
			final String swaggerDescription = swaggerInfo.isMissingNode()? "": swaggerInfo.path("description").getValueAsText();
			
			final String swaggerBaseURI = inputDoc.path("basePath").getValueAsText();
			
			//Read service operations
			final JsonNode paths = inputDoc.path("paths");
			Iterator<String> pathList = paths.getFieldNames();
			while(pathList.hasNext()){ //For each REST service in JSON file.
				final String pathName = pathList.next();
				System.out.println("Operation: "+pathName);
				
				final ArrayList<String[]> apiInfo = new ArrayList<String[]>();
				apiInfo.add(new String[]{swaggerTitle, swaggerVersion, swaggerDescription, swaggerBaseURI+pathName});
				//apiUriList.add(swaggerBaseURI+pathName);
				
				final JsonNode pathNode = paths.findValue(pathName);
				
				for(JsonNode operationNode: pathNode.findParents("operationId")){
					
					//Build reqData, respData [][] for each operation
					ArrayList<String[]> reqData = new ArrayList<String[]>();
					ArrayList<String[]> rspData = new ArrayList<String[]>();
					
					final String operationId = operationNode.findValue("operationId").getValueAsText();
					
					//BEGIN REQUEST --- PROCESSING
					final JsonNode reqParamsNode = operationNode.findPath("parameters");
					System.out.println("reqParamsNode.isArray():"+ reqParamsNode.isArray());
					
					//Loop for all REQ params
					for(int p=0; p<reqParamsNode.size(); p++){
						final JsonNode curParamNode = reqParamsNode.get(p);
						
						final boolean refExists = !curParamNode.path("$ref").isMissingNode();
						
						//if its body node, skip it here.
						if(!curParamNode.findPath("schema").isMissingNode())
							continue;
							
						//if param is reference.
						if(refExists){
							final String paramPath = curParamNode.path("$ref").getTextValue();
							final String paramName = paramPath.substring(paramPath.lastIndexOf('/')+1);
							final JsonNode paramNode = inputDoc.path("parameters").path(paramName);
							if(paramNode.isMissingNode()){
								System.out.println(paramPath+" not found");
								continue;
							}
							addJSONFieldToExcel(reqData, paramNode, paramName, "", "", 0, false, "");

						}
						else{ //regular param node.
							final String fieldName = curParamNode.path("name").getValueAsText();
							addJSONFieldToExcel(reqData, curParamNode, fieldName, "", fieldName, 0, false, "");
						}
											
					}
					
										
						//find and print all body/Request parameters.
						final JsonNode reqSchema = reqParamsNode.findPath("schema");
						if(!reqSchema.isMissingNode()){
							final JsonNode findValue = reqSchema.findValue("$ref");
							System.out.println("Operation Request: "+findValue);
							final String refPathValue = findValue.getValueAsText();
							//Read the definitions from swagger i.e. individual definitions of Schema elements/types.
							final JsonNode reqDefNode = getNodeInDefinitions(inputDoc, refPathValue);
							
							//print the reqNode to excel.
							final int lastIndexOf = refPathValue.lastIndexOf('/');
							processRefJSONField(inputDoc, reqDefNode, refPathValue.substring(lastIndexOf), reqData, 
									"body", refPathValue.substring(lastIndexOf+1), 0, false, "");
						}
						
						//BEGIN RESPONSE --- PROCESSING
						final JsonNode respParamsNode = operationNode.findPath("responses");
						final Iterator<String> httpStatusCodesFields = respParamsNode.getFieldNames();
						
						while(httpStatusCodesFields.hasNext()){ //loop on httpStatusCodes
							final String respStatusCode = httpStatusCodesFields.next();
							
							final JsonNode respHttpStatusNode = respParamsNode.path(respStatusCode);
							
							final String httpStatusCode = "Status-Code="+respStatusCode;
							addJSONFieldToExcel(rspData, respHttpStatusNode, httpStatusCode, "Status-Line", httpStatusCode, 0, true, respStatusCode);
							
							final JsonNode respHeaderNode = respHttpStatusNode.findPath("headers");
							if(!respHeaderNode.isMissingNode()){
								
								//for each header field.
								final Iterator<String> hdrFields = respHeaderNode.getFieldNames();
								
								while(hdrFields.hasNext()){
									final String hdrFieldName = hdrFields.next();
									
									final JsonNode hdrFieldNode = respHeaderNode.path(hdrFieldName);
									
									addJSONFieldToExcel(rspData, hdrFieldNode, hdrFieldName, "header", hdrFieldName, 0, true, respStatusCode);
								}
							}
							
							//find and print all response parameters.
							final JsonNode respSchema = respHttpStatusNode.findPath("schema");
							if(!respSchema.isMissingNode()){
								final JsonNode findValue = respSchema.findValue("$ref");
								System.out.println("Operation Request: "+findValue);
								final String refPathValue = findValue.getValueAsText();
								//Read the definitions from swagger i.e. individual definitions of Schema elements/types.
								final JsonNode respDefNode = getNodeInDefinitions(inputDoc, refPathValue);
								
								//print the reqNode to excel.
								final int lastIndexOf = refPathValue.lastIndexOf('/');
								processRefJSONField(inputDoc, respDefNode, refPathValue.substring(lastIndexOf), rspData, 
										"body", refPathValue.substring(lastIndexOf+1), 0, true, respStatusCode);
							}
						}//outer while loop end

					 //END RESPONSE --- PROCESSING
						
					
					//Create Excel for each operation
					try {
						final String operationName = pathName.substring(pathName.indexOf("/")+1);
						final String serviceName = multipartFile.getOriginalFilename().replaceAll(".json", "");
						System.out.println("generating file..."+serviceName);
						String uri=ExcelUtil.createExcel(
								serviceName,
								operationId,
								reqExcelTitle,
								respExcelTitle,
								apiInfoTitle,
								(String[][]) (reqData
										.toArray(new String[reqData.size()][])),
										
								(String[][]) (rspData
												.toArray(new String[rspData.size()][])), 
								(String[][]) (apiInfo
										.toArray(new String[apiInfo.size()][])), 
								bldOptipons, 
								storageService
										);
						
						FileObj fileNameUri=new FileObj();
						fileNameUri.setFilename(uri.substring(uri.lastIndexOf("/")+1));
						fileNameUri.setUri(uri);
						apiUriList.add(fileNameUri);
					} catch (IOException | ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				} //end of operation loop
					
					
			} // end of each PATH node loop
				
			
			JsonPrettyPrint jp = new JsonPrettyPrint(System.out);
			//jp.print(parent);
			
			UIMessage uiMsg=new UIMessage();
			uiMsg.setMessageType(MESSAGETYPE.INFO);
			uiMsg.setMessage("Your swagger " + multipartFile.getOriginalFilename() + " is successfully parsed ! Please download your API documents below.");
			
			pr.setUiMsg(uiMsg);
			pr.setFileURIs(apiUriList);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			UIMessage uiMsg=new UIMessage();
			uiMsg.setMessageType(MESSAGETYPE.ERROR);
			uiMsg.setMessage("Error parsing the swagger file - "+multipartFile.getOriginalFilename());
			
			pr.setUiMsg(uiMsg);
		}
		return pr;
	}
	
	/**
	 * Reads all attributes of a node and adds new row to reqRespData
	 * @param reqRespData
	 * @param fieldNode
	 */
	public void addJSONFieldToExcel(ArrayList<String[]> reqRespData, JsonNode fieldNode, String path, String paramType, String fName, int tabSpaceCtr, 
			boolean isResp, String httpStatusCode){
		//Sequence of printing { "Parameter Type", "Element Name", "DataType", "Cardinality", 
	    //  "Description", "XPath" }
		//
		//fieldType will have one of values: "path", "query", "body", "header", "form"; form - for multipart data.
		final String fieldType = paramType!=null && paramType.isEmpty() ? fieldNode.path("in").getValueAsText() : paramType;
		final String fieldName = fName!=null && fName.isEmpty() ? fieldNode.path("name").getValueAsText() : fName;
		final String typeFormatInfo = fieldNode.path("format").isMissingNode()? "": "\n("+fieldNode.path("format").getValueAsText()+")";
		
		//if currentNode is of type array, then update xpath to have []
		final String typeDesc = fieldNode.path("type").isMissingNode() ? "": fieldNode.path("type").getValueAsText();
		final boolean isCurrentNodeArray = typeDesc.equalsIgnoreCase("array") || !fieldNode.path("items").isMissingNode();
				//curPath = typeInfo!=null && typeInfo.equals("array")? curPath+ "[]" : curPath;
		final String typeInfo = isCurrentNodeArray? "array" : typeDesc+typeFormatInfo;
		
		final boolean requiredFlag = fieldNode.path("required").isMissingNode() ? false : fieldNode.path("required").getValueAsBoolean();
		final String requiredVal = isCurrentNodeArray ? (requiredFlag ? "1/*" : "0/*") : (requiredFlag ? "1/1" : "0/1");
		final String description = fieldType.equals("header") ? "HTTP Header Parameter" : 
			fieldNode.path("description").isMissingNode()? "": fieldNode.path("description").getValueAsText();
		final String enumText = !fieldNode.path("enum").isMissingNode() ? "Possible Values:"+fieldNode.path("enum").toString() : "";
		final String xpath = path.replace("/$ref", "");
		
		StringBuilder sb=new StringBuilder();
		for(int i=0; i<tabSpaceCtr; i++){
			sb.append("   ");
		}
		
		final String printFieldName = sb.toString()+fieldName.replace("$ref", "");
		
		if(isResp){
			reqRespData.add(new String[]{httpStatusCode, fieldType, printFieldName, typeInfo, requiredVal, description+" "+enumText, xpath});
		}
		else{
			reqRespData.add(new String[]{fieldType, printFieldName, typeInfo, requiredVal, description+" "+enumText, xpath});
		}
		
		
	}
	
	/**
	 * Given a node, go dive deep JSON node and print all the elements into the reqRespData
	 * @param inputDoc
	 * @param currentNode
	 * @param curPath
	 * @param reqRespData
	 */
	public void processRefJSONField(JsonNode inputDoc, JsonNode currentNode, String curPath, 
			ArrayList<String[]> reqRespData, String fieldLocation, String fName, int tabSpaceCtr, boolean isResp, String httpStatusCode){
		
		//add one entry for the currentNode
		addJSONFieldToExcel(reqRespData, currentNode, curPath, fieldLocation, fName, tabSpaceCtr, isResp, httpStatusCode);
		
		//exit condition
		//check if the currentNode has properties or not. if not, we can exit this method.
		final JsonNode propNode = currentNode.path("properties");
		final JsonNode itemsNode = currentNode.path("items");
		if(propNode.isMissingNode() && itemsNode.isMissingNode()){
			return;
		}
		
		//for each of its properties, recursively call this method.
		//if it's ref, get that node, and recursively call this method.
		final Iterator<String> itemsOrPropsNames = propNode.isMissingNode() ? itemsNode.getFieldNames() : propNode.getFieldNames();
		
		while(itemsOrPropsNames.hasNext()){
			final String fieldName = itemsOrPropsNames.next();
			
			final JsonNode fieldNode = propNode.path(fieldName);
			
			//processRefJSONField(inputDoc, fieldNode, curPath+"/"+fieldName, reqRespData, fieldLocation, fieldName, tabSpaceCtr+1, isResp, httpStatusCode);
			
			//check if it has '$ref', then we need to get the actual node.
			final JsonNode refNode = propNode.isMissingNode() ? itemsNode.findValue("$ref") : fieldNode.findValue("$ref");
			if(refNode!=null && !refNode.isMissingNode()){
				final String refNodeValue = refNode.getValueAsText();
				final int lastIndexOf = refNodeValue.lastIndexOf("/");
				final String refNodeFieldName = refNodeValue.substring(lastIndexOf+1);
				final JsonNode actualRefFieldNode = inputDoc.path("definitions").path(refNodeFieldName);
				processRefJSONField(inputDoc, actualRefFieldNode, curPath+"/"+fieldName, reqRespData, fieldLocation, fieldName, tabSpaceCtr+1, isResp, httpStatusCode);
			}
			else{
				processRefJSONField(inputDoc, fieldNode, curPath+"/"+fieldName, reqRespData, fieldLocation, fieldName, tabSpaceCtr+1, isResp, httpStatusCode);
			}
		}
		
		//if $ref node is present in items node
		
		
	}
	public String getJsonFieldPath(JsonNode node, String fieldName){
		return "";
	}
	public JsonNode getNodeInDefinitions(JsonNode inputDoc, String refNodeValue){
		final int lastIndexOf = refNodeValue.lastIndexOf("/");
		final String refNodeFieldName = refNodeValue.substring(lastIndexOf+1);
		final JsonNode actualRefFieldNode = inputDoc.path("definitions").path(refNodeFieldName);		
		return actualRefFieldNode;
	}
	
	public static class JsonPrettyPrint {
		final ObjectMapper om = new ObjectMapper() ;
		final JsonFactory jf = new JsonFactory();
		final JsonGenerator jgPrettyOut ;

		public JsonPrettyPrint (PrintStream out) {
			try {
				jgPrettyOut = jf.createJsonGenerator(out) ;
				jgPrettyOut.setPrettyPrinter(new DefaultPrettyPrinter());
			}
			catch (Exception x) {
				throw new RuntimeException ("JsonPrettyPrint failed to initialize.", x) ;
			}
		}
		
		public JsonPrettyPrint (Writer out) { //XXX code dup
			try {
				jgPrettyOut = jf.createJsonGenerator(out) ;
				jgPrettyOut.setPrettyPrinter(new DefaultPrettyPrinter());
			}
			catch (Exception x) {
				throw new RuntimeException ("JsonPrettyPrint failed to initialize.", x) ;
			}
		}
		
		public void print (JsonNode jn) {
			try {
				om.writeTree(jgPrettyOut, jn) ;
			}
			catch (Exception x) {
				throw new RuntimeException ("JsonPrettyPrint failed to print.", x) ;
			}
		}
	}

	public static void pretty (JsonNode jn) {
		new JsonPrettyPrint(System.out).print(jn) ;
		System.out.println();
	}
	
	
}
