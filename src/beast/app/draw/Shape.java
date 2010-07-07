
/*
 * File Shape.java
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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

import java.awt.Rectangle;
import java.util.List;
import java.util.ArrayList;

import javax.swing.JPanel;

import org.w3c.dom.Node;

public class Shape {
	int m_x = 0;
	int m_y = 0;
	int m_w = 1;
	int m_h = 1;
	public boolean m_bNeedsDrawing = true;

	final static Color DEFUALT_FILL_COLOR =new Color(128,128,128);
	final static Color DEFUALT_PEN_COLOR =new Color(0, 0, 0);
	String m_id;
	public Shape() {}
	public Shape(Node node, Document doc) {}
	public void draw(Graphics2D g, JPanel panel) {}
	void drawLabel(Graphics2D g) {
		if (getLabel() !=null) {
			FontMetrics fm = g.getFontMetrics(g.getFont());
			String sLabel = getLabel();
			int i = 0;
			while (sLabel.indexOf('\n')>= 0) {
				String sStr = sLabel.substring(0,sLabel.indexOf('\n'));
				g.drawString(sStr, m_x + m_w/2 - fm.stringWidth(sStr)/2, m_y + m_h/2 + i * fm.getHeight());
				sLabel = sLabel.substring(sStr.length() + 1);
				i++;
			}
			g.drawString(sLabel, m_x + m_w/2 - fm.stringWidth(sLabel)/2, m_y + m_h/2 + i * fm.getHeight());
		}
	} // drawLabel
	List<TrackPoint> getTracker() {
		List<TrackPoint> tracker = new ArrayList<TrackPoint>(8);
		tracker.add(new TrackPoint(m_x,m_y, Cursor.NW_RESIZE_CURSOR ));
		tracker.add(new TrackPoint(m_x+m_w,m_y, Cursor.NE_RESIZE_CURSOR ));
		tracker.add(new TrackPoint(m_x,m_y+m_h, Cursor.SW_RESIZE_CURSOR ));
		tracker.add(new TrackPoint(m_x+m_w,m_y+m_h, Cursor.SE_RESIZE_CURSOR ));

		tracker.add(new TrackPoint(m_x,m_y+m_h/2, Cursor.W_RESIZE_CURSOR));
		tracker.add(new TrackPoint(m_x+m_w,m_y+m_h/2, Cursor.E_RESIZE_CURSOR));
		tracker.add(new TrackPoint(m_x+m_w/2,m_y, Cursor.N_RESIZE_CURSOR));
		tracker.add(new TrackPoint(m_x+m_w/2,m_y+m_h, Cursor.S_RESIZE_CURSOR));
		return tracker;
	}
	void parse(Node node, Document doc) {
		if (node.getAttributes().getNamedItem("id") != null) {
			m_id = node.getAttributes().getNamedItem("id").getNodeValue();
		}
		if (node.getAttributes().getNamedItem("x") != null) {
			m_x = (new Integer(node.getAttributes().getNamedItem("x").getNodeValue())).intValue();
			m_x += 400;
		}
		if (node.getAttributes().getNamedItem("y") != null) {
			m_y = (new Integer(node.getAttributes().getNamedItem("y").getNodeValue())).intValue();
			if (node.getAttributes().getNamedItem("h") != null) {
				m_h = (new Integer(node.getAttributes().getNamedItem("h").getNodeValue())).intValue();
			}
			m_y = 550-m_y-m_h;
		}
		if (node.getAttributes().getNamedItem("w") != null) {
			m_w = (new Integer(node.getAttributes().getNamedItem("w").getNodeValue())).intValue();
		}
	}
	String XMLnormalizeAtt(String sStr) {
	StringBuffer sStr2 = new StringBuffer();
	for (int iStr = 0; iStr < sStr.length(); iStr++)
	{
		switch (sStr.charAt(iStr))
		{
		case '<':
			sStr2.append("&lt;");
			break;
		case '>':
			sStr2.append("&gt;");
			break;
		case '\"':
			sStr2.append("&quot;");
			break;
		case '\'':
			sStr2.append("&apos;");
			break;
		case '&':
			sStr2.append("&amp;");
			break;
		case 13:
			break;
		case '\n':
			sStr2.append("&#xD;&#xA;");
			break;
		default:
			sStr2.append(sStr.charAt(iStr));
		}
	}
	return sStr2.toString();
	} // XMLnormalizeAtt
	String getAtts() {
		return " id='" + m_id + "'" +
		" x='" + (m_x-400) + "'" +
		" y='" + (550-m_y - m_h) + "'" +
		" w='" + m_w + "'" +
		" h='" + m_h + "'";
	}
	public String getXML() {
		return "<shape" + getAtts() + "/>";
	}
	boolean intersects(int nX, int nY) {
		return false;
	}
	boolean intersects(Rectangle rect) {
		return false;
	}

	int offsetX(int nX) {return nX - m_x;}
	int offsetY(int nY) {return nY - m_y;}
	void offset(int dX, int dY) {
		m_x += dX;
		m_y += dY;
	}
	void movePosition(int nOffsetX, int nOffsetY, int nToX, int nToY) {
		m_x = nToX - nOffsetX;
		m_y = nToY - nOffsetY;
	} // movePosition

	void movePoint(int nPoint, int nOffsetX, int nOffsetY, int nToX, int nToY) {
		switch (nPoint) {
		case 0:
			m_w = m_w +  m_x - nToX + nOffsetX; m_x = nToX - nOffsetX;
			m_h = m_h +  m_y - nToY + nOffsetY; m_y = nToY - nOffsetY;
			break;
		case 1:
			m_w = nToX - nOffsetX - m_x;
			m_h = m_h +  m_y - nToY + nOffsetY; m_y = nToY - nOffsetY;
			break;
		case 2:
			m_w = m_w +  m_x - nToX + nOffsetX; m_x = nToX - nOffsetX;
			m_h = nToY - nOffsetY - m_y;
			break;
		case 3:
			m_w = nToX - nOffsetX - m_x;
			m_h = nToY - nOffsetY - m_y;
			break;
		case 5: m_w = nToX - nOffsetX - m_x; break;
		case 4: m_w = m_w +  m_x - nToX + nOffsetX; m_x = nToX - nOffsetX; break;
		case 7: m_h = nToY - nOffsetY - m_y; break;
		case 6: m_h = m_h +  m_y - nToY + nOffsetY; m_y = nToY - nOffsetY; break;
		}
	} // movePoint

	void normalize() {
		int nX1 = m_x;
		int nX2 = m_x + m_w;
		int nY1 = m_y;
		int nY2 = m_y + m_h;
		m_x = Math.min(nX1, nX2);
		m_w = Math.max(nX1, nX2) - m_x;
		m_y = Math.min(nY1, nY2);
		m_h = Math.max(nY1, nY2) - m_y;
	}

	public Color getFillColor() {return DEFUALT_FILL_COLOR;}
	void setFillColor(Color color) {}
	Color getPenColor() {return DEFUALT_PEN_COLOR;}
	void setPenColor(Color color) {}
	int getX() {return m_x;}
	int getY() {return m_y;}
	int getX2() {return m_x+m_w;}
	int getY2() {return m_y+m_h;}
	void setX(int nX) {m_x = nX;}
	void setY(int nY) {m_y = nY;}
	void setX2(int nX2) {m_w = nX2 - m_x;}
	void setY2(int nY2) {m_h = nY2 - m_y;}
	void setLabel(String sLabel) {}
	String getLabel() {return "";}
	void setURL(String sURL) {}
	String getURL() {return "";}
	boolean isFilled() {return false;}
	void toggleFilled() {}
	int getPenWidth() {return 0;}
	void setPenWidth(int nPenWidth) {}
	String getImageSrc() {return "";}
	void setImageSrc(String sSrc) {};
}
