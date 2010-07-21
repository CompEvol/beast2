
/*
 * File Line.java
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
import java.awt.Rectangle;
import java.util.List;
import java.util.ArrayList;

import javax.swing.JPanel;

import org.w3c.dom.Node;

public class Line extends Rect {
	public Line() {}
	public Line(Node node, Document doc) {
		parse(node, doc);
	}
	@Override
	public void draw(Graphics2D g, JPanel panel) {
		g.setStroke(new BasicStroke(m_nPenWidth));
		g.setColor(m_pencolor);
		g.drawLine(m_x, m_y, m_x + m_w, m_y + m_h);
		drawLabel(g);
	}
	@Override
	boolean intersects(int nX, int nY) {
		int nX1 = m_x;
		int nX2 = m_x + m_w;
		int nY1 = m_y;
		int nY2 = m_y + m_h;
		int _x = Math.min(nX1, nX2);
		int _w = Math.max(nX1, nX2) - _x;
		int _y = Math.min(nY1, nY2);
		int _h = Math.max(nY1, nY2) - _y;
		return (nX>_x && nX < _x+_w && nY > _y && nY < _y+_h);
	}
	@Override
	boolean intersects(Rectangle rect) {
		return rect.intersectsLine(m_x, m_y, m_x+m_w,m_y+m_h);
	}
	@Override
	List<TrackPoint> getTracker() {
		List<TrackPoint> tracker = new ArrayList<TrackPoint>();
		tracker.add(new TrackPoint(m_x,m_y, Cursor.MOVE_CURSOR ));
		tracker.add(new TrackPoint(m_x+m_w,m_y+m_h, Cursor.MOVE_CURSOR ));
		return tracker;
	}
	@Override
	void movePoint(int nPoint, int nOffsetX, int nOffsetY, int nToX, int nToY) {
		switch (nPoint) {
		case 0:
			m_w = m_w +  m_x - nToX + nOffsetX; m_x = nToX - nOffsetX;
			m_h = m_h +  m_y - nToY + nOffsetY; m_y = nToY - nOffsetY;
			break;
		case 1:
			m_w = nToX - nOffsetX - m_x;
			m_h = nToY - nOffsetY - m_y;
			break;
		}
	} // movePoint

	public String getXML() {
		return "<line" + getAtts() + "/>";
	}
	void normalize() {}
} // class Line
