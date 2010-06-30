
/*
 * File Poly.java
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
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.List;
import java.util.ArrayList;

import javax.swing.JPanel;

import org.w3c.dom.Node;

public class Poly extends Rect {
	Polygon m_polygon;
	public Poly(int nX, int nY) {
		m_polygon = new Polygon();
		m_polygon.addPoint(nX, nY);
	}
	public Poly(Node node, Document doc) {
		m_polygon = new Polygon();
		parse(node, doc);
	}
	public void draw(Graphics2D g, JPanel panel) {
		if (m_bFilled) {
			g.setColor(m_fillcolor);
			g.fill(m_polygon);
		}
		g.setStroke(new BasicStroke(m_nPenWidth));
		g.setColor(m_pencolor);
		g.draw(m_polygon);
		drawLabel(g);
	}
	List<TrackPoint> getTracker() {
		List<TrackPoint> tracker = new ArrayList<TrackPoint>();
		for (int i = 0; i < m_polygon.npoints; i++) {
			tracker.add(new TrackPoint(m_polygon.xpoints[i],m_polygon.ypoints[i], Cursor.MOVE_CURSOR));
		}
		return tracker;
	} // getTracker
	void movePosition(int nOffsetX, int nOffsetY, int nToX, int nToY) {
		int dx = m_x - nToX + nOffsetX;
		int dy = m_y - nToY + nOffsetY;
		for (int i = 0; i < m_polygon.npoints; i++) {
			m_polygon.xpoints[i] -= dx;
			m_polygon.ypoints[i] -= dy;
		}
		m_x = nToX - nOffsetX;
		m_y = nToY - nOffsetY;
	} // movePosition
	void offset(int dX, int dY) {
		movePosition(0, 0, m_x + dX, m_y + dY);
	}
	void movePoint(int nPoint, int nOffsetX, int nOffsetY, int nToX, int nToY) {
		m_polygon.xpoints[nPoint] = nToX - nOffsetX;
		m_polygon.ypoints[nPoint] = nToY - nOffsetY;
		normalize();
	} // movePoint

	void parse(Node node, Document doc) {
		super.parse(node, doc);
		if (node.getAttributes().getNamedItem("points") != null) {
			String sPoints = node.getAttributes().getNamedItem("points").getNodeValue();
			int i = -1;
			int iPrev = 0;
			while (sPoints.indexOf(' ',iPrev) > 0) {
				i = sPoints.indexOf(' ', iPrev);
				String sX = sPoints.substring(iPrev, i);
				iPrev = i + 1;
				i = sPoints.indexOf(' ', iPrev);
				String sY = sPoints.substring(iPrev, i);
				m_polygon.addPoint((new Integer(sX)).intValue() + 400, 550-(new Integer(sY)).intValue());
				iPrev = i + 1;
			}
			normalize();
		}
	} // parse
	void normalize() {
		m_x = m_polygon.xpoints[0];
		m_y = m_polygon.ypoints[0];
		m_w = m_x;
		m_h = m_y;
		for (int i = 0; i < m_polygon.npoints; i++) {
			if (m_polygon.xpoints[i]<m_x) {m_x = m_polygon.xpoints[i];}
			if (m_polygon.xpoints[i]>m_w) {m_w = m_polygon.xpoints[i];}
			if (m_polygon.ypoints[i]<m_y) {m_y = m_polygon.ypoints[i];}
			if (m_polygon.ypoints[i]>m_h) {m_h = m_polygon.ypoints[i];}
		}
		m_w = m_w - m_x;
		m_h = m_h - m_y;
	} // normalize
	String getAtts() {
		StringBuffer sPoints = new StringBuffer();
		for (int i = 0; i < m_polygon.npoints; i++) {
			sPoints.append((m_polygon.xpoints[i]-400) + " " + (550-m_polygon.ypoints[i]) + " ");
		}
		return
		" points='" + sPoints.toString() + "'"
		+ super.getAtts();
   }
	public String getXML() {
		return "<poly" + getAtts() + "/>";
	}
	String getPostScript() {
		StringBuffer sPostScript = new StringBuffer();
		if (m_bFilled) {
			sPostScript.append((m_fillcolor.getRed()/256.0) + " " + (m_fillcolor.getGreen()/256.0) + " " + (m_fillcolor.getBlue()/256.0) + " setrgbcolor\n");
			sPostScript.append(m_pencolor.getRed() + " " + m_pencolor.getGreen() + " " + m_pencolor.getBlue() + " setrgbcolor\n");
			sPostScript.append("newpath " + m_polygon.xpoints[0] + " " + (500-m_polygon.ypoints[0]) + " moveto\n");
			for (int i = 1; i < m_polygon.npoints; i++) {
				sPostScript.append(m_polygon.xpoints[i] + " " + (500-m_polygon.ypoints[i]) + " lineto\n");
			}
			sPostScript.append("closepath fill\n");
		}

		sPostScript.append((m_pencolor.getRed()/256.0) + " " + (m_pencolor.getGreen()/256.0) + " " + (m_pencolor.getBlue()/256.0) + " setrgbcolor\n");
		sPostScript.append(m_nPenWidth + " setlinewidth\n");
		sPostScript.append("newpath " + m_polygon.xpoints[0] + " " + (500-m_polygon.ypoints[0]) + " moveto\n");
		for (int i = 1; i < m_polygon.npoints; i++) {
			sPostScript.append(m_polygon.xpoints[i] + " " + (500-m_polygon.ypoints[i]) + " lineto\n");
		}
		sPostScript.append("closepath stroke\n");

		if (m_sLabel!=null && m_sLabel!="") {
			sPostScript.append("/Times-Roman findfont 12 scalefont setfont\n");
			sPostScript.append((m_x + m_w/2 - m_sLabel.length() * 6) + " " + (500-m_y + -m_h/2-6)+ " moveto\n");
			sPostScript.append("(" + m_sLabel + ") show\n");
		}
		return sPostScript.toString();
	}
} // class Poly
