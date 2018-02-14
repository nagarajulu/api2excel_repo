package com.apibuilder.wsdl;

import com.sun.xml.xsom.ForeignAttributes;
import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSAttGroupDecl;
import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSFacet;
import com.sun.xml.xsom.XSIdentityConstraint;
import com.sun.xml.xsom.XSListSimpleType;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSModelGroupDecl;
import com.sun.xml.xsom.XSNotation;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSRestrictionSimpleType;
import com.sun.xml.xsom.XSSchema;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.XSType;
import com.sun.xml.xsom.XSUnionSimpleType;
import com.sun.xml.xsom.XSWildcard;
import com.sun.xml.xsom.XSXPath;
import com.sun.xml.xsom.XSWildcard.Any;
import com.sun.xml.xsom.XSWildcard.Other;
import com.sun.xml.xsom.XSWildcard.Union;
import com.sun.xml.xsom.impl.Const;
import com.sun.xml.xsom.impl.ForeignAttributesImpl;
import com.sun.xml.xsom.visitor.XSSimpleTypeVisitor;
import com.sun.xml.xsom.visitor.XSTermVisitor;
import com.sun.xml.xsom.visitor.XSVisitor;
import com.sun.xml.xsom.visitor.XSWildcardFunction;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ExcelWriter implements XSVisitor, XSSimpleTypeVisitor {
	public ExcelWriter(List<String[]> _out, XSSchemaSet schemaSet, String[] startPath, int tabstop) {
		this.out = _out;
		this.schemaSet = schemaSet;
		this.path = new ArrayList<>(Arrays.asList(startPath));
		this.tabstop = tabstop;
	}

	private static long cplxCalls = 0;
	
	/** output is sent to this object. */
	private final List<String[]> out;
	private XSSchemaSet schemaSet;
	private Set<String> alreadyProcessed = new HashSet<>();
	private List<String> path;
	private int tabstop;

	/** indentation. */
	private int indent;

	private String getPath()
	{
		StringBuffer buff = new StringBuffer(1024);
		for (String value : path)
		{
			buff.append("/").append(value);
		}
		
		return buff.toString();
	}
	private void println(String s) {
		println(s, "", "", "", "", "");
	}
	
	private void println(String name, String typeName, String description, String xpath, String multiplicity, String additionalAttrs) {
		StringBuffer formattedName = new StringBuffer(256);
		int indentOffset = indent * tabstop;
		for (int i = 0; i < indentOffset; i++)
			formattedName.append(' ');
		
		formattedName.append(name);
		//out.add(new String[] { formattedName.toString(), typeName, description, xpath, extraAttrs, additionalAttrs });
		out.add(new String[] { formattedName.toString(), typeName, multiplicity, description + "".trim(), xpath, additionalAttrs });
		//System.out.println("out>> name=" + s + ", typeName=" + typeName + ", descr=" + description);
		//System.out.println(getPath());
	}

	private void println() {
		println("");
	}

	/** If IOException is encountered, this flag is set to true. */
	private boolean hadError = false;

	public void attGroupDecl(XSAttGroupDecl decl) {
		Iterator itr;

		println(MessageFormat.format("<attGroup name=\"{0}\">", decl.getName()));
//		indent++;

		// TODO: wildcard

//		itr = decl.iterateAttGroups();
//		while (itr.hasNext())
//			dumpAttributeGroupRef((XSAttGroupDecl) itr.next());

		itr = decl.iterateDeclaredAttributeUses();  //orig missing NONE /cart/billTo/customerAccount/@actionCode, @customerAccountId, @parentURI, @referenceURI
		//itr = decl.iterateAttributeUses();  //eboyrob (appeared but 3 times doubleInherit) /cart/billTo/customerAccount/@actionCode, @customerAccountId, @parentURI, @referenceURI
		while (itr.hasNext())
			attributeUse((XSAttributeUse) itr.next());

		//eboyrob - moved so @attributes appear before children elements
		itr = decl.iterateAttGroups();
		while (itr.hasNext())
			dumpAttributeGroupRef((XSAttGroupDecl) itr.next());
		
//		indent--;
//		println("</attGroup>");
	}

	public void dumpAttributeGroupRef(XSAttGroupDecl decl) {
//		println(MessageFormat.format("<attGroup ref=\"'{'{0}'}'{1}\"/>",
//				decl.getTargetNamespace(), decl.getName()));
		System.out.println("THIS IS A REF + " + decl.getName());
		println("","",decl.getName(), "", "", "");
	}

	public void attributeUse(XSAttributeUse use) {
		XSAttributeDecl decl = use.getDecl();

		String additionalAtts = "";

		if (use.isRequired())
			additionalAtts += " use=\"required\"";
		if (use.getFixedValue() != null
				&& use.getDecl().getFixedValue() == null)
			additionalAtts += " fixed=\"" + use.getFixedValue() + '\"';
		if (use.getDefaultValue() != null
				&& use.getDecl().getDefaultValue() == null)
			additionalAtts += " default=\"" + use.getDefaultValue() + '\"';

//ORIG
//		if (decl.isLocal()) {
//			// this is anonymous attribute use
//			dumpAttributeDecl(decl, additionalAtts);
//		} else {
//			// reference to a global one
//			println(MessageFormat.format(
//					"<attribute ref=\"'{'{0}'}'{1}{2}\"/>",
//					decl.getTargetNamespace(), decl.getName(), additionalAtts, ""));
//		}
//ORIG
		
		//BEGIN BOB
//		indent++;
		
		String baseType = "";
//		if (!decl.getType().isPrimitive())
//		{  //unfortunately precision & scale (integer) fall into here to return decimal
//			baseType = "Restricted (" + decl.getType().asRestriction().getBaseType().asSimpleType().getName() + ")";
//		}
		
		String xpath = getPath() + "/@" + decl.getName();
		//additionalAtts="test";  //debug for column only
		//println('@' + decl.getName(), decl.getType().getName(), baseType, xpath, "", additionalAtts);
		println('@' + decl.getName(), displayType(decl.getType()), baseType, xpath, "", additionalAtts);

//		indent--;		
		//END BOB
	}

	public void attributeDecl(XSAttributeDecl decl) {
		dumpAttributeDecl(decl, "");
	}

	private void dumpAttributeDecl(XSAttributeDecl decl, String additionalAtts) {
		XSSimpleType type = decl.getType();

//		println(MessageFormat.format(
//				"<attribute name=\"{0}\"{1}{2}{3}{4}{5}>",
//				decl.getName(),
//				additionalAtts,
//				type.isLocal() ? "" : MessageFormat.format(
//						" type=\"'{'{0}'}'{1}\"", type.getTargetNamespace(),
//						type.getName()),
//				decl.getFixedValue() == null ? "" : " fixed=\""
//						+ decl.getFixedValue() + '\"',
//				decl.getDefaultValue() == null ? "" : " default=\""
//						+ decl.getDefaultValue() + '\"', type.isLocal() ? ""
//						: " /"));

		if (type.isLocal()) {
			//indent++;
			simpleType(type);
			//indent--;
			// println("</attribute>");
		}
	}

	public void simpleType(XSSimpleType type) {
		indent++;

		type.visit((XSSimpleTypeVisitor) this);

		indent--;
	}

	public void listSimpleType(XSListSimpleType type) {
		XSSimpleType itemType = type.getItemType();

		if (itemType.isLocal()) {
			println("<list>");
//			indent++;
			simpleType(itemType);
//			indent--;
			println("</list>");
		} else {
			// global type
			println(MessageFormat.format("<list itemType=\"'{'{0}'}'{1}\" />",
					itemType.getTargetNamespace(), itemType.getName()));
		}
	}

	public void unionSimpleType(XSUnionSimpleType type) {
		final int len = type.getMemberSize();
		StringBuffer ref = new StringBuffer();

		for (int i = 0; i < len; i++) {
			XSSimpleType member = type.getMember(i);
			if (member.isGlobal())
				ref.append(MessageFormat.format(" '{'{0}'}'{1}",
						member.getTargetNamespace(), member.getName()));
		}

		if (ref.length() == 0)
			println("<union>");
		else
			println("<union memberTypes=\"" + ref + "\">");
//		indent++;

		for (int i = 0; i < len; i++) {
			XSSimpleType member = type.getMember(i);
			if (member.isLocal())
				simpleType(member);
		}
//		indent--;
		println("</union>");
	}

	public void restrictionSimpleType(XSRestrictionSimpleType type) {

		if (type.getBaseType() == null) {
			// don't print anySimpleType
			if (!type.getName().equals("anySimpleType"))
				throw new InternalError();
			if (!Const.schemaNamespace.equals(type.getTargetNamespace()))
				throw new InternalError();
			return;
		}

		XSSimpleType baseType = type.getSimpleBaseType();

//		println(MessageFormat.format(
//				"<restriction{0}>",
//				baseType.isLocal() ? "" : " base=\"{"
//						+ baseType.getTargetNamespace() + '}'
//						+ baseType.getName() + '\"'));
//		indent++;

		if (baseType.isLocal())
			simpleType(baseType);

//does nothing for now!!! (e.g. enum/list elements)
//		Iterator itr = type.iterateDeclaredFacets();
//		while (itr.hasNext())
//			facet((XSFacet) itr.next());

//		indent--;
	}

	public void facet(XSFacet facet) {
//		println(MessageFormat.format("<{0} value=\"{1}\"/>", facet.getName(),
//				facet.getValue()));
	}

	public void notation(XSNotation notation) {
		println(MessageFormat.format(
				"<notation name='\"0}\" public =\"{1}\" system=\"{2}\" />",
				notation.getName(), notation.getPublicId(),
				notation.getSystemId()));
	}

	public void complexType(XSComplexType type) {
//		indent++;

		//System.out.println("inside complexType name=" + type.getName() + "calls = " + (++cplxCalls));
		// TODO: wildcard

		if (type.getContentType().asSimpleType() != null) {
			// simple content
//			indent++;

			XSType baseType = type.getBaseType();

			if (type.getDerivationMethod() == XSType.RESTRICTION) {
				// restriction
//				println(MessageFormat.format("<restriction base=\"<{0}>{1}\">",
//						baseType.getTargetNamespace(), baseType.getName()));
				//indent++;

				dumpComplexTypeAttribute(type);

				//indent--;
//				println("</restriction>");
			} else {
				// extension
//				println(MessageFormat.format("<extension base=\"<{0}>{1}\">",
//						baseType.getTargetNamespace(), baseType.getName()));

				//indent++;  //eboyrob moved before visit to dump atrributes before elements
				dumpComplexTypeAttribute(type);  //eboyrob moved before visit to dump atrributes before elements
				//indent--;
				
				// check if have redefine tag - Kirill
				if (type.isGlobal()
						&& type.getTargetNamespace().equals(
								baseType.getTargetNamespace())
						&& type.getName().equals(baseType.getName())) {
//					indent++;
//					println("<redefine>");
//					indent++;
					baseType.visit(this);
//					indent--;
//					println("</redefine>");
//					indent--;
				}

//				indent++;
//
//				dumpComplexTypeAttribute(type);  //eboyrob moved to before visit to dump atrributes before elements
//
//				indent--;
//				println("</extension>");
			}

//			indent--;
		} else {
			// complex content
//			indent++;

			XSComplexType baseType = type.getBaseType().asComplexType();

			if (type.getDerivationMethod() == XSType.RESTRICTION) {
				// restriction
//				indent++;

				dumpComplexTypeAttribute(type);  //eboyrob moved before visit to dump attributes before elements
				type.getContentType().visit(this);
				//dumpComplexTypeAttribute(type);  //eboyrob moved before visit to dump attributes before elements

//				indent--;
			} else {
				// extension
//				indent++;  //eboyrob adding this
				dumpComplexTypeAttribute(type);  //eboyrob moved from below to dump attrs before base type and elements
//				indent--;
				
				// check if have redefine - Kirill
				if (type.isGlobal()
						&& type.getTargetNamespace().equals(
								baseType.getTargetNamespace())
						&& type.getName().equals(baseType.getName())) {
//					indent++;
//					println("<redefine>");
//					indent++;
					baseType.visit(this);
//					indent--;
//					println("</redefine>");
//					indent--;
				}

//				indent++;

				//type.getExplicitContent().visit(this);
				type.getContentType().visit(this);
				//dumpComplexTypeAttribute(type);  //moved to before baseType to dump attrs before elements

//				indent--;
			}

//			indent--;
			// println("</complexContent>");
		}

//		indent--;
		// println("</complexType>");
	}

	private void dumpComplexTypeAttribute(XSComplexType type) {
		Iterator itr;

//		itr = type.iterateAttGroups();
//		while (itr.hasNext())
//			dumpAttributeGroupRef((XSAttGroupDecl) itr.next());
		
		//problem here is cart/customerAccount/@attributes never get listed if use iterateDeclaredAttributeUses
		//but for customerAccount extends CustomerAccount extends CustomeAccountReference all appear 3 times repeated for iterateAttributeUses (XSOM BUG)
		Set<XSAttributeUse> attributeUseSet = new LinkedHashSet<>();  //maintain order
		//orig itr = type.iterateDeclaredAttributeUses();
		itr = type.iterateAttributeUses();  //eboyrob
		while (itr.hasNext())
		{
			attributeUseSet.add((XSAttributeUse)itr.next());
			//attributeUse((XSAttributeUse) itr.next());
		}
		
		for (XSAttributeUse attributeUseEntry : attributeUseSet)
		{
			attributeUse(attributeUseEntry);
		}
		
		//eboyrob - moved so @attributes appear before children elements
		itr = type.iterateAttGroups();
		while (itr.hasNext())
			dumpAttributeGroupRef((XSAttGroupDecl) itr.next());

		XSWildcard awc = type.getAttributeWildcard();
		if (awc != null)
			wildcard("anyAttribute", awc, "");
	}

	public void elementDecl(XSElementDecl decl) {
		elementDecl(decl, "");
	}
	
	/**
	 * Method to get the value of a private class field value
	 * 
	 * @param obj
	 * @param fieldName
	 * @return
	 */
	private Object getPrivateMemberValue(Object obj, String fieldName)
	{
		Object value = null;
		try
		{
			Field f = obj.getClass().getDeclaredField(fieldName); //NoSuchFieldException
			f.setAccessible(true);
			value = (Object) f.get(obj); //IllegalAccessException
		}
		catch(SecurityException|NoSuchFieldException|IllegalAccessException|NullPointerException e)
		{
			value = "*Unknown*";
		}
		
		return value;
	}
	
	/**
	 * Get the private value of the private "prefix" member of the NGCCRuntimeEx ValidationContext
	 * Uses reflection method getPrivateMember value to to get the private prefix value from XSOM parser's 
	 * private static NGCCRuntimeEx$Context class.
	 * This way we don't have to change XSOM to override XSOM's non-extensible NGCCRuntimeEx class or static ValidationContext class
	 * 
	 * @param attr
	 * @return String
	 */
	private String getPrefix(ForeignAttributes attr)
	{
		String value = (String) getPrivateMemberValue(attr.getContext(), "prefix");
		return value;
	}
	
	/**
	 * Returns the namespace prefix for the type
	 * 
	 * @param type
	 * @return String
	 */
	private String getPrefix(XSType type)
	{
		String prefix = "";
        List<? extends ForeignAttributes> foreignAttrs = type.getForeignAttributes();
        if (foreignAttrs.size() != 0)
        {
        	ForeignAttributesImpl faImpl = (ForeignAttributesImpl)foreignAttrs.get(0);
        	prefix = getPrefix(faImpl);
        }
        
        return prefix;
	}
	
	/**
	 * Returns the namespace prefix for the type
	 * 
	 * @param type
	 * @return String
	 */
	private String getPrefixAll(XSType type)
	{
		String prefix = "";
        List<? extends ForeignAttributes> foreignAttrs = type.getForeignAttributes();
        if (foreignAttrs.size() > 0)
        {
        	for(int i=0; i<foreignAttrs.size(); i++){
        		ForeignAttributesImpl faImpl = (ForeignAttributesImpl)foreignAttrs.get(i);
            	prefix = getPrefix(faImpl);
            	
            	System.out.println("ExcelWriter.getPrefixAll():"+prefix);
        	}
        	
        }
        
        return prefix;
	}
	
	/**
	 * Returns the display name of a type
	 * 
	 * @param type
	 * @return
	 */
	private String displayType(XSType type)
	{
		//return type.isLocal() ? "" : " " + type.getName();
		//return type.getName() != null ? type.getName() : "(" + type.toString() + ")";
		String typeName = type.getName();
		String prefix = getPrefix(type);
		if (typeName == null)
		{
			if (type.isSimpleType())
			{
				XSSimpleType simpleType = type.asSimpleType();
				if (simpleType.isRestriction())
				{
					StringBuffer enumList = new StringBuffer(256);
					enumList.append("restriction enum {");
					boolean first = true;
					XSRestrictionSimpleType restrictionSimpleType = simpleType.asRestriction();
					Iterator<? extends XSFacet> it = restrictionSimpleType.getDeclaredFacets().iterator();
					while(it.hasNext())
					{
						Object obj = it.next().getValue();
						if (!first)
						{
							enumList.append(",");
						}
						first = false;
						enumList.append(obj);
					}
					enumList.append("}");
					typeName = enumList.toString();
				}
				else if (simpleType.isList())
				{
					XSListSimpleType listSimpleType = simpleType.asList();
					//FIXME
				}
					
			} 
			else if (type.isComplexType())
			{
				XSComplexType cplxType = type.asComplexType();
				XSType baseType = cplxType.getBaseType();
				typeName = "(" + type.toString() + " extends " + displayType(baseType) + ")";
			}
			else
			{
				
			}
		}
		
		//fallback if still null
		if (typeName == null)
		{
			typeName = "(" + type.toString() + ")";
		}
		else if (prefix != null && prefix.length() > 0)
		{
			typeName = prefix + ":" + typeName;
		}
		
		return typeName;
	}

	private void elementDecl(XSElementDecl decl, String extraAtts) {
		XSType type = decl.getType();
		//println(decl.getName() , (type.isLocal() ? "" : " " + type.getName()), "");

		 XSAnnotation annotation = decl.getAnnotation();
		    String documentation = "";
		    if (annotation != null) {
		      documentation = (String)annotation.getAnnotation();
		    }
		    
		String qualifiedName = decl.getTargetNamespace() + "/" + decl.getName();
		String key = qualifiedName + ":" + type.getName();
		

		if(type.getName()!=null && type.getName().equals("MSISDN")){
			getPrefixAll(type);
			System.out.println("ExcelWriter.elementDecl():"+key);
		}
		
		if (!alreadyProcessed.contains(key))
		{
//			if (decl.getType().getDerivationMethod() == XSType.EXTENSION)
//			{  //eboyrob don't indent and dont add to xpath if extension  cannot do since (/cart/billTo/customerAccount xpath appears as cart/billTo)
//				indent--;  //don't want to nest
//			}
//			else
//			{
				path.add(decl.getName());
//			}
			
			
			//println(decl.getName() , (type.isLocal() ? "" : " " + type.getName()), "", getPath(), extraAtts, "");
			////println(decl.getName() , type.getName(), "", getPath(), extraAtts, "");
			//println(decl.getName() , displayType(type), "", getPath(), extraAtts, "");
			println(decl.getName(), displayType(type), documentation, getPath(), extraAtts, "");
			alreadyProcessed.add(key);

//NOTE:  NBI CartWSIL  cartItem/productOffering/@productOfferingId missing (below does not fix it)
//			if (type.isComplexType())
//			{  //eboyrob adding this
//				////indent++;  //eboyrob adding this
//				//dumpComplexTypeAttribute(type.asComplexType());  //eboyrob moved from below to dump attrs before base type and elements
//				Iterator itr = type.asComplexType().iterateDeclaredAttributeUses();
//				while (itr.hasNext())
//					attributeUse((XSAttributeUse) itr.next());
//				////indent--;
//				//			itr = decl.iterateDeclaredAttributeUses();
//				//			while (itr.hasNext())
//				//				attributeUse((XSAttributeUse) itr.next());
//			}
			
			// if (type.isLocal()) {
			indent++;
			// if (type.isLocal())
			type.visit(this);
			//path.remove(path.size() - 1);  //eboyrob moved below in extension code
			indent--;
			// }

			alreadyProcessed.remove(key);
			
//			if (decl.getType().getDerivationMethod() == XSType.EXTENSION)
//			{  //eboyrob don't indent and dont add to xpath if extension cannot do since (/cart/billTo/customerAccount xpath appears as cart/billTo)
//				indent++;  //don't want to nest
//			}
//			else
//			{
				path.remove(path.size() - 1);
//			}
		}
		else
		{
			//System.out.println("*** Recursive name=" + qualifiedName + ", type=" + type.getName());
			//println(decl.getName() , (type.isLocal() ? "" : " " + type.getName()), "*** Recursive ***", "", extraAtts, "");
			////println(decl.getName() , type.getName(), "*** Recursive ***", "", extraAtts, "");
			println(decl.getName(), displayType(type), documentation, "", extraAtts, "");
		}
	}

	public void modelGroupDecl(XSModelGroupDecl decl) {
		println(decl.getName());
		
//		if (!externalProcessed.contains(decl.getName()))
//		{
//			externalProcessed.add(decl.getName());
//			indent++;

			modelGroup(decl.getModelGroup());

//			indent--;

//			externalProcessed.remove(decl.getName());
//		}
	}

	public void modelGroup(XSModelGroup group) {
		modelGroup(group, "");
	}

	private void modelGroup(XSModelGroup group, String extraAtts) {
		//indent++;
		boolean isChoice = (group.isModelGroup()) && (group.asModelGroup().getCompositor().toString().equals("choice"));
	    if (isChoice)
	    {
	      println("xs:choice");
	      this.indent += 1;
	    }
	    int len = group.getSize();
	    for (int i = 0; i < len; i++) {
	      particle(group.getChild(i));
	    }
	    if (isChoice) {
	      this.indent -= 1;
	    }
		//indent--;
	}

	public void particle(XSParticle part) {
		StringBuffer buf = new StringBuffer();
	    
	    int i = part.getMinOccurs();
	    

	    buf.append(i);
	    

	    i = part.getMaxOccurs();
	    if (i == -1) {
	      buf.append("/*");
	    } else {
	      buf.append("/" + i);
	    }
	    final String extraAtts = buf.toString();

		part.getTerm().visit(new XSTermVisitor() {
			public void elementDecl(XSElementDecl decl) {
				if (decl.isLocal())
					ExcelWriter.this.elementDecl(decl, extraAtts);
				else {
					//orig println(decl.getName());
					
					//BOB Begin - Resolve external element
					XSElementDecl nestedDecl = WSDLBuilder.findEntryPoint(schemaSet, decl.getTargetNamespace(), decl.getName());
					
					String qualifiedName = decl.getTargetNamespace() + "/" + decl.getName();
					//println(decl.getName(), qualifiedName, "");
					
					if (nestedDecl != null)
					{
//						System.out.println("Nested, name=" + nestedDecl.getType().getName());
//						if (nestedDecl.getType().isComplexType())
//						{  //just for debugging
//							System.out.println("complex, name=" + nestedDecl.getType().getName());
//						}
											
							//println(decl.getName(), qualifiedName, "");

							ExcelWriter.this.elementDecl(nestedDecl, extraAtts);  //FIXME recurseive forever!!!
					}
					else
					{
						println(decl.getName(), decl.getType().getName(), "Unknown", "", extraAtts, "");
					}										

////BOB END
				
					
				}
			}

			public void modelGroupDecl(XSModelGroupDecl decl) {
				println(decl.getName());
			}

			public void modelGroup(XSModelGroup group) {
				ExcelWriter.this.modelGroup(group, extraAtts);
			}

			public void wildcard(XSWildcard wc) {
				ExcelWriter.this.wildcard("any", wc, extraAtts);
			}
		});
	}

	public void wildcard(XSWildcard wc) {
		wildcard("any", wc, "");
	}

	private void wildcard(String tagName, XSWildcard wc, String extraAtts) {
		final String proessContents;
		switch (wc.getMode()) {
		case XSWildcard.LAX:
			proessContents = " processContents='lax'";
			break;
		case XSWildcard.STRTICT:
			proessContents = "";
			break;
		case XSWildcard.SKIP:
			proessContents = " processContents='skip'";
			break;
		default:
			throw new AssertionError();
		}

		println(MessageFormat.format("<{0}{1}{2}{3}/>", tagName,
				proessContents, wc.apply(WILDCARD_NS), extraAtts));
	}

	private static final XSWildcardFunction<String> WILDCARD_NS = new XSWildcardFunction<String>() {
		public String any(Any wc) {
			return ""; // default
		}

		public String other(Other wc) {
			return " namespace='##other'";
		}

		public String union(Union wc) {
			StringBuffer buf = new StringBuffer(" namespace='");
			boolean first = true;
			for (String s : wc.getNamespaces()) {
				if (first)
					first = false;
				else
					buf.append(' ');
				buf.append(s);
			}
			return buf.append('\'').toString();
		}
	};

	public void annotation(XSAnnotation ann) {
		// TODO: it would be nice even if we just put <xs:documentation>
	}

	public void identityConstraint(XSIdentityConstraint decl) {
		// TODO
	}

	public void xpath(XSXPath xp) {
		// TODO
	}

	public void empty(XSContentType t) {
	}

	@Override
	public void schema(XSSchema schema) {
		// TODO Auto-generated method stub
		
	}
}
