package com.wsdl.isdbuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.extensions.schema.Schema;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.apibuilder.storage.StorageService;
import com.apibuilder.util.ExcelOptions;
import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSSchema;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.XSType;
import com.sun.xml.xsom.impl.util.SchemaWriter;
import com.sun.xml.xsom.impl.util.DraconianErrorHandler;
import com.sun.xml.xsom.impl.util.ResourceEntityResolver;
import com.sun.xml.xsom.parser.AnnotationContext;
import com.sun.xml.xsom.parser.AnnotationParser;
import com.sun.xml.xsom.parser.AnnotationParserFactory;
import com.sun.xml.xsom.parser.XSOMParser;
import com.sun.xml.xsom.util.DomAnnotationParserFactory;
import com.sun.xml.xsom.visitor.XSVisitor;

/**
 * 
 * @author Nagarajulu Aerakoni
 *
 */
public class ISDBuilder {

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
	 * 
	 * @param wsdlUri
	 * @throws WSDLException
	 * @throws IOException 
	 */
	public void parseWSDL(String wsdlUri,  MultipartFile multipartFile, final StorageService storageService) throws WSDLException, IOException {

		System.out.println("\n\n=========== PARSING WSDL ====================");

		//InputStream inputStream= new FileInputStream(file);
		
		WSDLReader wsdlReader11 = WSDLFactory.newInstance().newWSDLReader();
		Definition def = wsdlReader11.readWSDL(null, new InputSource(multipartFile.getInputStream()));
		wsdlReader11.setFeature("javax.wsdl.verbose", true);
		wsdlReader11.setFeature("javax.wsdl.importDocuments", true);

		System.out.println("\n\n======= PARSING INCLUDES SCHEMA =============");

		XSOMParser xsomParser = new XSOMParser();
		xsomParser.setErrorHandler(new DraconianErrorHandler());
		//xsomParser.setAnnotationParser(new DomAnnotationParserFactory());
		xsomParser.setAnnotationParser(new AnnotationParserFactory()
	    {
	      public AnnotationParser create()
	      {
	        return new AnnotationParser()
	        {
	          final StringBuffer content = new StringBuffer();
	          
	          public ContentHandler getContentHandler(AnnotationContext context, String parentElementName, ErrorHandler errorHandler, EntityResolver entityResolver)
	          {
	            return new ContentHandler()
	            {
	              public void characters(char[] ch, int start, int length)
	                throws SAXException
	              {
	                content.append(ch, start, length);
	              }
	              
	              public void endDocument()
	                throws SAXException
	              {}
	              
	              public void endElement(String uri, String localName, String name)
	                throws SAXException
	              {}
	              
	              public void endPrefixMapping(String prefix)
	                throws SAXException
	              {}
	              
	              public void ignorableWhitespace(char[] ch, int start, int length)
	                throws SAXException
	              {}
	              
	              public void processingInstruction(String target, String data)
	                throws SAXException
	              {}
	              
	              public void setDocumentLocator(Locator locator) {}
	              
	              public void skippedEntity(String name)
	                throws SAXException
	              {}
	              
	              public void startDocument()
	                throws SAXException
	              {}
	              
	              public void startElement(String uri, String localName, String name, Attributes atts)
	                throws SAXException
	              {}
	              
	              public void startPrefixMapping(String prefix, String uri)
	                throws SAXException
	              {}
	            };
	          }
	          
	          public Object getResult(Object existing)
	          {
	            return this.content.toString().trim();
	          }
	        };
	      }
	    });
	    xsomParser.setEntityResolver(new XSOMEntityResolver());
		//xsomParser.setEntityResolver(new XSOMEntityResolver());
		parseSchemas(xsomParser, def);

		System.out
				.println("\n\n=========== GENERATING EXCEL FILES ===========");
		XSSchemaSet schemaSet = null;
		try {
			schemaSet = xsomParser.getResult();
		} catch (SAXException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		String[] excelTitle = { "Element Name", "DataType", "Cardinality", 
			      "Description", "XPath", "Additional Attrs" };
//		String[] excelTitle = new String[] { "Element Name", "DataType",
//				"Description", "XPath", "Multiplicity", "Additional Attrs" };
		Map<String, Service> allServices = def.getAllServices();
		for (Service service : allServices.values()) {
			String serviceInfo = service.getQName().getLocalPart();
			System.out.println("SERVICE: " + service.getQName());
			Map<String, Port> ports = service.getPorts();
			for (Port port : ports.values()) {
				Binding binding = port.getBinding();
				PortType portType = binding.getPortType();
				List<Operation> operations = portType.getOperations();

				for (Operation operation : operations) {
					ArrayList<String[]> reqData = new ArrayList<String[]>();
					ArrayList<String[]> rspData = new ArrayList<String[]>();
//					reqData.add(new String[] { "", "", "","" });
//					rspData.add(new String[] { "", "", "","" });
				
					String operationInfo = serviceInfo + "."
							+ operation.getName();
					System.out.println("OPERATION_INFO(service+op.getName: " + operationInfo);
					Input input = operation.getInput();

					if ((input != null) && (input.getMessage() != null)
							&& (input.getMessage().getQName() != null)) {
						Message inputMessage = input.getMessage();
						String inputInfo = operationInfo + " "
								+ inputMessage.getQName().getLocalPart();
						Map<String, Part> inputMessageParts = inputMessage
								.getParts();
						for (Part inputMessagePart : inputMessageParts.values()) {
							String reqTypeName = inputMessagePart
									.getElementName().getLocalPart();
							operationInfo += " " + reqTypeName;
							System.out.println("REQUEST: " + reqTypeName);
							reqData.add(new String[] {
						              "Request Object:" + reqTypeName, 
						              "Complex Type", 
						              "", 
						              "", "", "" });

							XSElementDecl reqType = findEntryPoint(schemaSet,
									inputMessagePart.getElementName()
											.getNamespaceURI(), reqTypeName);
							if (reqType != null) {
								XSComplexType tutu = reqType.getType()
										.asComplexType();
								//tutu.visit(new ExcelWriter(reqData, schemaSet, new String[] {service.getQName().getLocalPart(), operation.getName(), reqTypeName}));
								tutu.visit(new ExcelWriter(reqData, schemaSet, new String[] {}, ExcelOptions.TAB));

							} else {
								System.err.println("NOT FOUND: " + reqTypeName);
							}

							reqData.add(new String[] {
									"End Of :" + reqTypeName, "", "","" });
			
							// System.out.println(inputInfo + " " +
							// inputMessagePart.getElementName().getLocalPart());

						}
					} else {
						System.err.println("INPUT MESSAGE is null for "
								+ operation.getName());
					}

					//theData.add(new String[] { "", "", "", ""});

					Output output = operation.getOutput();
					if ((output != null) && (output.getMessage() != null)
							&& (output.getMessage().getQName() != null)) {
						Message outputMessage = output.getMessage();
						String outputInfo = operationInfo + " "
								+ outputMessage.getQName().getLocalPart();
						Map<String, Part> outputMessageParts = outputMessage
								.getParts();
						for (Part outputMessagePart : outputMessageParts
								.values()) {
							String rspTypeName = outputMessagePart
									.getElementName().getLocalPart();
							operationInfo += " " + rspTypeName;

							System.out.println("RESPONSE: " + rspTypeName);
							XSElementDecl rspType = findEntryPoint(schemaSet,
									outputMessagePart.getElementName()
											.getNamespaceURI(), rspTypeName);
							rspData.add(new String[] {
									"Response Object:"
											+ outputMessagePart
													.getElementName()
													.getLocalPart(),
									"Complex Type",
									"The response object for the XXX",
									"The object path", "Multiplicity", "Additional Attrs"  });

							if (rspType != null) {
								XSComplexType tutu = rspType.getType()
										.asComplexType();
								//tutu.visit(new ExcelWriter(rspData, schemaSet, new String[] {service.getQName().getLocalPart(), operation.getName(), rspTypeName}));
								tutu.visit(new ExcelWriter(rspData, schemaSet, new String[] {}, ExcelOptions.TAB));

							} else {
								System.err.println("NOT FOUND: " + rspTypeName);
							}

							rspData.add(new String[] {
									"End Of :"
											+ outputMessagePart
													.getElementName()
													.getLocalPart(), "", "","" });
						}
					} else {
						System.err.println("OUTPUT MESSAGE is null for "
								+ operation.getName());
					}

					try {
						createExcel(
								serviceInfo,
								operation.getName(),
								excelTitle,
								(String[][]) (reqData
										.toArray(new String[reqData.size()][])),
										
								(String[][]) (rspData
												.toArray(new String[rspData.size()][])),
								storageService
										);
					} catch (IOException | ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println(operationInfo);
				}
			}

		}

	}

	/**
	 * 
	 * @param schemaSet
	 * @param schemaName
	 * @param messageType
	 */
	public static XSElementDecl findEntryPoint(XSSchemaSet schemaSet,
			String schemaName, String localMessageName) {
		Iterator<XSSchema> schemaIter = schemaSet.iterateSchema();
		XSSchema schema = null;
		boolean found = false;
		while ((!found) && (schemaIter.hasNext())) {
			schema = schemaIter.next();
			if (schema.getTargetNamespace().equals(schemaName)) {
				found = true;
			}
		}

		if (found) {
			// System.out.println("SCHEMA WAS FOUND: " +
			// schema.getTargetNamespace());
			return schema.getElementDecl(localMessageName);
		} else {
			System.out.println("SCHEMA WAS NOT FOUND: " + schemaName);
			return null;
		}

	}

	/**
	 * 
	 * @param def
	 */
	public void dumpUsefullInfo(Definition def) {
		Map<String, Message> allMessages = def.getMessages();
		for (Message msg : allMessages.values()) {
			System.out.println("MESSAGE: " + msg.getQName());
		}

		Map<String, Binding> allBindings = def.getAllBindings();
		for (Binding binding : allBindings.values()) {
			System.out.println("BINDING: " + binding.getQName());
		}

		Map<String, PortType> allPortTypes = def.getAllPortTypes();
		for (PortType portType : allPortTypes.values()) {
			System.out.println("PORTYPE: " + portType.getQName());
			portType.getOperations();
		}
	}

	/**
	 * 
	 * @param xsomParser
	 * @param def
	 * @return
	 */
	public List<XSSchemaSet> parseSchemas(XSOMParser xsomParser, Definition def) {
		List<XSSchemaSet> theTypes = new ArrayList<XSSchemaSet>();
		Types types = def.getTypes();
		Collection<ExtensibilityElement> elts = types
				.getExtensibilityElements();
		for (ExtensibilityElement elt : elts) {
			if (elt instanceof Schema) {
				Schema schema = (Schema) elt;
				String targetNamespace = schema.getElement().getAttribute(
						"targetNamespace");
				System.out.println("\n--- PARSING " + targetNamespace
						+ " ---- ");

				// System.out.println("ELEMENTTYPE " + targetNamespace + " "
				// + schema.getElementType());
				Map imports = schema.getImports();
				System.out.println("IMPORTS: " + targetNamespace + " "
						+ imports.size());
				List includes = schema.getIncludes();
				System.out.println("INCLUDES: " + targetNamespace + " "
						+ includes.size());
				List redefines = schema.getRedefines();
				System.out.println("REDEFINES: " + targetNamespace + " "
						+ redefines.size());
				// dumpNodeContent(schema.getElement());
				try {
					InputSource source = new InputSource();
					source.setSystemId(schema.getDocumentBaseURI());
					source.setByteStream(toInputStream(schema.getElement()));
					xsomParser.parse(source);
					System.out.println("XSSchemaSet WORKED for "
							+ targetNamespace);
				} catch (SAXException e) {
					System.out.println("XSSchemaSet FAILED for "
							+ targetNamespace);
					e.printStackTrace();
				}

			}

		}
		return theTypes;
	}

	/**
	 * 
	 * @author ejerobr
	 *
	 */
	public class XSOMEntityResolver implements EntityResolver {

		@Override
		public InputSource resolveEntity(String arg0, String arg1)
				throws SAXException, IOException {
			System.out.println("Entity resolver with " + arg0 + " " + arg1);
			return null;
		}

	}

	/**
	 * 
	 * @author ejerobr
	 *
	 */
	public class XSOMErrorHandler implements ErrorHandler {

		@Override
		public void error(SAXParseException arg0) throws SAXException {
			System.err.println(arg0.getMessage());

		}

		@Override
		public void fatalError(SAXParseException arg0) throws SAXException {
			System.err.println(arg0.getMessage());

		}

		@Override
		public void warning(SAXParseException arg0) throws SAXException {
			System.err.println(arg0.getMessage());

		}

	}

	/**
	 * 
	 * @param schemaElement
	 * @return
	 */
	private ByteArrayInputStream toInputStream(Element schemaElement) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			DOMSource source = new DOMSource(schemaElement);
			StreamResult result = new StreamResult(out);
			TransformerFactory transFactory = TransformerFactory.newInstance();
			Transformer transformer = transFactory.newTransformer();
			transformer.transform(source, result);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return new ByteArrayInputStream(out.toByteArray());

	}
	
	private Sheet createSheet(Workbook wb, Map<String, CellStyle> styles, String[] titles, String sheetName)
	{
		Sheet sheet = wb.createSheet(sheetName);

		// turn off gridlines
		sheet.setDisplayGridlines(false);
		sheet.setPrintGridlines(false);
		sheet.setFitToPage(true);
		sheet.setHorizontallyCenter(true);
		PrintSetup printSetup = sheet.getPrintSetup();
		printSetup.setLandscape(true);

		// the following three statements are required only for HSSF
		sheet.setAutobreaks(true);
		printSetup.setFitHeight((short) 1);
		printSetup.setFitWidth((short) 1);

		// the header row: centered text in 48pt font
		Row headerRow = sheet.createRow(0);
		headerRow.setHeightInPoints(12.75f);
		for (int i = 0; i < titles.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(titles[i]);
			cell.setCellStyle(styles.get("header"));
		}
		

		// freeze the first row
		sheet.createFreezePane(0, 1);

		
		// set column widths, the width is measured in units of 1/256th of a
		// character width
		sheet.setColumnWidth(0, 256 * 50);
		sheet.setColumnWidth(1, 256 * 50);
		sheet.setColumnWidth(2, 256 * 50);
		sheet.setColumnWidth(3, 256 * 50);
		sheet.setColumnWidth(4, 256 * 50);
		sheet.setColumnWidth(5, 256 * 50);
		// sheet.setZoom(3, 4);
		
		return sheet;
	}

	/**
	 * 
	 * @param serviceName
	 * @param operationName
	 * @param titles
	 * @param data
	 * @throws IOException
	 * @throws ParseException
	 */
	public void createExcel(String serviceName, String operationName,
			String[] titles, String[][] reqData, String[][] rspData, final StorageService storageService) throws IOException, ParseException {
		Workbook wb = new XSSFWorkbook();

		Map<String, CellStyle> styles = createStyles(wb, false);
	    Map<String, CellStyle> headerElementStyles = createStyles(wb, true);

		//Sheet sheet = createSheet(wb, styles, titles, "Business Plan");
		Sheet reqSheet = createSheet(wb, styles, titles, "Request");
		Sheet rspSheet = createSheet(wb, styles, titles, "Response");
		

		fillSheet(wb, reqSheet, reqData, styles, headerElementStyles);
		fillSheet(wb, rspSheet, rspData, styles, headerElementStyles);

		/*try {
			// Let's try to create the output directory first
			Path p = FileSystems.getDefault().getPath(
					bldOptipons.getISDsDirectory());
			Files.createDirectory(p);
		} catch (Exception oops) {
			// the directory may already exist.
		}*/

		// Write the output to a file
		String fName = serviceName + "."+ operationName + ".xls"+(wb instanceof XSSFWorkbook? "x": "");
		File file = storageService.getRootLocation().resolve(fName).toFile();
		//if (wb instanceof XSSFWorkbook)
			//file += "x";
		FileOutputStream out = new FileOutputStream(file);
		wb.write(out);
		out.close();
	}
	

	/**
	 * put data in a sheet
	 * 
	 * @param sheet
	 * @param data
	 * @param styles
	 */
	private void fillSheet(Workbook wb, Sheet sheet, String[][] data, Map<String, CellStyle> styles, Map<String, CellStyle> headerStyles)
	  {
	    Map<String, CellStyle> headerReqStyles = (Map)((HashMap)styles).clone();
	    
	    Font headerFont1 = wb.createFont();
	    headerFont1.setBoldweight((short)700);
	    headerFont1.setColor(IndexedColors.CORNFLOWER_BLUE
	      .getIndex());
	    

	    int rownum = 1;
	    for (int i = 0; i < data.length; rownum++)
	    {
	      Row row = sheet.createRow(rownum);
	      if (data[i] != null) {
	        for (int j = 0; j < data[i].length; j++)
	        {
	          Cell cell = row.createCell(j);
	          String styleName;
	          switch (j)
	          {
	          case 0: 
	            styleName = "cell_normal";
	            cell.setCellValue(data[i][j]);
	            break;
	          case 1: 
	            styleName = "cell_indented";
	            cell.setCellValue(data[i][j]);
	            break;
	          case 2: 
	            styleName = "cell_normal";
	            cell.setCellValue(data[i][j]);
	            break;
	          case 3: 
	          case 4: 
	          case 5: 
	            styleName = "cell_normal";
	            cell.setCellValue(data[i][j]);
	            break;
	          default: 
	            styleName = data[i][j] != null ? "cell_blue" : 
	              "cell_normal";
	          }
	          //if ((rownum >= 4) && (rownum < 40)) {
	            //cell.setCellStyle((CellStyle)headerStyles.get(styleName));
	          //} else {
	            cell.setCellStyle((CellStyle)styles.get(styleName));
	          //}
	        }
	      }
	      i++;
	    }
	  }

	/**
	 * create a library of cell styles
	 */
	private static Map<String, CellStyle> createStyles(Workbook wb, boolean isHeader) {
	    Map<String, CellStyle> styles = new HashMap();
	    DataFormat df = wb.createDataFormat();
	    

	    Font headerFont = wb.createFont();
	    headerFont.setBoldweight((short)700);
	    CellStyle style = createBorderedStyle(wb);
	    style.setAlignment((short)2);
	    style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE
	      .getIndex());
	    style.setFillPattern((short)1);
	    style.setFont(headerFont);
	    styles.put("header", style);
	    
	    Font headerFont1 = wb.createFont();
	    headerFont1.setBoldweight((short)400);
	    headerFont1.setColor(IndexedColors.BLUE_GREY
	      .getIndex());
	    
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)1);
	    style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE
	      .getIndex());
	    style.setFillPattern((short)1);
	    style.setFont(headerFont1);
	    style.setWrapText(true);
	    styles.put("request_header", style);
	    
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)2);
	    style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE
	      .getIndex());
	    style.setFillPattern((short)1);
	    style.setFont(headerFont1);
	    style.setDataFormat(df.getFormat("d-mmm"));
	    styles.put("header_date", style);
	    
	    Font font1 = wb.createFont();
	    font1.setBoldweight((short)400);
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)1);
	    style.setFont(font1);
	    styles.put("cell_b", style);
	    
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)2);
	    style.setFont(font1);
	    styles.put("cell_b_centered", style);
	    
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)3);
	    style.setFont(font1);
	    style.setDataFormat(df.getFormat("d-mmm"));
	    styles.put("cell_b_date", style);
	    
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)3);
	    style.setFont(font1);
	    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
	    style.setFillPattern((short)1);
	    style.setDataFormat(df.getFormat("d-mmm"));
	    styles.put("cell_g", style);
	    
	    Font font2 = wb.createFont();
	    font2.setColor(IndexedColors.BLUE.getIndex());
	    font2.setBoldweight((short)400);
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)1);
	    style.setFont(font2);
	    styles.put("cell_bb", style);
	    
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)3);
	    style.setFont(font1);
	    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
	    style.setFillPattern((short)1);
	    style.setDataFormat(df.getFormat("d-mmm"));
	    styles.put("cell_bg", style);
	    
	    Font font3 = wb.createFont();
	    font3.setFontHeightInPoints((short)14);
	    font3.setColor(IndexedColors.DARK_BLUE.getIndex());
	    font3.setBoldweight((short)400);
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)1);
	    style.setFont(font3);
	    style.setWrapText(true);
	    styles.put("cell_h", style);
	    
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)1);
	    style.setWrapText(true);
	    styles.put("cell_normal", style);
	    
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)2);
	    style.setWrapText(true);
	    styles.put("cell_normal_centered", style);
	    
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)3);
	    style.setWrapText(true);
	    style.setDataFormat(df.getFormat("d-mmm"));
	    styles.put("cell_normal_date", style);
	    
	    style = createBorderedStyle(wb);
	    style.setAlignment((short)1);
	    style.setIndention((short)1);
	    style.setWrapText(true);
	    styles.put("cell_indented", style);
	    
	    style = createBorderedStyle(wb);
	    style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
	    style.setFillPattern((short)1);
	    styles.put("cell_blue", style);
	    if (isHeader)
	    {
	      Iterator<CellStyle> cs = styles.values().iterator();
	      while (cs.hasNext())
	      {
	        CellStyle st = (CellStyle)cs.next();
	        st.setFont(headerFont1);
	      }
	    }
	    return styles;
	}

	private static CellStyle createBorderedStyle(Workbook wb) {
		CellStyle style = wb.createCellStyle();
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderBottom(CellStyle.BORDER_THIN);
		style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderTop(CellStyle.BORDER_THIN);
		style.setTopBorderColor(IndexedColors.BLACK.getIndex());
		return style;
	}

}
