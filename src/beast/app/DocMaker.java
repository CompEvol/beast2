
/*
 * File DocMaker.java
 *
 * Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
 *
 * This file is part of BEAST2.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */
package beast.app;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import beast.core.Description;
import beast.core.Input;
import beast.core.Plugin;

import beast.app.draw.ClassDiscovery;

/** Plug in documentation generator.
 * Goes through all plug-ins and generate one page per plug-in.
 *
 * Usage: DocMaker <target directory>
 * where <target directory> is the place where the HTML files
 * should go. Default directory is /tmp.
 *  **/
public class DocMaker {
	/** output directory **/
	String m_sDir = "/tmp";
	/** names of the plug-ins to document **/
	List<String> m_sPluginNames;
	/** map of plug-in name to its derived plug-ins **/
	HashMap<String,String[]> m_isa;
	/** map of plug-in name to its ancestor plug-ins **/
	HashMap<String,List<String>> m_ancestors;
	/** map of plug-in name to its description (from @Description annotations) **/
	HashMap<String,String> m_descriptions;


	public DocMaker(String [] args) {
		if (args.length > 0) {
			m_sDir = args[0];
		}
	} // c'tor

	/** create CSS style sheet for all pages **/
	void createCSS() throws Exception {
		PrintStream out = new PrintStream(m_sDir + "/doc.css");
		out.println("table {\n"+
				"	width: 650px;\n"+
				"	border-collapse:collapse;\n"+
				"	border:1px solid #2E2E2E;\n"+
				"}\n"+
				"caption {\n"+
				"	font: 1.8em/1.8em Arial, Helvetica, sans-serif;\n"+
				"	text-align: left;\n"+
				"	text-indent: 10px;\n"+
				"	background: url(images/caption.jpg) right top;\n"+
				"	height: 45px;\n"+
				"	color: #243D02;\n"+
				"	border-top: 1px solid #243D02;\n"+
				"}\n"+
				"thead {\n"+
				"background: #AAAAAA;\n"+
				"	color: #FFFFFF;\n"+
				"	font-size: 0.8em;\n"+
				"	font-weight: bold;\n"+
				"	margin: 20px 0px 0px;\n"+
				"	text-align: left;\n"+
				"	border-right: 1px solid #8D8D8D;\n"+
				"}\n"+
				"tbody tr {\n"+
				"}\n"+

				"tbody th,td {\n"+
				"	font-size: 0.8em;\n"+
				"	line-height: 1.4em;\n"+
				"	font-family: Arial, Helvetica, sans-serif;\n"+
				"	color: #2E2E2E;\n"+
				"	border-top: 1px solid #243D02;\n"+
				"	border-right: 1px solid #8D8D8D;\n"+
				"	text-align: left;\n"+
				"}\n"+
				"a {\n"+
				"	color: #2E2E2E;\n"+
				"	font-weight: bold;\n"+
				"	text-decoration: underline;\n"+
				"}\n"+
				"a:hover {\n"+
				"	color: #FFFF50;\n"+
				"	text-decoration: underline;\n"+
				"}\n"+
				"tfoot th {\n"+
				"	background: #243D02 url(images/foot.jpg) repeat-x bottom;\n"+
				"	border-top: 1px solid #243D02;\n"+
				"	color: #FFFFFF;\n"+
				"	height: 30px;\n"+
				"}\n"+
				"tfoot td {\n"+
				"	background: #243D02 url(images/foot.jpg) repeat-x bottom;\n"+
				"	color: #FFFFFF;\n"+
				"	height: 30px;\n"+
				"}");
	}

