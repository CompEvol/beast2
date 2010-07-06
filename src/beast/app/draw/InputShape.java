
/*
 * File Ellipse.java
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
package beast.app.draw;

import java.awt.BasicStroke;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

import org.w3c.dom.Node;

import beast.core.Plugin;

public class InputShape extends Rect {
	public InputShape() {super();}
	public InputShape(Node node, Document doc) {
		parse(node, doc);
	}

	public PluginShape m_function = null;
	PluginShape getFunction() {return m_function;}
	void setFunction(PluginShape function) {m_function = function;}

	String getInputName() throws Exception {
		String sName = getLabel();
		if (sName.indexOf('=') >= 0) {
			sName = sName.substring(0, sName.indexOf('='));
		}
		return sName;
		// should never get here
		//throw new Exception("Ellipse::getInput should not get here. Label not properly set/validated");
	}
	Plugin getPlugin() {
		return m_function.m_function;
	}


	public void draw(Graphics2D g, JPanel panel) {
		if (m_function == null || m_function.m_bNeedsDrawing) {
			if (m_bFilled) {
				g.setColor(m_fillcolor);
				g.fillOval(m_x, m_y, m_w, m_h);
			}
			g.setStroke(new BasicStroke(m_nPenWidth));
			g.setColor(m_pencolor);
			g.drawOval(m_x, m_y, m_w, m_h);
			if (getLabel() !=null) {
				FontMetrics fm = g.getFontMetrics(g.getFont());
				String sLabel = getLabel();
				int i = 0;
				g.drawString(sLabel, m_x + m_w/2 - fm.stringWidth(sLabel), m_y + m_h/2 + i * fm.getHeight());
			}
		}
	}
	
	void parse(Node node, Document doc) {
		super.parse(node, doc);
	}
	public String getXML() {
		return "<ellipse" + getAtts() + "/>";
	}
	boolean intersects(int nX, int nY) {
		return (m_x+m_w/2-nX)*(m_x+m_w/2-nX)+ (m_y+m_h/2-nY)*(m_y+m_h/2-nY) < m_w*m_w/4+m_h*m_h/4;
	}
	String getPostScript() {
		StringBuffer sStr = new StringBuffer();
		if (m_bFilled) {
			sStr.append((m_fillcolor.getRed()/256.0) + " " + (m_fillcolor.getGreen()/256.0) + " " + (m_fillcolor.getBlue()/256.0) + " setrgbcolor\n");
			sStr.append("newpath " + m_x + " " + (500-m_y) + " " + (m_w/2) + " " + (m_h/2));
			sStr.append("0 360 ellipse fill\n");
		}
		sStr.append((m_pencolor.getRed()/256.0) + " " + (m_pencolor.getGreen()/256.0) + " " + (m_pencolor.getBlue()/256.0) + " setrgbcolor\n");
		sStr.append(m_nPenWidth + " setlinewidth\n");
		sStr.append("newpath " + (m_x+ m_w/2) + " " + (500-m_y - m_h/2) + " " + (m_w/2) + " " + (m_h/2));
		sStr.append(" 0 360 ellipse stroke\n");
		if (m_sLabel!=null && m_sLabel!="") {
			sStr.append("/Times-Roman findfont 12 scalefont setfont\n");
			sStr.append((m_x + m_w/2 - m_sLabel.length() * 6) + " " + (500-m_y + -m_h/2-6)+ " moveto\n");
			sStr.append("(" + m_sLabel + ") show\n");
		}
		return sStr.toString();
	}
} // class Ellipse
