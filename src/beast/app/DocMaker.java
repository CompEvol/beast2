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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import beast.core.BEASTObject;
import beast.core.Citation;
import beast.core.Description;
import beast.core.Input;
import beast.core.Loggable;
import beast.util.AddOnManager;



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
    
    BEASTVersion version = new BEASTVersion();
    
    public DocMaker(String[] args) {
        this();
        if (args.length > 0) {
            if (args[0].equals("-javadoc")) {
                makeJavaDoc();
                System.exit(0);
            }
            m_sDir = args[0];
        }
    } // c'tor

    public DocMaker() {
        // find plug ins to document
        m_sPluginNames = AddOnManager.find(beast.core.BEASTObject.class, AddOnManager.IMPLEMENTATION_DIR);
        /** determine hierarchical relation between plug-ins **/
        m_isa = new HashMap<>();
        m_ancestors = new HashMap<>();
        m_descriptions = new HashMap<>();
        m_sLoggables = new HashSet<>();
        for (String sPlugin : m_sPluginNames) {
            m_ancestors.put(sPlugin, new ArrayList<>());
        }
        for (String sPlugin : m_sPluginNames) {
            try {
                Class _class = Class.forName(sPlugin);
                BEASTObject plugin = (BEASTObject) _class.newInstance();
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
     * print @Description and Input.description info so that it can
     * be inserted in the code before creating Javadoc documentation
     * for the Beast II SDK.
     */
    void makeJavaDoc() {
        for (String sPlugin : m_sPluginNames) {
            try {
                BEASTObject plugin = (BEASTObject) Class.forName(sPlugin).newInstance();
                System.out.println(sPlugin + ":@description:" + plugin.getDescription());
                for (Input<?> input : plugin.listInputs()) {
                    System.out.println(sPlugin + ":" + input.getName() + ":" + input.getTipText());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * create CSS style sheet for all pages *
     */
    void createCSS() throws Exception {
        PrintStream out = new PrintStream(m_sDir + "/doc.css");
        out.println(getCSS());
    }

    String getCSS() {
    		return  "<!--\n" +
    				 "a.summary-letter {text-decoration: none}\n" +
    				 "blockquote.smallquotation {font-size: smaller}\n" +
    				 "div.display {margin-left: 3.2em}\n" +
    				 "div.example {margin-left: 3.2em}\n" +
    				 "div.indentedblock {margin-left: 3.2em}\n" +
    				 "div.lisp {margin-left: 3.2em}\n" +
    				 "div.smalldisplay {margin-left: 3.2em}\n" +
    				 "div.smallexample {margin-left: 3.2em}\n" +
    				 "div.smallindentedblock {margin-left: 3.2em; font-size: smaller}\n" +
    				 "div.smalllisp {margin-left: 3.2em}\n" +
    				 "kbd {font-style:oblique}\n" +
    				 "pre.display {font-family: inherit}\n" +
    				 "pre.format {font-family: inherit}\n" +
    				 "pre.menu-comment {font-family: serif}\n" +
    				 "pre.menu-preformatted {font-family: serif}\n" +
    				 "pre.smalldisplay {font-family: inherit; font-size: smaller}\n" +
    				 "pre.smallexample {font-size: smaller}\n" +
    				 "pre.smallformat {font-family: inherit; font-size: smaller}\n" +
    				 "pre.smalllisp {font-size: smaller}\n" +
    				 "span.nocodebreak {white-space:nowrap}\n" +
    				 "span.nolinebreak {white-space:nowrap}\n" +
    				 "span.roman {font-family:serif; font-weight:normal}\n" +
    				 "span.sansserif {font-family:sans-serif; font-weight:normal}\n" +
    				 "ul.no-bullet {list-style: none}\n" +
    				 "body {margin-left: 5%; margin-right: 5%;}\n" +
    				 "\n" +
    				 "H1 {             \n" +
    				 "    background: white;\n" +
    				 "    color: rgb(25%, 25%, 25%);\n" +
    				 "    font-family: monospace;\n" +
    				 "    font-size: xx-large;\n" +
    				 "    text-align: center\n" +
    				 "}\n" +
    				 "\n" +
    				 "H2 {\n" +
    				 "    background: white;\n" +
    				 "    color: rgb(40%, 40%, 40%);\n" +
    				 "    font-family: monospace;\n" +
    				 "    font-size: x-large;\n" +
    				 "    text-align: center\n" +
    				 "}\n" +
    				 "\n" +
    				 "H3 {\n" +
    				 "    background: white;\n" +
    				 "    color: rgb(40%, 40%, 40%);\n" +
    				 "    font-family: monospace;\n" +
    				 "    font-size: large\n" +
    				 "}\n" +
    				 "\n" +
    				 "H4 {\n" +
    				 "    background: white;\n" +
    				 "    color: rgb(40%, 40%, 40%);\n" +
    				 "    font-family: monospace\n" +
    				 "}\n" +
    				 "\n" +
    				 "span.samp{font-family: monospace}\n" +
    				 "span.command{font-family: monospace}\n" +
    				 "span.option{font-family: monospace}\n" +
    				 "span.file{font-family: monospace}\n" +
    				 "span.env{font-family: monospace}\n" +
    				 "\n" +
    				 "ul {\n" +
    				 "    margin-top: 0.25ex;\n" +
    				 "    margin-bottom: 0.25ex;\n" +
    				 "}\n" +
    				 "li {\n" +
    				 "    margin-top: 0.25ex;\n" +
    				 "    margin-bottom: 0.25ex;\n" +
    				 "}\n" +
    				 "p {\n" +
    				 "    margin-top: 0.6ex;\n" +
    				 "    margin-bottom: 1.2ex;\n" +
    				 "}\n" +
    				 "caption {\n" +
	                 "	font:  20pt Arial, Helvetica, sans-serif;\n" +
	                 "	text-align: left;\n" +
	                 "	height: 45px;\n" +
	                 "	color: #243D02;\n" +
	                 "	border-top: 1px solid #243D02;\n" +
	                 "}\n" +
	                 "table, th, td {border: 0px;)\n"
    				 ;
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
                    "		<TITLE>BEAST " + version.getVersionString() + " Documentation</TITLE>\n" +
                    "		</HEAD>\n" +
                    "		<FRAMESET cols='20%, 80%'>\n" +
                    "		  <FRAMESET rows='50, 200'>\n" +
//                    "		      <FRAME src='http://www.omnomnomnom.com/random/rotate.php' align='center'>\n" +
                    "		      <FRAME src='beast.png' align='center'>\n" +
                    "		      <FRAME src='contents.html'>\n" +
                    "		  </FRAMESET>\n" +
                    "		  <FRAME name='display' src='contents.html'>\n" +
                    "		</FRAMESET>\n" +
                    "		</HTML>");
        }

        {
        }

        {
        	try {
	            InputStream in = new FileInputStream(new File("src/beast/app/draw/icons/beast.png"));
	            OutputStream out = new FileOutputStream(new File(m_sDir + "/beast.png"));
	            byte[] buf = new byte[1024];
	            int len;
	            while ((len = in.read(buf)) > 0) {
	                out.write(buf, 0, len);
	            }
	            in.close();
	            out.close();
        	} catch (Exception e) {
				// TODO: handle exception
			}
        }
        PrintStream out = new PrintStream(m_sDir + "/contents.html");
        out.println("<html>\n<head><title>BEAST " + version.getVersionString() + " Documentation index</title>\n" +
                "<link rel='StyleSheet' href='doc.css' type='text/css'>\n" +
                "</head>\n");
        out.println("<body>\n");
        out.println("<h1>BEAST " + version.getVersionString() + " Documentation index</h1>\n");
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
    String[] getImplementations(BEASTObject plugin) {
        String sName = plugin.getClass().getName();
        List<String> sImplementations = new ArrayList<>();
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
        Annotation[] classAnnotations = pluginClass.getAnnotations();
        for (Annotation annotation : classAnnotations) {
            if (annotation instanceof Description) {
                Description description = (Description) annotation;
                if (description.isInheritable()) {
                    sStr += description.value();
                } else {
                    return null;
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
            System.err.println("Page creation failed for " + sPlugin + ": " + e.getMessage());
        }
    } // createPluginPage


    public String getHTML(String sPlugin, boolean bUseExternalStyleSheet) throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("<html>\n<head>\n<title>BEAST " + version.getVersionString() + " Documentation: " + sPlugin + "</title>\n");
        if (bUseExternalStyleSheet) {
            buf.append("<link rel='StyleSheet' href='/tmp/styles.css' type='text/css'>\n");
        } else {
            buf.append("<style type='text/css'>\n");
            buf.append(getCSS());
            buf.append("</style>\n");
        }
        buf.append("</head>\n");
        buf.append("<body>\n");
        buf.append("<h1>BEAST " + version.getVersionString() + " Documentation: " + sPlugin + "</h1>\n");
        BEASTObject plugin = (BEASTObject) Class.forName(sPlugin).newInstance();

        // show all implementation of this plug-in
        String[] sImplementations = m_isa.get(sPlugin);
        if (sImplementations == null) {
            // this class is not documented, perhaps outside ClassDiscover path?
            buf.append("No documentation available for " + sPlugin + ". Perhaps it is not in the ClassDiscovery path\n");
            buf.append("</body>\n");
            buf.append("</html>\n");
            return buf.toString();
        }

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

        // show short list its inputs
        buf.append("<h2>Inputs:</h2>\n");
        buf.append("<p>");
        List<Input<?>> inputs = plugin.listInputs();
        for (Input<?> input : inputs) {
        	buf.append("<a href='#" + input.getName()+"'>" + input.getName() + "</a>, ");
        }
        buf.delete(buf.length() - 3, buf.length()-1);
        buf.append("</p>\n");
        
        // list its inputs
        if (inputs.size() == 0) {
            buf.append("&lt;none&gt;");
        }
        for (Input<?> input : inputs) {
        	buf.append("<p>&nbsp</p>");
            buf.append("<table id='" + input.getName() + "' border='1px' width='90%'>\n");
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
    String getType(BEASTObject plugin, String sName) {
        try {
            Field[] fields = plugin.getClass().getFields();
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].getType().isAssignableFrom(Input.class)) {
                    final Input<?> input = (Input<?>) fields[i].get(plugin);
                    if (input.getName().equals(sName)) {
                        Type t = fields[i].getGenericType();
                        Type[] genericTypes = ((ParameterizedType) t).getActualTypeArguments();
                        if (input.getType() != null) {
                            return (input.getType().isAssignableFrom(BEASTObject.class) ? "<a href='" + input.getType().getName() + ".html'>" : "") +
                                    input.getType().getName() +
                                    (input.get() != null && input.get() instanceof List<?> ? "***" : "") +
                                    (input.getType().isAssignableFrom(BEASTObject.class) ? "</a>" : "");
                        }
                        if (input.get() != null && input.get() instanceof List<?>) {
                            Type[] genericTypes2 = ((ParameterizedType) genericTypes[0]).getActualTypeArguments();
                            Class<?> _class = (Class<?>) genericTypes2[0];
                            Object o = null;
                            try {
                                o = Class.forName(_class.getName()).newInstance();
                            } catch (Exception e) {
                            }
                            if (o != null && o instanceof BEASTObject) {
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
                            if (o != null && o instanceof BEASTObject) {
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
            AddOnManager.loadExternalJars();
            DocMaker b = new DocMaker(args);
            b.generateDocs();
            System.err.println("Done!!!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    } // main


} // BeastDocMaker
