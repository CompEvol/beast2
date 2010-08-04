
/*
 * File Beast1To2.java
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.TransformerException;

/** Conversion of Beast version 1 xml files to Beast 2.0 xml.
 * Usage: Beast1To2 beast1.xml
 * Output is written to stdout, so use
 * Beast1To2 beast1.xml > beast2.xml
 * to get output in beast2.xml.
 *
 *
 * NB: current limitations Only alignments are converted.
 */
public class Beast1To2 {
	final static String BEAST1TO2_XSL_FILE = "src/beast/app/beast1To2.xsl";
	String m_sXSL;

	public Beast1To2(String [] args) throws Exception {
		BufferedReader fin = new BufferedReader(new FileReader((args.length < 2 ? BEAST1TO2_XSL_FILE : args[1])));
		StringBuffer buf = new StringBuffer();
		while (fin.ready()) {
			buf.append(fin.readLine());

		}
		m_sXSL = buf.toString();
	}

	/** applies beast 1 to t2 XSL conversion script (specified in m_sXSL)
	 */
	public static void main(String [] args) throws TransformerException {
		try {
			String sBeast1 = args[0];

			Beast1To2 b = new Beast1To2(args);
			StringWriter strWriter = new StringWriter();
			Reader xmlInput =  new FileReader(sBeast1);
			javax.xml.transform.Source xmlSource =
	            new javax.xml.transform.stream.StreamSource(xmlInput);
			Reader xslInput =  new StringReader(b.m_sXSL);
		    javax.xml.transform.Source xsltSource =
	            new javax.xml.transform.stream.StreamSource(xslInput);
		    javax.xml.transform.Result result =
	            new javax.xml.transform.stream.StreamResult(strWriter);
		    // create an instance of TransformerFactory
		    javax.xml.transform.TransformerFactory transFact = javax.xml.transform.TransformerFactory.newInstance();
		    javax.xml.transform.Transformer trans = transFact.newTransformer(xsltSource);

		    trans.transform(xmlSource, result);
		    System.out.println(strWriter.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	} // main

} // class Beast1To2
