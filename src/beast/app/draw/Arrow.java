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
import java.awt.geom.GeneralPath;
import java.io.PrintStream;
import java.util.List;

import javax.swing.JPanel;

import org.w3c.dom.Node;

public class Arrow extends Shape {
    String m_sHeadID;
    String m_sTailID;
    public PluginShape m_tailShape;
    public InputShape m_headShape;
//	String m_sPenStyle;

    /* c'tor for creating arrow while parsing XDL format XML **/
    public Arrow(Node node, Document doc, boolean bReconstructPlugins) {
        parse(node, doc, bReconstructPlugins);
    }

    /* c'tor for creating arrow when starting to draw new one **/
    public Arrow(PluginShape tailShape, int x, int y) {
        m_sTailID = tailShape.getID();
        m_x = x;
        m_y = y;
        m_w = 1;
        m_h = 1;
        m_tailShape = tailShape;
    }

    /* c'tor for creating arrow with all fields set properly
      * Used when arrows are created by Document.recalcArrows */
    public Arrow(PluginShape tailShape, PluginShape headShape, String sInputName) {
	        m_sTailID = tailShape.getID();
	        m_tailShape = tailShape;
	        InputShape input = headShape.getInputShape(sInputName);
	        if (input == null) {
	        	System.err.println("Arrow from " + tailShape.m_plugin.getID() + " to " + headShape.m_plugin.getID() + "." + sInputName + " skipped");	        	
	        }
	        m_sHeadID = input.getID();
	        m_headShape = input;
	        //m_sHeadID = headShape.m_id;
	        m_x = 0;
	        m_y = 0;
	        m_w = 1;
	        m_h = 1;
	        m_pencolor = Color.gray;
    }

    @Override
    public void draw(Graphics2D g, JPanel panel) {
        g.setStroke(new BasicStroke(m_nPenWidth));
        g.setColor(m_pencolor);
        g.setColor(Color.gray);
        GeneralPath path = new GeneralPath();
        path.moveTo(m_x, m_y);
        path.curveTo(m_x + 20, m_y, m_x + m_w - 40, m_y + m_h, m_x + m_w, m_y + m_h);
        g.draw(path);
        drawLabel(g);
    }

    /* change head position while dragging by mouse */
    public void setHead(int w, int h) {
        m_w = w;
        m_h = h;
    }

    /* set all parameters properly at end of dragging when mouse is released */
    public boolean setHead(InputShape shape, List<Shape> objects, Document doc) throws Exception {
        m_sHeadID = shape.getID();
        m_headShape = shape;
        adjustCoordinates();
        String sInputName = m_headShape.getInputName();
        m_headShape.getPlugin().setInputValue(sInputName, m_tailShape.m_plugin);
        return true;//setFunctionInput(objects, doc);
    }

    /* parse arrow in XDL format XML **/
    @Override
    void parse(Node node, Document doc, boolean bReconstructPlugins) {
        super.parse(node, doc, bReconstructPlugins);
        if (node.getAttributes().getNamedItem("headid") != null) {
            m_sHeadID = node.getAttributes().getNamedItem("headid").getNodeValue();
        }
        if (node.getAttributes().getNamedItem("tailid") != null) {
            m_sTailID = node.getAttributes().getNamedItem("tailid").getNodeValue();
        }
//		if (node.getAttributes().getNamedItem("penstyle") != null) {
//			m_sPenStyle = node.getAttributes().getNamedItem("penstyle").getNodeValue();
//		}
    }

    @Override
    String getAtts() {
        return
                " headid='" + m_sHeadID + "'" +
                        " tailid='" + m_sTailID + "'" +
//		" penstyle='" + m_sPenStyle + "'" +
                        super.getAtts();
    }

    @Override
    public String getXML() {
        return "<" + Document.ARROW_ELEMENT + getAtts() + "/>";
    }

    void adjustCoordinates() {
        Point tailCenter = new Point((m_tailShape.getX() + m_tailShape.getX2()) / 2, (m_tailShape.getY() + m_tailShape.getY2()) / 2);
        Point headCenter = new Point((m_headShape.getX() + m_headShape.getX2()) / 2, (m_headShape.getY() + m_headShape.getY2()) / 2);
        Shape rect = m_tailShape;
        Point roundness = new Point(0, 0);
        if (rect instanceof InputShape) {
            roundness.x = rect.m_w;
            roundness.y = rect.m_h;
        }
        Point tailPoint = CalcIntersectionLineAndNode(
                tailCenter, headCenter, rect, roundness);

        rect = m_headShape;
        roundness = new Point(0, 0);
        if (rect instanceof InputShape) {
            roundness.x = rect.m_w;
            roundness.y = rect.m_h;
        }

        Point headPoint = CalcIntersectionLineAndNode(
                headCenter, tailCenter, rect, roundness);
        m_x = tailPoint.x;
        m_y = tailPoint.y;
        m_w = headPoint.x - m_x;
        m_h = headPoint.y - m_y;
    }

    Point CalcIntersectionLineAndNode(Point p0, Point p1,
                                      Shape position, Point roundness) {
//	 Note: a rounded rectangle is a rectangle in which the corners are quarter elipses
//		.p0			   .
//					   .
//					   .
//					   |
//		............=-	 <- partly elipse
        //
        Point pt = new Point();
        int w, h, a, b, c; // width, height, elipse width, elipse height, cut position
        w = Math.abs((position.getX() - position.getX2()) / 2);
        h = Math.abs((position.getY() - position.getY2()) / 2);
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
            pt.x = p0.x + (p1.x - p0.x) * (c - p0.y) / (p1.y - p0.y);
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
            pt.y = p0.y + (p1.y - p0.y) * (c - p0.x) / (p1.x - p0.x);
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
        A = 1 / (ar * ar) + (ga * ga) / (br * br);
        B = -2.0 * p / (ar * ar) + 2.0 * ga * gb / (br * br) - 2.0 * ga * q / (br * br);
        C = p * p / (ar * ar) + gb * gb / (br * br) - 2.0 * gb * q / (br * br) + q * q / (br * br) - 1.0;

        if (p1.x > p0.x) {
            pt.x = (int) ((-B + Math.sqrt(B * B - 4.0 * A * C)) / (2.0 * A));
        } else {
            pt.x = (int) ((-B - Math.sqrt(B * B - 4.0 * A * C)) / (2.0 * A));
        }
        pt.y = (int) (ga * pt.x + gb);
        return pt;

    }

    String m_sID = null;

    @Override
    public String getID() {
        return m_sID;
    }

    public void setID(String sID) {
        m_sID = sID;
    }

    public String toString() {
        return m_sTailID + "-->" + m_sHeadID;
    }

    @Override
    void toSVG(PrintStream out) {
        out.println("<path d='M " + m_x + " " + m_y +
                " C " + (m_x + 20) + " " + m_y + " " + (m_x + m_w - 40) + " " + (m_y + m_h) + " " + (m_x + m_w) + " " + (m_y + m_h) + "'" +
//	    		" q 20 0 " + (m_w-40) + " " + (m_h) + " T 40 0 '" +
                " stroke='rgb(" + m_pencolor.getRed() + "," + m_pencolor.getGreen() + "," + m_pencolor.getBlue() + ")'" +
                " stroke-width='" + m_nPenWidth + "' fill='none'/>");
    }
} // class Arrow
