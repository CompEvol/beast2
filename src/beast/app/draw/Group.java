
/*
 * File Group.java
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

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import java.util.ArrayList;

import javax.swing.JPanel;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Group extends Shape {
	int m_x2 = 0;
	int m_y2 = 0;
	int m_w2 = 1;
	int m_h2 = 1;

	List<Shape> m_objects;
	public Group() {
		super();
		m_objects = new ArrayList<Shape>();
	}
	public Group(Node node, Document doc) {
		m_objects = new ArrayList<Shape>();
		parse(node, doc);
		normalize();
	}
	public Group(List<Shape>  objects) {
		m_objects = new ArrayList<Shape>();
		m_objects.addAll(objects);
		normalize();
	}
	public void draw(Graphics2D g, JPanel panel) {
		//AffineTransform oldTransform = g.getTransform();
		//AffineTransform newTranform = AffineTransform.getScaleInstance((double)m_w/(double)m_w2, (double)m_h/(double)m_h2);
		//g.setTransform(newTranform);
		//g.translate(m_x - m_x2, m_y - m_y2);
		for (int i = 0; i < m_objects.size(); i++) {
			((Shape)m_objects.get(i)).draw(g, panel);
		}

		//g.setTransform(oldTransform);
	}
	void parse(Node node, Document doc) {
		super.parse(node, doc);
		if (node.getAttributes().getNamedItem("x2") != null) {
			m_x2 = (new Integer(node.getAttributes().getNamedItem("x2").getNodeValue())).intValue();
		}
		if (node.getAttributes().getNamedItem("y2") != null) {
			m_y2 = (new Integer(node.getAttributes().getNamedItem("y2").getNodeValue())).intValue();
		}
		if (node.getAttributes().getNamedItem("w2") != null) {
			m_w2 = (new Integer(node.getAttributes().getNamedItem("w2").getNodeValue())).intValue();
		}
		if (node.getAttributes().getNamedItem("h2") != null) {
			m_h2 = (new Integer(node.getAttributes().getNamedItem("h2").getNodeValue())).intValue();
		}
		NodeList nodes = node.getChildNodes();
		for (int iNode = 0; iNode < nodes.getLength(); iNode++) {
			Node childNode = nodes.item(iNode);
			if (childNode.getNodeType()== Node.ELEMENT_NODE) {
				m_objects.add(Document.parseNode(childNode, doc));
			}
		}
	}
	String getAtts() {
		return super.getAtts() +
		" x2='" + m_x2 + "'" +
		" y2='" + m_y2 + "'" +
		" w2='" + m_w2 + "'" +
		" h2='" + m_h2 + "'";
	}

	public String getXML() {
		StringBuffer sStr = new StringBuffer();
		sStr.append("<group" + getAtts() + ">\n");
		for (int i = 0; i < m_objects.size(); i++) {
			sStr.append("   ");
			sStr.append(((Shape)m_objects.get(i)).getXML());
			sStr.append("\n");
		}
		sStr.append("</group>\n");
		return sStr.toString();
	}
	boolean intersects(int nX, int nY) {
		for (int i = 0; i < m_objects.size(); i++) {
			if (((Shape)m_objects.get(i)).intersects(nX, nY)) {
				return true;
			}
		}
		return false;
	}
	boolean intersects(Rectangle rect) {
		for (int i = 0; i < m_objects.size(); i++) {
			if (((Shape)m_objects.get(i)).intersects(rect)) {
				return true;
			}
		}
		return false;
	}
	void normalize() {
		m_x = ((Shape)m_objects.get(0)).m_x;
		m_y = ((Shape)m_objects.get(0)).m_y;
		m_w = m_x;
		m_h = m_y;
		for (int i = 0; i < m_objects.size(); i++) {
			if (((Shape)m_objects.get(i)).getX()<m_x) {m_x = ((Shape)m_objects.get(i)).getX();}
			if (((Shape)m_objects.get(i)).getX2()>m_w) {m_w = ((Shape)m_objects.get(i)).getX2();}
			if (((Shape)m_objects.get(i)).getY()<m_y) {m_y = ((Shape)m_objects.get(i)).getY();}
			if (((Shape)m_objects.get(i)).getY2()>m_h) {m_h = ((Shape)m_objects.get(i)).getY2();}
		}
		m_w = m_w - m_x;
		m_h = m_h - m_y;
		m_x2 = m_x;
		m_y2 = m_y;
		m_w2 = m_w;
		m_h2 = m_h;
	} // normalize
	void movePosition(int nOffsetX, int nOffsetY, int nToX, int nToY) {
		for (int i = 0; i < m_objects.size(); i++) {
			Shape shape = (Shape)m_objects.get(i);
			int nOffsetX2 = shape.offsetX(nOffsetX + m_x);
			int nOffsetY2 = shape.offsetY(nOffsetY + m_y);
			shape.movePosition(nOffsetX2, nOffsetY2, nToX, nToY);
		}
		m_x = nToX - nOffsetX;
		m_y = nToY - nOffsetY;
	} // movePosition

	void movePoint(int nPoint, int nOffsetX, int nOffsetY, int nToX, int nToY) {
		//movePosition(nOffsetX, nOffsetY, nToX, nToY);
	} // movePoint
} // class Group
