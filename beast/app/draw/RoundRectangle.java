
/*
 * File RoundRectangle.java
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
import java.util.List;

import javax.swing.JPanel;

import org.w3c.dom.Node;

public class RoundRectangle extends Rect {
	int m_nRoundX = 16;
	int m_nRoundY = 16;
	public RoundRectangle() {
		super();
	}
	public RoundRectangle(Node node, Document doc) {
		parse(node, doc);
	}
	public void draw(Graphics2D g, JPanel panel) {
		if (m_bFilled) {
			g.setColor(m_fillcolor);
			g.fillRoundRect(m_x, m_y, m_w, m_h, m_nRoundX, m_nRoundY);
		}
		g.setStroke(new BasicStroke(m_nPenWidth));
		g.setColor(m_pencolor);
		g.drawRoundRect(m_x, m_y, m_w, m_h, m_nRoundX, m_nRoundY);
		drawLabel(g);
	}
	List<TrackPoint> getTracker() {
		List<TrackPoint> tracker = super.getTracker();
		tracker.add(new TrackPoint(m_x+m_w-m_nRoundX/2,m_y+m_nRoundY/2, Cursor.MOVE_CURSOR ));
//		tracker.addElement(new TrackPoint(m_x,m_y+m_nRoundY, Cursor.E_RESIZE_CURSOR ));
		return tracker;
	} // getTracker

	void movePoint(int nPoint, int nOffsetX, int nOffsetY, int nToX, int nToY) {
		super.movePoint(nPoint, nOffsetX, nOffsetY, nToX, nToY);
		switch (nPoint) {
		case 8:
			m_nRoundX = 2*(-nToX + m_x + m_w);
			if (m_nRoundX > m_w) {m_nRoundX = m_w;}
			if (m_nRoundX < 0) {m_nRoundX = 0;}
			m_nRoundY = 2*(nToY - m_y);
			if (m_nRoundY > m_h) {m_nRoundY = m_h;}
			if (m_nRoundY < 0) {m_nRoundY = 0;}
			break;
		}
	} // movePoint

	void parse(Node node, Document doc) {
		super.parse(node, doc);
		if (node.getAttributes().getNamedItem("roundx") != null) {
			m_nRoundX = (new Integer(node.getAttributes().getNamedItem("roundx").getNodeValue())).intValue();
		}
		if (node.getAttributes().getNamedItem("roundy") != null) {
			m_nRoundY = (new Integer(node.getAttributes().getNamedItem("roundy").getNodeValue())).intValue();
		}
	}
	String getAtts() {
			return
			" roundx='" + m_nRoundX + "'" +
			" roundy='" + m_nRoundY + "'"
			+ super.getAtts();
	}
	public String getXML() {
		return "<roundrectangle" + getAtts() + "/>";
	}
}