	/** create plug in index pages, shown in left frame **/
	void createIndex() throws Exception {
		{
		PrintStream out = new PrintStream(m_sDir + "/index.html");
		out.println("<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Frameset//EN'\n" +
"		   'http://www.w3.org/TR/html4/frameset.dtd'>\n" +
"		<HTML>\n" +
"		<HEAD>\n" +
"		<TITLE>BEAST 2.0 Documentation</TITLE>\n" +
"		</HEAD>\n" +
"		<FRAMESET cols='20%, 80%'>\n" +
"		  <FRAMESET rows='50, 200'>\n" +
"		      <FRAME src='http://www.omnomnomnom.com/random/rotate.php' align='center'>\n" +
"		      <FRAME src='contents.html'>\n" +
"		  </FRAMESET>\n" +
"		  <FRAME name='display' src='contents.html'>\n" +
"		</FRAMESET>\n" +
"		</HTML>");
		}

		{
			}

		{
	      InputStream in = new FileInputStream(new File("doc/beast.jpg"));
	      OutputStream out = new FileOutputStream(new File(m_sDir + "/beast.jpg"));
	      byte[] buf = new byte[1024];
	      int len;
	      while ((len = in.read(buf)) > 0){
	        out.write(buf, 0, len);
	      }
	      in.close();
	      out.close();
		}
 		PrintStream out = new PrintStream(m_sDir + "/contents.html");
		out.println("<html>\n<head><title>BEAST 2.0 Documentation index</title>\n" +
				"<link rel='StyleSheet' href='doc.css' type='text/css'>\n"+
				"</head>\n");
		out.println("<body>\n");
		out.println("<h1>BEAST 2.0 Documentation index</h1>\n");
		String sPrev = null;
		String sPrevPackage = null;
		for (String sPlugin: m_sPluginNames) {
			String sNext = sPlugin.substring(0, sPlugin.indexOf('.'));
			if (sPrev != null && !sNext.equals(sPrev)) {
				out.println("<hr/>");
			}
			sPrev = sNext;
			String sName = sPlugin.substring(sPlugin.lastIndexOf('.') + 1);
			String sPackage = sPlugin.substring(0, sPlugin.lastIndexOf('.'));
			// count nr of packages
			int i = 0;
			while (sPlugin.indexOf('.',i) > 0) {
				sName = "." + sName;
				i = sPlugin.indexOf('.', i) +1;
			}
			System.err.println(sName + " <= " + sPlugin);
			if (sPrevPackage == null || !sPackage.equals(sPrevPackage)) {
				out.println("<span style='color:grey'>" + sPackage + "</span><br/>");
			}
			out.println("<a href='"+sPlugin+".html' target='display'>"+ sName+ "</a><br/>");
			sPrevPackage = sPackage;
		}
		out.println("</body>\n");
		out.println("</html>\n");
	} // createIndex

	/** Find all plugins that are derived from given plugin **/
	String [] getImplementations(Plugin plugin) {
		String sName = plugin.getClass().getName();
		List<String> sImplementations = new ArrayList<String>();
		for (String sPlugin : m_sPluginNames) {
			try {
				if (!sPlugin.equals(sName) && plugin.getClass().isAssignableFrom(Class.forName(sPlugin))) {
					sImplementations.add(sPlugin);
				}
			} catch (ClassNotFoundException e) {
			}
		}
		return sImplementations.toArray(new String[0]);
	}
	/** Extract description from @Description annotation **/
	String getDescription(Plugin plugin) {
		Annotation[] classAnnotations = plugin.getClass().getAnnotations();
		for(Annotation annotation : classAnnotations)
		{
			if (annotation instanceof Description)
			{
				Description description = (Description)annotation;
				return description.value();
			}
		}
		return "Not documented!!!";
	}
	/** Extract description from @Description annotation
	 * but only if the description is inheritable **/
	String getInheritableDescription(Plugin plugin) {
		Annotation[] classAnnotations = plugin.getClass().getAnnotations();
		for(Annotation annotation : classAnnotations)
		{
			if (annotation instanceof Description)
			{
				Description description = (Description)annotation;
				if (description.isInheritable()) {
					return description.value();
				}
				return "";
			}
		}
		return "";
	}

	/** Create page for individual plug-in **/
	void createPluginPage(String sPlugin) throws Exception {
		PrintStream out = new PrintStream(m_sDir + "/" + sPlugin + ".html");
		out.println("<html>\n<head>\n<title>BEAST 2.0 Documentation: " + sPlugin +"</title>\n" +
				"<link rel='StyleSheet' href='doc.css' type='text/css'>\n"+
				"</head>\n");
		out.println("<body>\n");
		out.println("<h1>BEAST 2.0 Documentation: " + sPlugin + "</h1>\n");
		Plugin plugin = (Plugin) Class.forName(sPlugin).newInstance();

		// show all implementation of this plug-in
		String [] sImplementations = m_isa.get(sPlugin);
		if (sImplementations.length > 0) {
			out.println("<table>");
			out.println("<thead><tr><td>implemented by the following</td></tr></thead>");
			for (String sImp : sImplementations) {
				out.println("<tr><td><a href='"+sImp+".html'>"+sImp+"</a></td></tr>");
			}
			out.println("</table>");
		}

		// show descriptions of all plug-ins implemented by this plug in...
		List<String> sAncestors = m_ancestors.get(sPlugin);
		for (String sAncestor: sAncestors) {
			String sDescription =  m_descriptions.get(sAncestor);
			if (sDescription.length() > 0) {
				//out.println("<p>"+ sAncestor + ":"+sDescription + "</p>");
				out.println("<p>" + sDescription + "</p>");
			}
		}
		// ... plus its own description
		out.println("<p>"+getDescription(plugin)+"</p>");


		// show citation (if any)
		String sCite = plugin.getCitation();
		if (sCite.length() > 0) {
			out.println("<h2>Reference:</h2>" + sCite);
		}

		// list its inputs
		out.println("<h2>Inputs:</h2>");
		Input<?> [] inputs = plugin.listInputs();
		if (inputs.length == 0) {
			out.println("&lt;none&gt;");
		}
		for (Input<?> input : inputs) {
			out.println("<table>");
			out.println("<caption>" + input.getName() + "</caption>");
			out.println("<thead><tr><td>type: " + getType(plugin, input.getName()) + "</td></tr></thead>");
			out.println("<tr><td>" + input.getTipText() + "</td></tr>");
			out.print("<tr><td>");
			switch (input.getRule()) {
			case OPTIONAL:
				out.print("Optional input");
				if (input.defaultValue !=null) {
					if (input.defaultValue instanceof Integer ||
						input.defaultValue instanceof Double ||
						input.defaultValue instanceof Boolean ||
						input.defaultValue instanceof String) {
						out.print(". Default: " + input.defaultValue.toString());
					}
				}
				break;
			case REQUIRED:
				out.print("Required input");
				break;
			case XOR:
				out.print("Either this, or " + input.getOther().getName() + " needs to be specified");
				break;
			}
			out.println("</td></tr>");
			out.println("</table>");
		}
		out.println("</body>\n");
		out.println("</html>\n");
	} // createPluginPage

