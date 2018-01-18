package com.wsdl.wsdlvalidator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.wsdl.Definition;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.impl.util.DraconianErrorHandler;
import com.sun.xml.xsom.parser.XSOMParser;
import com.sun.xml.xsom.util.DomAnnotationParserFactory;

public class WSDLValidator {
	private WSDLOptions bldOptipons;

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		WSDLValidator builder = new WSDLValidator();
		try {
			if (!builder.parseCommandLineArg(args)) {
				return;
			}
			if (builder.bldOptipons.getWSDLsDirectory() != null) {
				List<String> fileNames = builder.scanDirectory(
						builder.bldOptipons.getWSDLsDirectory(), "*.wsdl");
				if ((fileNames != null) && (fileNames.size() != 0)) {
					for (String fileName : fileNames) {
						builder.parseWDSDL(fileName);
					}
				} else {
					System.out.println("No WSDL found to convert");
				}
			} 
			else if (builder.bldOptipons.getWsdlsFile() != null)
			{
				builder.parseWDSDL(builder.bldOptipons.getWsdlsFile());
			}
			else {
				builder.parseWDSDL(args[0]);
			}

		} catch (WSDLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * parseCommandLineArg
	 *
	 * @param args
	 * @return
	 */
	private boolean parseCommandLineArg(String[] args) {

		bldOptipons = new WSDLOptions();
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
	 */
	void parseWDSDL(String wsdlUri) throws WSDLException {

		System.out.println("\n\n=========== PARSING WSDL ====================");

		WSDLReader wsdlReader11 = WSDLFactory.newInstance().newWSDLReader();
		Definition def = wsdlReader11.readWSDL(wsdlUri);
		wsdlReader11.setFeature("javax.wsdl.verbose", true);
		wsdlReader11.setFeature("javax.wsdl.importDocuments", true);

		System.out.println("\n\n======= PARSING INCLUDES SCHEMA =============");

		XSOMParser xsomParser = new XSOMParser();
		xsomParser.setErrorHandler(new DraconianErrorHandler());
		xsomParser.setAnnotationParser(new DomAnnotationParserFactory());
		xsomParser.setEntityResolver(new XSOMEntityResolver());
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
}
