
/*
 * File Rect.java
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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JPanel;

import org.w3c.dom.Node;

public class Rect extends Shape {
	int m_nPenWidth = 1;
	boolean m_bFilled = true;
	Color m_fillcolor = DEFUALT_FILL_COLOR;
	Color m_pencolor = DEFUALT_PEN_COLOR;

	public Rect() {}
	public Rect(Node node, Document doc) {
		parse(node, doc);
	} // c'tor

	public void draw(Graphics2D g, JPanel panel) {
		if (m_bFilled) {
			g.setColor(m_fillcolor);
			g.fillRect(m_x, m_y, m_w, m_h);
		}
		g.setStroke(new BasicStroke(m_nPenWidth));
		g.setColor(m_pencolor);
		g.drawRect(m_x, m_y, m_w, m_h);
		drawLabel(g);
	} // draw




	void parse(Node node, Document doc) {
		super.parse(node, doc);
		if (node.getAttributes().getNamedItem("penwidth") != null) {
			m_nPenWidth = (new Integer(node.getAttributes().getNamedItem("penwidth").getNodeValue())).intValue();
		}
		if (node.getAttributes().getNamedItem("fillcolor") != null) {
			m_fillcolor = string2Color(node.getAttributes().getNamedItem("fillcolor").getNodeValue());
		}
		if (node.getAttributes().getNamedItem("pencolor") != null) {
			m_pencolor = string2Color(node.getAttributes().getNamedItem("pencolor").getNodeValue());
		}
		if (node.getAttributes().getNamedItem("filled") != null) {
			m_bFilled = !node.getAttributes().getNamedItem("filled").getNodeValue().equals("no");
		}
	} // parse

	Color string2Color(String sColor) {
		int iSpace = sColor.indexOf(' ');
		if (iSpace < 0) {
			return new Color(128,128,128);
		}
		int iStart = 0;
		String sR = sColor.substring(iStart,iSpace);
		int r = (new Integer(sR)).intValue();
		iStart = iSpace+1;
		iSpace = sColor.indexOf(' ', iStart);
		if (iSpace < 0) {
			return new Color(128,128,128);
		}
		String sG = sColor.substring(iStart,iSpace);
		int g = (new Integer(sG)).intValue();
		iStart = iSpace+1;
		iSpace = sColor.indexOf(' ', iStart);
		if (iSpace < 0) {
			iSpace = sColor.length();
		}
		String sB = sColor.substring(iStart,iSpace);
		int b = (new Integer(sB)).intValue();
		return new Color(r,g,b);
	} // string2Color

	String getAtts() {
		return
		(m_nPenWidth != 1 ? " penwidth='" + m_nPenWidth + "'" : "") +
		(!m_bFilled ? " filled='no'" : "") +
		(m_fillcolor.equals(DEFUALT_FILL_COLOR)? "" : " fillcolor='" + m_fillcolor.getRed() + " " + m_fillcolor.getGreen() + " " + m_fillcolor.getBlue() + "'") +
		(m_pencolor.equals(DEFUALT_PEN_COLOR)? "" : " pencolor='" + m_pencolor.getRed() + " " + m_pencolor.getGreen() + " " + m_pencolor.getBlue() + "'") +
		 super.getAtts();
	}
	boolean intersects(int nX, int nY) {
		return (nX>=m_x - 1&& nX <= m_x+m_w + 1&& nY >= m_y - 1 && nY <= m_y+m_h + 1);
	}
	boolean intersects(Rectangle rect) {
		return rect.intersects(m_x-1, m_y-1, m_w + 2,m_h + 2);
	}

	public Color getFillColor() {return m_fillcolor;}
	Color getPenColor() {return m_pencolor;}
	void setFillColor(Color color) {m_fillcolor = color;}
	void setPenColor(Color color) {m_pencolor = color;}

	boolean isFilled() {return m_bFilled;}
	void toggleFilled() {m_bFilled = !m_bFilled;}
	int getPenWidth() {return m_nPenWidth;}
	void setPenWidth(int nPenWidth) {m_nPenWidth = nPenWidth;}
} // class Rectangle
