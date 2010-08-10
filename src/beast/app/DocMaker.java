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


import beast.core.Citation;
import beast.core.Description;
import beast.core.Input;
import beast.core.Loggable;
import beast.core.Plugin;
import beast.util.ClassDiscovery;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Plug in documentation generator.
 * Goes through all plug-ins and generate one page per plug-in.
 * <p/>
 * Usage: DocMaker <target directory>
 * where <target directory> is the place where the HTML files
 * should go. Default directory is /tmp.
 * *
 */
public class DocMaker {

	private static final long serialVersionUID = 1L;
	
	/**
     * output directory *
     */
    String m_sDir = "/tmp";
    /**
     * names of the plug-ins to document *
     */
    List<String> m_sPluginNames;
    /**
     * map of plug-in name to its derived plug-ins *
     */
    HashMap<String, String[]> m_isa;
    /**
     * map of plug-in name to its ancestor plug-ins *
     */
    HashMap<String, List<String>> m_ancestors;
    /**
     * map of plug-in name to its description (from @Description annotations) *
     */
    HashMap<String, String> m_descriptions;

    Set<String> m_sLoggables;
    
    public DocMaker(String[] args) {
    	this();
        if (args.length > 0) {
            m_sDir = args[0];
        }
    } // c'tor

    public DocMaker() {
        // find plug ins to document
        m_sPluginNames = ClassDiscovery.find(beast.core.Plugin.class, ClassDiscovery.IMPLEMENTATION_DIR);
        /** determine hierarchical relation between plug-ins **/
        m_isa = new HashMap<String, String[]>();
        m_ancestors = new HashMap<String, List<String>>();
        m_descriptions = new HashMap<String, String>();
        m_sLoggables = new HashSet<String>();
        for (String sPlugin : m_sPluginNames) {
            m_ancestors.put(sPlugin, new ArrayList<String>());
        }
        for (String sPlugin : m_sPluginNames) {
        	try {
	            Plugin plugin = (Plugin) Class.forName(sPlugin).newInstance();
	            String sDescription = getInheritableDescription(plugin.getClass());
	            System.err.println(sPlugin + " => " + sDescription);
	            m_descriptions.put(sPlugin, sDescription);
	            String[] sImplementations = getImplementations(plugin);
	            m_isa.put(sPlugin, sImplementations);
	            for (String sImp : sImplementations) {
	                m_ancestors.get(sImp).add(sPlugin);
	            }
	            if (plugin instanceof Loggable) {
	            	m_sLoggables.add(sPlugin);
	            }
        	} catch (Exception e) {
        		System.err.println(sPlugin + " not documented :" + e.getMessage());
        	}
        }
    } // c'tor

    
    /**
     * create CSS style sheet for all pages *
     */
    void createCSS() throws Exception {
        PrintStream out = new PrintStream(m_sDir + "/doc.css");
        out.println(getCSS());
    }

    String getCSS() {
    	return "table {\n" +
        "	width: 550px;\n" +
        "	border-collapse:collapse;\n" +
        "	border:1px solid #2E2E2E;\n" +
        "}\n" +
        "caption {\n" +
        "	font:  20pt Arial, Helvetica, sans-serif;\n" +
        "	text-align: left;\n" +
        "	text-indent: 10px;\n" +
        "	height: 45px;\n" +
        "	color: #243D02;\n" +
        "	border-top: 1px solid #243D02;\n" +
        "}\n" +
        "thead {\n" +
        "background: #AAAAAA;\n" +
        "	color: #FFFFFF;\n" +
        "	font-size: 0.8em;\n" +
        "	font-weight: bold;\n" +
        "	margin: 20px 0px 0px;\n" +
        "	text-align: left;\n" +
        "	border-right: 1px solid #8D8D8D;\n" +
        "}\n" +
        "tbody tr {\n" +
        "}\n" +

        "tbody th,td {\n" +
        "	font-size: 0.8em;\n" +
        "	line-height: 1.4em;\n" +
        "	font-family: Arial, Helvetica, sans-serif;\n" +
        "	color: #2E2E2E;\n" +
        "	border-top: 1px solid #243D02;\n" +
        "	border-right: 1px solid #8D8D8D;\n" +
        "	text-align: left;\n" +
        "}\n" +
        "a {\n" +
        "	color: #2E2E2E;\n" +
        "	font-weight: bold;\n" +
        "	text-decoration: underline;\n" +
        "}\n" +
        "a:hover {\n" +
        "	color: #FFFF50;\n" +
        "	text-decoration: underline;\n" +
        "}\n" +
        "tfoot th {\n" +
        "	background: #243D02;\n" +
        "	border-top: 1px solid #243D02;\n" +
        "	color: #FFFFFF;\n" +
        "	height: 30px;\n" +
        "}\n" +
        "tfoot td {\n" +
        "	background: #243D02;\n" +
        "	color: #FFFFFF;\n" +
        "	height: 30px;\n" +
        "}";
    }

