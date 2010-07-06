
/*
 * File Arrow.java
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
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.GeneralPath;
import java.util.List;

import javax.swing.JPanel;

import org.w3c.dom.Node;

public class Arrow extends Line {
	String m_sHeadID;
	String m_sTailID;
	String m_sPenStyle;
	boolean m_bHasTail = false;
	boolean m_bHasHead= true;
	Polygon m_polygon;

	final static double m_nArrowAngle = 0.08;
	final static int m_nArrowLength = 15;

	public Arrow(Node node, Document doc) {
		parse(node, doc);
		setArrow();
	}
	public Arrow(Shape tailShape, int x, int y) {
		m_sTailID = tailShape.m_id;
		m_x = x;
		m_y = y;
		m_w = 1;
		m_h = 1;
		setArrow();
	}
	public Arrow(Shape tailShape, PluginShape headShape, String sInputName) {
		m_sTailID = tailShape.m_id;
		m_sHeadID = headShape.getInput(sInputName).m_id;
		//m_sHeadID = headShape.m_id;
		m_x = 0;
		m_y = 0;
		m_w = 1;
		m_h = 1;
		setArrow();
		m_pencolor = Color.gray;
	}
	public void draw(Graphics2D g, JPanel panel) {
		g.setStroke(new BasicStroke(m_nPenWidth));
		g.setColor(m_pencolor);
//		g.fill(m_polygon);
//		g.draw(m_polygon);


//		 for (int i = 0; i < 4; i++) {
//		      if (i == 0 || i == 3)
//		        g.setColor(Color.blue);
//		      else
//		        g.setColor(Color.cyan);
//		      g.fillOval(xs[i] - 6, ys[i] - 6, 12, 12);
//		    }
//		    Graphics2D g2d = (Graphics2D) g;
//		    g2d.setColor(Color.black);
		    GeneralPath path = new GeneralPath();
		    path.moveTo(m_polygon.xpoints[0], m_polygon.ypoints[0]);
		    path.curveTo(
		    		m_polygon.xpoints[0]+20, m_polygon.ypoints[0],
		    		m_polygon.xpoints[1]-40, m_polygon.ypoints[1],
		    		m_polygon.xpoints[1], m_polygon.ypoints[1]);
		    g.draw(path);


		drawLabel(g);
	}
	public void setHead(int w, int h) {
		m_w = w;
		m_h = h;
		setArrow();
	}
	public boolean setHead(Shape shape, List<Shape> objects, Document doc) throws Exception {
		m_sHeadID = shape.m_id;
		adjustCoordinates(objects, true);
		setArrow();
		return setFunctionInput(objects, doc);
	}

	boolean setFunctionInput(List<Shape> objects, Document doc) throws Exception {
		Shape head = null;
		int i = 0;
		while (i < objects.size() && !((Shape)objects.get(i)).m_id.equals(m_sHeadID)) {
			i++;
		}
		head = (Shape)objects.get(i);
		if (head instanceof InputShape && ((InputShape)head).getFunction()!=null) {
			Shape tail = null;
			int j = 0;
			while (j < objects.size() && !((Shape)objects.get(j)).m_id.equals(m_sTailID)) {
				j++;
			}
			tail = (Shape)objects.get(j);
			//try {
				return ((InputShape)head).getFunction().connect(tail, m_sHeadID, doc);
			//} catch (Exception e) {
				//return false;
			//}
		}
		return false;
	}
	void parse(Node node, Document doc) {
		super.parse(node, doc);
		if (node.getAttributes().getNamedItem("headid") != null) {
			m_sHeadID = node.getAttributes().getNamedItem("headid").getNodeValue();
		}
		if (node.getAttributes().getNamedItem("tailid") != null) {
			m_sTailID = node.getAttributes().getNamedItem("tailid").getNodeValue();
		}
		if (node.getAttributes().getNamedItem("hastail") != null) {
			m_bHasTail = node.getAttributes().getNamedItem("hastail").getNodeValue().equals("1");
		}
		if (node.getAttributes().getNamedItem("hashead") != null) {
			m_bHasHead = node.getAttributes().getNamedItem("hashead").getNodeValue().equals("1");
		}
		if (node.getAttributes().getNamedItem("penstyle") != null) {
			m_sPenStyle = node.getAttributes().getNamedItem("penstyle").getNodeValue();
		}
	}
	void setArrow() {
		m_polygon = new Polygon();
		m_polygon.addPoint(m_x, m_y);
		m_polygon.addPoint(m_x+m_w, m_y+m_h);

		//m_points[4] = m_points[1];
		m_polygon.addPoint(m_polygon.xpoints[1], m_polygon.ypoints[1]);
		//m_points[5] = m_points[0];
		m_polygon.addPoint(m_polygon.xpoints[0], m_polygon.ypoints[0]);

		//m_points[8] = m_points[0];
		m_polygon.addPoint(m_polygon.xpoints[0], m_polygon.ypoints[0]);

	if (m_bHasHead && (m_polygon.xpoints[0] != m_polygon.xpoints[1] || m_polygon.ypoints[0] != m_polygon.ypoints[1])) {
			double fi, dx, dy;
			dx = m_polygon.xpoints[0] - m_polygon.xpoints[1];
			dy = m_polygon.ypoints[0] - m_polygon.ypoints[1];
			if (dx != 0) {
				fi = Math.atan(dy / dx);
				if (dx < 0) {
					fi = fi + Math.PI;
				}
			} else {
				if (dy > 0) {
					fi = Math.PI / 2;
				}  else {
					fi = 3 * Math.PI / 2;
				}
			}
			double ARROWANGLE = m_nArrowAngle * Math.PI;
			long ARROWLENGTH = m_nArrowLength;
			//m_points[2] = m_points[1];
			//m_points[2].Offset((int) (ARROWLENGTH * cos(fi + ARROWANGLE)), (int) (ARROWLENGTH * sin(fi + ARROWANGLE)));
			m_polygon.addPoint(m_x+m_w, m_y+m_h);
			m_polygon.addPoint(m_polygon.xpoints[1] + (int) (ARROWLENGTH * Math.cos(fi + ARROWANGLE)), m_polygon.ypoints[1] + (int) (ARROWLENGTH * Math.sin(fi + ARROWANGLE)));
			//m_points[3] = m_points[1];
			//m_points[3].Offset((int) (ARROWLENGTH * cos(fi - ARROWANGLE)), (int) (ARROWLENGTH * sin(fi - ARROWANGLE)));
			m_polygon.addPoint(m_polygon.xpoints[1] + (int) (ARROWLENGTH * Math.cos(fi - ARROWANGLE)), m_polygon.ypoints[1] + (int) (ARROWLENGTH * Math.sin(fi - ARROWANGLE)));
			m_polygon.addPoint(m_x+m_w, m_y+m_h);
	} else {
			//m_points[2] = m_points[1];
//		m_polygon.addPoint(m_x+m_w, m_y+m_h);
			//m_points[3] = m_points[1];
//		m_polygon.addPoint(m_x+m_w, m_y+m_h);
	}

	if (m_bHasTail)
	{
		if (m_polygon.xpoints[0] != m_polygon.xpoints[1] || m_polygon.ypoints[0] != m_polygon.ypoints[1]) {
			double fi, dx, dy;
			dx = m_polygon.xpoints[1] - m_polygon.xpoints[0];
			dy = m_polygon.ypoints[1] - m_polygon.ypoints[0];
			if (dx != 0) {
				fi = Math.atan(dy / dx);
				if (dx < 0) {
					fi = fi + Math.PI;
				}
			} else {
				if (dy > 0) {
					fi = Math.PI / 2;
				}  else {
					fi = 3 * Math.PI / 2;
				}
			}
			double ARROWANGLE = m_nArrowAngle * Math.PI;
			long ARROWLENGTH = m_nArrowLength;
			//m_points[6] = m_points[0];
			//m_points[6].Offset((int) (ARROWLENGTH * cos(fi + ARROWANGLE)), (int) (ARROWLENGTH * sin(fi + ARROWANGLE)));
			m_polygon.addPoint(m_x, m_y);
			m_polygon.addPoint(m_x + (int) (ARROWLENGTH * Math.cos(fi + ARROWANGLE)), m_y + (int) (ARROWLENGTH * Math.sin(fi + ARROWANGLE)));
			//m_points[7] = m_points[0];
			//m_points[7].Offset((int) (ARROWLENGTH * cos(fi - ARROWANGLE)), (int) (ARROWLENGTH * sin(fi - ARROWANGLE)));
			m_polygon.addPoint(m_x + (int) (ARROWLENGTH * Math.cos(fi - ARROWANGLE)), m_y + (int) (ARROWLENGTH * Math.sin(fi - ARROWANGLE)));
			m_polygon.addPoint(m_x, m_y);
		}
	} else {
			//m_points[6] = m_points[0];
//			m_polygon.addPoint(m_x, m_y);
			//m_points[7] = m_points[0];
//			m_polygon.addPoint(m_x, m_y);
	}

	}
	String getAtts() {
		return
		" headid='" + m_sHeadID + "'" +
		" tailid='" + m_sTailID + "'" +
		" penstyle='" + m_sPenStyle + "'" +
		(!m_bHasHead ? " hashead='0'" : "") +
		(m_bHasTail ? " hastail='1'" : "")
		+ super.getAtts();
   }
	public String getXML() {
		return "<arrow" + getAtts() + "/>";
	}
	public Shape m_tail;
	public Shape m_head;
	void resetIDs(List<Shape> objects) {
		for (int i = 0; i < objects.size(); i++) {
			Shape shape = (Shape) objects.get(i);
			if (shape.m_id.equals(m_sHeadID)) {
				m_head = shape;
			}
			if (shape.m_id.equals(m_sTailID)) {
				m_tail = shape;
			}
			if (shape instanceof Group) {
				Group group = (Group) shape;
				resetIDs(group.m_objects);
			}
		}
	}
	void adjustCoordinates(List<Shape> objects, boolean bResetIDs) {
		if (m_tail == null || bResetIDs == true) {
			resetIDs(objects);

		}
		Point tailCenter = new Point(
		 (m_tail.getX() + m_tail.getX2()) / 2,
		 (m_tail.getY() + m_tail.getY2()) / 2);
		Point headCenter = new Point(
		 (m_head.getX() + m_head.getX2()) / 2,
		 (m_head.getY() + m_head.getY2()) / 2);
		Rect rect = (Rect) m_tail;
		Point roundness = new Point(0,0);
		if (rect instanceof InputShape) {
			roundness.x = rect.m_w;
			roundness.y = rect.m_h;
		}
		 Point tailPoint = CalcIntersectionLineAndNode(
				tailCenter, headCenter, rect, roundness);

			rect = (Rect) m_head;
			roundness = new Point(0,0);
			if (rect instanceof InputShape) {
				roundness.x = rect.m_w;
				roundness.y = rect.m_h;
			}

		 Point headPoint = CalcIntersectionLineAndNode(
				 headCenter, tailCenter , rect, roundness);
		 m_x = tailPoint.x;
		 m_y = tailPoint.y;
		 m_w = headPoint.x - m_x;
		 m_h = headPoint.y - m_y;
		 setArrow();
		 //System.err.println(m_x + " " + m_y + " " + (m_x+m_w) + " " + (m_y+m_h));
	}
	Point CalcIntersectionLineAndNode(Point p0,Point p1,
			Rect position, Point roundness) {
//	 Note: a rounded rectangle is a rectangle in which the corners are quarter elipses
//		.p0			   .
//					   .
//					   .
//					   |
//		............=-	 <- partly elipse
	//
	Point pt = new Point();
	int w, h, a, b, c; // width, height, elipse width, elipse height, cut position
		w = Math.abs((position.getX()- position.getX2())/2);
		h = Math.abs((position.getY()- position.getY2())/2);
		a = Math.abs(roundness.x / 2);
		b = Math.abs(roundness.y / 2);

		// try intersection with horizontal line
		if (p1.y != p0.y) { // Don't try if Line is horizontal
			if (p1.y > p0.y) {
				c = p0.y + h;
			} else {
				c = p0.y - h;
			}
			pt.y = c;
			pt.x = p0.x + (p1.x-p0.x) * (c - p0.y) / (p1.y - p0.y);
			if ((pt.x >= p0.x - w + a) && (pt.x <= p0.x + w - a))
				return pt;
		}

		// try intersection with vertical line
		if (p1.x != p0.x) { // Don't try if Line is vertical
			if (p1.x > p0.x) {
				c = p0.x + w;
			} else {
				c = p0.x - w;
			}
			pt.x = c;
			pt.y = p0.y + (p1.y-p0.y) * (c - p0.x) / (p1.x - p0.x);
			if ((pt.y >= p0.y - h + b) && (pt.y <= p0.y + h - b))
				return pt;
		}

		// finally try intersection with one of the elips-shaped corners
	double ar, br, ga, gb, A, B, C, p, q;

		if (p1.x > p0.x)
			p = (double) (p0.x + w - a);
		else
			p = (double) (p0.x - w + a);
		if (p1.y > p0.y)
			q = (double) (p0.y + h - b);
		else
			q = (double) (p0.y - h + b);

		ar = (double) a;
		br = (double) b;
		if (p1.x == p0.x) // cheat to prevent divsion by zero
		{
			ga = (double) (p1.y - p0.y) / 1;
		} else {
			ga = (double) (p1.y - p0.y) / (double) (p1.x - p0.x);
		}
		gb = (double) (p0.y - ga * p0.x);
		A = 1 / (ar*ar) + (ga*ga) / (br*br);
		B = -2.0 * p / (ar*ar) + 2.0 * ga * gb / (br*br) - 2.0 * ga * q / (br*br);
		C = p*p / (ar*ar) + gb*gb / (br*br) - 2.0 * gb * q / (br*br) + q*q / (br*br) -1.0;

		if (p1.x > p0.x) {
			pt.x = (int) (( - B + Math.sqrt( B * B - 4.0 * A * C))/(2.0 * A));
		} else {
			pt.x = (int) (( - B - Math.sqrt( B * B - 4.0 * A * C))/(2.0 * A));
		}
		pt.y = (int) (ga * pt.x + gb);
		return pt;

	}
	String getPostScript() {
		StringBuffer sPostScript = new StringBuffer();
		sPostScript.append(m_nPenWidth + " setlinewidth\n");
		sPostScript.append((m_pencolor.getRed()/256.0) + " " + (m_pencolor.getGreen()/256.0) + " " + (m_pencolor.getBlue()/256.0) + " setrgbcolor\n");
		sPostScript.append("newpath " + m_polygon.xpoints[0] + " " + (500-m_polygon.ypoints[0]) + " moveto\n");
		for (int i = 1; i < m_polygon.npoints; i++) {
			sPostScript.append(m_polygon.xpoints[i] + " " + (500-m_polygon.ypoints[i]) + " lineto\n");
		}
		sPostScript.append("closepath fill\n");
		sPostScript.append("newpath " + m_polygon.xpoints[0] + " " + (500-m_polygon.ypoints[0]) + " moveto\n");
		sPostScript.append(m_polygon.xpoints[1] + " " + (500-m_polygon.ypoints[1]) + " lineto stroke\n");

		if (m_sLabel!=null && m_sLabel!="") {
			sPostScript.append("/Times-Roman findfont 12 scalefont setfont\n");
			sPostScript.append((m_x + m_w/2 - m_sLabel.length() * 6) + " " + (500-m_y + -m_h/2-6)+ " moveto\n");
			sPostScript.append("(" + m_sLabel + ") show\n");
		}
		return sPostScript.toString();
	}
} // class Arrow
