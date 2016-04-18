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
import java.io.FileNotFoundException;
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
import beast.core.util.Log;
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

    /**
     * output directory *
     */
    String m_sDir = "/tmp";
    /**
     * names of the plug-ins to document *
     */
    List<String> m_beastObjectNames;
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
    
    BEASTVersion2 version = new BEASTVersion2();
    
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
        m_beastObjectNames = AddOnManager.find(beast.core.BEASTObject.class, AddOnManager.IMPLEMENTATION_DIR);
        /** determine hierarchical relation between plug-ins **/
        m_isa = new HashMap<>();
        m_ancestors = new HashMap<>();
        m_descriptions = new HashMap<>();
        m_sLoggables = new HashSet<>();
        for (String beastObjectName : m_beastObjectNames) {
            m_ancestors.put(beastObjectName, new ArrayList<>());
        }
        for (String beastObjectName : m_beastObjectNames) {
            try {
                Class<?> _class = Class.forName(beastObjectName);
                BEASTObject beastObject = (BEASTObject) _class.newInstance();
                String description = getInheritableDescription(beastObject.getClass());
                Log.warning.println(beastObjectName + " => " + description);
                m_descriptions.put(beastObjectName, description);
                String[] implementations = getImplementations(beastObject);
                m_isa.put(beastObjectName, implementations);
                for (String imp : implementations) {
                    m_ancestors.get(imp).add(beastObjectName);
                }
                if (beastObject instanceof Loggable) {
                    m_sLoggables.add(beastObjectName);
                }
            } catch (Exception e) {
                Log.err.println(beastObjectName + " not documented :" + e.getMessage());
            }
        }
    } // c'tor


    /**
     * print @Description and Input.description info so that it can
     * be inserted in the code before creating Javadoc documentation
     * for the Beast II SDK.
     */
    void makeJavaDoc() {
        for (String beastObjectName : m_beastObjectNames) {
            try {
                BEASTObject beastObject = (BEASTObject) Class.forName(beastObjectName).newInstance();
                Log.info.println(beastObjectName + ":@description:" + beastObject.getDescription());
                for (Input<?> input : beastObject.listInputs()) {
                    Log.info.println(beastObjectName + ":" + input.getName() + ":" + input.getTipText());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * create CSS style sheet for all pages 
     * @throws FileNotFoundException *
     */
    void createCSS() throws FileNotFoundException  {
        PrintStream out = new PrintStream(m_sDir + "/doc.css");
        out.println(getCSS());
        out.close();
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
     * create plug in index pages, shown in left frame 
     * @throws FileNotFoundException *
     */
    void createIndex() throws FileNotFoundException  {
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
            out.close();
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
				// ignore exception -- too bad we could not 
        		Log.warning.println("WARNING: something went wrong copying beast.png image:" + e.getMessage());
			}
        }
        PrintStream out = new PrintStream(m_sDir + "/contents.html");
        out.println("<html>\n<head><title>BEAST " + version.getVersionString() + " Documentation index</title>\n" +
                "<link rel='StyleSheet' href='doc.css' type='text/css'>\n" +
                "</head>\n");
        out.println("<body>\n");
        out.println("<h1>BEAST " + version.getVersionString() + " Documentation index</h1>\n");
        String prev = null;
        String prevPackage = null;
        for (String beastObjectName : m_beastObjectNames) {
            String next = beastObjectName.substring(0, beastObjectName.indexOf('.'));
            if (prev != null && !next.equals(prev)) {
                out.println("<hr/>");
            }
            prev = next;
            String name = beastObjectName.substring(beastObjectName.lastIndexOf('.') + 1);
            String packageName = beastObjectName.substring(0, beastObjectName.lastIndexOf('.'));
            // count nr of packages
            int i = 0;
            while (beastObjectName.indexOf('.', i) > 0) {
                name = "." + name;
                i = beastObjectName.indexOf('.', i) + 1;
            }
            Log.warning.println(name + " <= " + beastObjectName);
            if (prevPackage == null || !packageName.equals(prevPackage)) {
                out.println("<span style='color:grey'>" + packageName + "</span><br/>");
            }
            out.println("<a href='" + beastObjectName + ".html' target='display'>" + name + "</a><br/>");
            prevPackage = packageName;
        }
        out.println("</body>\n");
        out.println("</html>\n");
        out.close();
    } // createIndex

    /**
     * Find all beastObjects that are derived from given beastObject *
     */
    String[] getImplementations(BEASTObject beastObject) {
        String name = beastObject.getClass().getName();
        List<String> implementations = new ArrayList<>();
        for (String beastObjectName : m_beastObjectNames) {
            try {
                if (!beastObjectName.equals(name) && beastObject.getClass().isAssignableFrom(Class.forName(beastObjectName))) {
                    implementations.add(beastObjectName);
                }
            } catch (ClassNotFoundException e) {
            }
        }
        return implementations.toArray(new String[0]);
    }

    /**
     * Extract description from @Description annotation
     * but only if the description is inheritable *
     */
    String getInheritableDescription(Class<?> beastObjectClass) {
        String str = "";
        Class<?> superClass = beastObjectClass.getSuperclass();
        if (superClass != null) {
            String superName = getInheritableDescription(superClass);
            if (superName != null) {
                str += superName + "<br/>";
            }
        }
        Annotation[] classAnnotations = beastObjectClass.getAnnotations();
        for (Annotation annotation : classAnnotations) {
            if (annotation instanceof Description) {
                Description description = (Description) annotation;
                if (description.isInheritable()) {
                    str += description.value();
                } else {
                    return null;
                }
            }
        }
        return str;
    }

    /**
     * Create page for individual plug-in 
     * @throws FileNotFoundException *
     */
    void createBEASTObjectPage(String beastObjectName) throws FileNotFoundException {
        PrintStream out = new PrintStream(m_sDir + "/" + beastObjectName + ".html");
        try {
            out.print(getHTML(beastObjectName, true));
        } catch (Exception e) {
        	Log.warning.println("Page creation failed for " + beastObjectName + ": " + e.getMessage());
        }
        out.close();
    } // createBEASTObjectPage


    public String getHTML(String beastObjectName, boolean useExternalStyleSheet) throws InstantiationException, IllegalAccessException, ClassNotFoundException  {
        StringBuffer buf = new StringBuffer();
        buf.append("<html>\n<head>\n<title>BEAST " + version.getVersionString() + " Documentation: " + beastObjectName + "</title>\n");
        if (useExternalStyleSheet) {
            buf.append("<link rel='StyleSheet' href='/tmp/styles.css' type='text/css'>\n");
        } else {
            buf.append("<style type='text/css'>\n");
            buf.append(getCSS());
            buf.append("</style>\n");
        }
        buf.append("</head>\n");
        buf.append("<body>\n");
        buf.append("<h1>BEAST " + version.getVersionString() + " Documentation: " + beastObjectName + "</h1>\n");
        BEASTObject beastObject = (BEASTObject) Class.forName(beastObjectName).newInstance();

        // show all implementation of this plug-in
        String[] implementations = m_isa.get(beastObjectName);
        if (implementations == null) {
            // this class is not documented, perhaps outside ClassDiscover path?
            buf.append("No documentation available for " + beastObjectName + ". Perhaps it is not in the ClassDiscovery path\n");
            buf.append("</body>\n");
            buf.append("</html>\n");
            return buf.toString();
        }

        if (implementations.length > 0) {
            buf.append("<table border='1px'>\n");
            buf.append("<thead><tr><td>implemented by the following</td></tr></thead>\n");
            for (String imp : implementations) {
                buf.append("<tr><td><a href='" + imp + ".html'>" + imp + "</a></td></tr>\n");
            }
            buf.append("</table>\n");
        }

        // show descriptions of all plug-ins implemented by this plug in...
        buf.append("<p>" + m_descriptions.get(beastObjectName) + "</p>\n");

        // show citation (if any)
        Citation citation = beastObject.getCitation();
        if (citation != null) {
            buf.append("<h2>Reference:</h2><p>" + citation.value() + "</p>\n");
            if (citation.DOI().length() > 0) {
                buf.append("<p><a href=\"http://dx.doi.org/" + citation.DOI() + "\">doi:" + citation.DOI() + "</a></p>\n");
            }
        }

        // show if this is Loggable
        if (m_sLoggables.contains(beastObjectName)) {
            buf.append("<p>Logable:");
            buf.append(" yes, this can be used in a log.");
            buf.append("</p>\n");
//        } else {
//        	buf.append(" no, this cannot be used in a log.");
        }

        // show short list its inputs
        buf.append("<h2>Inputs:</h2>\n");
        buf.append("<p>");
        List<Input<?>> inputs = beastObject.listInputs();
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
            buf.append("<thead><tr bgcolor='#AAAAAA'><td>type: " + getType(beastObject, input.getName()) + "</td></tr></thead>\n");
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
                case FORBIDDEN:
                    buf.append("Forbidden: must not be specified");
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
     * determine type of input of a plug in with name name
     */
    String getType(BEASTObject beastObject, String name) {
        try {
            Field[] fields = beastObject.getClass().getFields();
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].getType().isAssignableFrom(Input.class)) {
                    final Input<?> input = (Input<?>) fields[i].get(beastObject);
                    if (input.getName().equals(name)) {
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
     * @throws FileNotFoundException 
     */
    public void generateDocs() throws FileNotFoundException {
        // first, produce CSS & index page
        createCSS();
        createIndex();
        // next, produce pages for individual plug-ins
        for (String beastObjectName : m_beastObjectNames) {
            createBEASTObjectPage(beastObjectName);
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