	/** determine type of input of a plug in with name sName**/
	String getType(Plugin plugin, String sName) {
			try {
				Field [] fields = plugin.getClass().getFields();
				for (int i = 0; i < fields.length; i++) {
					if (fields[i].getType().isAssignableFrom(Input.class)) {
						Input<?> input = (Input<?>) fields[i].get(plugin);
						if (input.getName().equals(sName)) {
							Type t = fields[i].getGenericType();
							Type [] genericTypes = ((ParameterizedType)t).getActualTypeArguments();
							if (input.get() != null && input.get() instanceof List<?>) {
								Type [] genericTypes2 = ((ParameterizedType)genericTypes[0]).getActualTypeArguments();
								Class<?> _class = (Class<?>) genericTypes2[0];
								Object o = null;
								try {
									o = Class.forName(_class.getName()).newInstance();
								} catch (Exception e) {
								}
								if (o != null && o instanceof Plugin) {
									return "<a href='" + _class.getName() + ".html'>" + _class.getName() + "***</a>";
								} else {
									return _class.getName() + "***";
								}
							} else {
								Class<?> genericType = (Class<?>)genericTypes[0];
								Class<?> _class = genericType;
								Object o = null;
								try {
									o = Class.forName(_class.getName()).newInstance();
								} catch (Exception e) {
								}
								if (o != null && o instanceof Plugin) {
									return "<a href='" + _class.getName() + ".html'>" + _class.getName() + "</a>";
								} else {
									return _class.getName();
								}
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "?";
	} // getType

	/** generate set of documents for plug ins
	 * including index page + frame
	 * individual pages for each plug in
	 * **/
	public void generateDocs() throws Exception {
		// find plug ins to document
		m_sPluginNames = ClassDiscovery.find(beast.core.Plugin.class, beast.app.draw.Document.IMPLEMENTATION_DIR);

		/** determine hierarchical relation between plug-ins **/
		m_isa = new HashMap<String,String[]>();
		m_ancestors = new HashMap<String,List<String>>();
		m_descriptions = new HashMap<String,String>();
		for (String sPlugin: m_sPluginNames) {
			m_ancestors.put(sPlugin, new ArrayList<String>());
		}
		for (String sPlugin: m_sPluginNames) {
			Plugin plugin = (Plugin) Class.forName(sPlugin).newInstance();
			m_descriptions.put(sPlugin, getInheritableDescription(plugin));
			String [] sImplementations = getImplementations(plugin);
			m_isa.put(sPlugin, sImplementations);
			for (String sImp : sImplementations) {
				m_ancestors.get(sImp).add(sPlugin);
				//System.err.println(sImp + " <= " + sPlugin);
			}
		}

		// first, produce CSS & index page
		createCSS();
		createIndex();
		// next, produce pages for individual plug-ins
		for (String sPlugin: m_sPluginNames) {
			createPluginPage(sPlugin);
		}
	} // generateDocs

	 /**
	 * Usage: DocMaker <target directory>
	 * where <target directory> is the place where the HTML files
	 * should go. Default directory is /tmp
	 **/
	public static void main(String [] args) {
		try {
			System.err.println("Producing documentation...");
			DocMaker b = new DocMaker(args);
			b.generateDocs();
			System.err.println("Done!!!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	} // main

} // BeastDocMaker
