package test.beast.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;

import org.junit.Test;

import beast.base.evolution.substitutionmodel.JukesCantor;
import beast.base.parser.XMLParser;
import beast.pkgmgmt.BEASTVersion;
import junit.framework.TestCase;

public class XMLParserTest extends TestCase {

	
	// Note that this test must run in a separate class from XMLTest
	// since the XMLParser globally imports the class map
	// so if another test were run before this one, the class map 
	// (which this test temporarily changes) will not be picked up		
    @Test
    public void testClassMap() throws IOException {
    	// back up version.xml 
        Files.move(new File("version.xml").toPath(), 
        		new File("version.xml.backup").toPath(), 
        		java.nio.file.StandardCopyOption.REPLACE_EXISTING);

    	
    	// create new version.xml
    	PrintStream out = new PrintStream(new File("version.xml"));
    	out.println("<package name='BEAST' version='" + new BEASTVersion().getVersion() + "'>");
    	out.println("<map from='beast.evolution.substitutionmodel.JoMamma' to='beast.base.evolution.substitutionmodel.JukesCantor'/>");
    	out.println("</package>");
    	out.close();
    	
    	// parse XML containing entry in map
    	Object o = null;
    	try {
	    	String xml = "<beast namespace=\"beast.base.evolution.substitutionmodel:beast.base.evolution.likelihood\" version=\"2.6\">"
	    			+ "<input spec='JoMamma'/>"
	    			+ "</beast>";
	    
	    	XMLParser parser = new XMLParser();
    		o = parser.parseBareFragment(xml, false);
    	} catch (Throwable e) {
    		e.printStackTrace();
    	}

    	// restore version.xml
        Files.move(new File("version.xml.backup").toPath(), 
        		new File("version.xml").toPath(), 
        		java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        
        assertEquals(true, o instanceof JukesCantor);
    }
}