    /**
     * create plug in index pages, shown in left frame *
     */
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
            InputStream in = new FileInputStream(new File("doc/book/beast.jpg"));
            OutputStream out = new FileOutputStream(new File(m_sDir + "/beast.jpg"));
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
        PrintStream out = new PrintStream(m_sDir + "/contents.html");
        out.println("<html>\n<head><title>BEAST 2.0 Documentation index</title>\n" +
                "<link rel='StyleSheet' href='doc.css' type='text/css'>\n" +
                "</head>\n");
        out.println("<body>\n");
        out.println("<h1>BEAST 2.0 Documentation index</h1>\n");
        String sPrev = null;
        String sPrevPackage = null;
        for (String sPlugin : m_sPluginNames) {
            String sNext = sPlugin.substring(0, sPlugin.indexOf('.'));
            if (sPrev != null && !sNext.equals(sPrev)) {
                out.println("<hr/>");
            }
            sPrev = sNext;
            String sName = sPlugin.substring(sPlugin.lastIndexOf('.') + 1);
            String sPackage = sPlugin.substring(0, sPlugin.lastIndexOf('.'));
            // count nr of packages
            int i = 0;
            while (sPlugin.indexOf('.', i) > 0) {
                sName = "." + sName;
                i = sPlugin.indexOf('.', i) + 1;
            }
            System.err.println(sName + " <= " + sPlugin);
            if (sPrevPackage == null || !sPackage.equals(sPrevPackage)) {
                out.println("<span style='color:grey'>" + sPackage + "</span><br/>");
            }
            out.println("<a href='" + sPlugin + ".html' target='display'>" + sName + "</a><br/>");
            sPrevPackage = sPackage;
        }
        out.println("</body>\n");
        out.println("</html>\n");
    } // createIndex

    /**
     * Find all plugins that are derived from given plugin *
     */
    String[] getImplementations(Plugin plugin) {
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

    /**
     * Extract description from @Description annotation
     * but only if the description is inheritable *
     */
    String getInheritableDescription(Class<?> pluginClass) {
        String sStr = "";
        Class<?> superClass = pluginClass.getSuperclass();
        if (superClass != null) {
        	String sSuper = getInheritableDescription(superClass);
        	if (sSuper != null) {
        		sStr += sSuper + "<br/>"; 
        	}
        }
    	Annotation [] classAnnotations = pluginClass.getAnnotations();
        for (Annotation annotation : classAnnotations) {
            if (annotation instanceof Description) {
                Description description = (Description) annotation;
                if (description.isInheritable()) {
                	sStr += description.value();
                } else {
                	return  null;
                }
            }
        }
        return sStr;
    }

    /**
     * Create page for individual plug-in *
     */
    void createPluginPage(String sPlugin) throws Exception {
        PrintStream out = new PrintStream(m_sDir + "/" + sPlugin + ".html");
        try {
        	out.print(getHTML(sPlugin, true));
        } catch (Exception e) {
			System.err.println("Page creation failed for " +sPlugin + ": " + e.getMessage());
		}
    } // createPluginPage

    
    public String getHTML(String sPlugin, boolean bUseExternalStyleSheet) throws Exception {
    	StringBuffer buf = new StringBuffer();
        buf.append("<html>\n<head>\n<title>BEAST 2.0 Documentation: " + sPlugin + "</title>\n");
        if (bUseExternalStyleSheet) {
        	buf.append("<link rel='StyleSheet' href='doc.css' type='text/css'>\n");
        } else {
        	buf.append("<style type='text/css'>\n");
        	buf.append(getCSS());
        	buf.append("</style>\n");
        }
        buf.append("</head>\n");
        buf.append("<body>\n");
        buf.append("<h1>BEAST 2.0 Documentation: " + sPlugin + "</h1>\n");
        Plugin plugin = (Plugin) Class.forName(sPlugin).newInstance();

        // show all implementation of this plug-in
        String[] sImplementations = m_isa.get(sPlugin);
        if (sImplementations.length > 0) {
            buf.append("<table border='1px'>\n");
            buf.append("<thead><tr><td>implemented by the following</td></tr></thead>\n");
            for (String sImp : sImplementations) {
                buf.append("<tr><td><a href='" + sImp + ".html'>" + sImp + "</a></td></tr>\n");
            }
            buf.append("</table>\n");
        }

        // show descriptions of all plug-ins implemented by this plug in...
        buf.append("<p>" + m_descriptions.get(sPlugin) + "</p>\n");


        // show citation (if any)
        Citation citation = plugin.getCitation();
        if (citation != null) {
            buf.append("<h2>Reference:</h2><p>" + citation.value() + "</p>\n");
            if (citation.DOI().length() > 0) {
                buf.append("<p><a href=\"http://dx.doi.org/" + citation.DOI() + "\">doi:" + citation.DOI() + "</a></p>\n");
            }
        }

        
        // show if this is Loggable
        if (m_sLoggables.contains(sPlugin)) {
        	buf.append("<p>Logable:");
        	buf.append(" yes, this can be used in a log.");
        	buf.append("</p>\n");
//        } else {
//        	buf.append(" no, this cannot be used in a log.");
        }
        
        // list its inputs
        buf.append("<h2>Inputs:</h2>\n");
        List<Input<?>> inputs = plugin.listInputs();
        if (inputs.size() == 0) {
            buf.append("&lt;none&gt;");
        }
        for (Input<?> input : inputs) {
            buf.append("<table border='1px'>\n");
            buf.append("<caption>" + input.getName() + "</caption>\n");
            buf.append("<thead><tr bgcolor='#AAAAAA'><td>type: " + getType(plugin, input.getName()) + "</td></tr></thead>\n");
            buf.append("<tr><td>" + input.getTipText() + "</td></tr>\n");
            buf.append("<tr><td>\n");
            switch (input.getRule()) {
                case OPTIONAL:
                    buf.append("Optional input");
                    if (input.defaultValue != null) {
                        if (input.defaultValue instanceof Integer ||
                                input.defaultValue instanceof Double ||
                                input.defaultValue instanceof Boolean ||
                                input.defaultValue instanceof String) {
                            buf.append(". Default: " + input.defaultValue.toString());
                        }
                    }
                    break;
                case REQUIRED:
                    buf.append("Required input");
                    break;
                case XOR:
                    buf.append("Either this, or " + input.getOther().getName() + " needs to be specified");
                    break;
            }
            buf.append("</td></tr>\n");
            buf.append("</table>\n");
        }
        buf.append("</body>\n");
        buf.append("</html>\n");
        return buf.toString();
    } // getHTML
    
    /**
     * determine type of input of a plug in with name sName
     */
    String getType(Plugin plugin, String sName) {
        try {
            Field[] fields = plugin.getClass().getFields();
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].getType().isAssignableFrom(Input.class)) {
                    Input<?> input = (Input<?>) fields[i].get(plugin);
                    if (input.getName().equals(sName)) {
                        Type t = fields[i].getGenericType();
                        Type[] genericTypes = ((ParameterizedType) t).getActualTypeArguments();
                        if (input.getType() != null) {
                            return (input.getType().isAssignableFrom(Plugin.class) ?"<a href='" + input.getType().getName() + ".html'>":"") +
                                input.getType().getName() + 
                            	(input.get() != null && input.get() instanceof List<?> ? "***" : "") + 
                            	(input.getType().isAssignableFrom(Plugin.class) ?"</a>" :"");
                        }
                        if (input.get() != null && input.get() instanceof List<?>) {
                            Type[] genericTypes2 = ((ParameterizedType) genericTypes[0]).getActualTypeArguments();
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
                            Class<?> genericType = (Class<?>) genericTypes[0];
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

    /**
     * generate set of documents for plug ins
     * including index page + frame
     * individual pages for each plug in
     * *
     */
    public void generateDocs() throws Exception {
        // first, produce CSS & index page
        createCSS();
        createIndex();
        // next, produce pages for individual plug-ins
        for (String sPlugin : m_sPluginNames) {
            createPluginPage(sPlugin);
        }
    } // generateDocs


	/**
     * Usage: DocMaker <target directory>
     * where <target directory> is the place where the HTML files
     * should go. Default directory is /tmp
     */
    public static void main(String[] args) {
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
