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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.w3c.dom.Node;

abstract public class Shape {
    int m_x = 0;
    int m_y = 0;
    int m_w = 1;
    int m_h = 1;

    int m_nPenWidth = 1;
    boolean m_bFilled = true;
    Color m_fillcolor = DEFUALT_FILL_COLOR;
    Color m_pencolor = DEFUALT_PEN_COLOR;

    public boolean m_bNeedsDrawing = true;

    final static Color DEFUALT_FILL_COLOR = new Color(128, 128, 128);
    final static Color DEFUALT_PEN_COLOR = new Color(0, 0, 0);
    String m_sID;

    Document m_doc;
    
    String getID() {
        return m_sID;
    }

    public Shape() {
    }

    public Shape(Node node, Document doc) {
    	m_doc = doc;
    }

    public void draw(Graphics2D g, JPanel panel) {
    }

    void drawLabel(Graphics2D g) {
        if (getLabel() != null) {
            FontMetrics fm = g.getFontMetrics(g.getFont());
            String label = getLabel();
            if (m_doc != null && m_doc.sanitiseIDs()) {
            	if (label.contains(".")) {
            		label = label.substring(0, label.indexOf('.'));
            	}
            }
            int i = 0;
            while (label.indexOf('\n') >= 0) {
                String str = label.substring(0, label.indexOf('\n'));
                g.drawString(str, m_x + m_w / 2 - fm.stringWidth(str) / 2, m_y + m_h / 2 + i * fm.getHeight());
                label = label.substring(str.length() + 1);
                i++;
            }
            g.drawString(label, m_x + m_w / 2 - fm.stringWidth(label) / 2, m_y + m_h / 2 + i * fm.getHeight());
        }
    } // drawLabel

    List<TrackPoint> getTracker() {
        List<TrackPoint> tracker = new ArrayList<>(8);
        tracker.add(new TrackPoint(m_x, m_y, Cursor.NW_RESIZE_CURSOR));
        tracker.add(new TrackPoint(m_x + m_w, m_y, Cursor.NE_RESIZE_CURSOR));
        tracker.add(new TrackPoint(m_x, m_y + m_h, Cursor.SW_RESIZE_CURSOR));
        tracker.add(new TrackPoint(m_x + m_w, m_y + m_h, Cursor.SE_RESIZE_CURSOR));

        tracker.add(new TrackPoint(m_x, m_y + m_h / 2, Cursor.W_RESIZE_CURSOR));
        tracker.add(new TrackPoint(m_x + m_w, m_y + m_h / 2, Cursor.E_RESIZE_CURSOR));
        tracker.add(new TrackPoint(m_x + m_w / 2, m_y, Cursor.N_RESIZE_CURSOR));
        tracker.add(new TrackPoint(m_x + m_w / 2, m_y + m_h, Cursor.S_RESIZE_CURSOR));
        return tracker;
    }

    void parse(Node node, Document doc, boolean reconstructBEASTObjects) {
        if (node.getAttributes().getNamedItem("id") != null) {
            m_sID = node.getAttributes().getNamedItem("id").getNodeValue();
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
            m_y = 550 - m_y - m_h;
        }
        if (node.getAttributes().getNamedItem("w") != null) {
            m_w = (new Integer(node.getAttributes().getNamedItem("w").getNodeValue())).intValue();
        }
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
    }

    String XMLnormalizeAtt(String str) {
        StringBuffer str2 = new StringBuffer();
        for (int iStr = 0; iStr < str.length(); iStr++) {
            switch (str.charAt(iStr)) {
                case '<':
                    str2.append("&lt;");
                    break;
                case '>':
                    str2.append("&gt;");
                    break;
                case '\"':
                    str2.append("&quot;");
                    break;
                case '\'':
                    str2.append("&apos;");
                    break;
                case '&':
                    str2.append("&amp;");
                    break;
                case 13:
                    break;
                case '\n':
                    str2.append("&#xD;&#xA;");
                    break;
                default:
                    str2.append(str.charAt(iStr));
            }
        }
        return str2.toString();
    } // XMLnormalizeAtt

    String getAtts() {
        return " id='" + getID() + "'" +
                " x='" + (m_x - 400) + "'" +
                " y='" + (550 - m_y - m_h) + "'" +
                " w='" + m_w + "'" +
                " h='" + m_h + "'" +
                (m_nPenWidth != 1 ? " penwidth='" + m_nPenWidth + "'" : "") +
                (!m_bFilled ? " filled='no'" : "") +
                (m_fillcolor.equals(DEFUALT_FILL_COLOR) ? "" : " fillcolor='" + m_fillcolor.getRed() + " " + m_fillcolor.getGreen() + " " + m_fillcolor.getBlue() + "'") +
                (m_pencolor.equals(DEFUALT_PEN_COLOR) ? "" : " pencolor='" + m_pencolor.getRed() + " " + m_pencolor.getGreen() + " " + m_pencolor.getBlue() + "'")
                ;
    }

    void assignFrom(Shape other) {
        m_x = other.m_x;
        m_y = other.m_y;
        m_w = other.m_w;
        m_h = other.m_h;
        m_nPenWidth = other.m_nPenWidth;
        m_bFilled = other.m_bFilled;
        m_fillcolor = other.m_fillcolor;
        m_pencolor = other.m_pencolor;
    }

    Color string2Color(String color) {
        int iSpace = color.indexOf(' ');
        if (iSpace < 0) {
            return new Color(128, 128, 128);
        }
        int iStart = 0;
        String rStr = color.substring(iStart, iSpace);
        int r = (new Integer(rStr)).intValue();
        iStart = iSpace + 1;
        iSpace = color.indexOf(' ', iStart);
        if (iSpace < 0) {
            return new Color(128, 128, 128);
        }
        String gStr = color.substring(iStart, iSpace);
        int g = (new Integer(gStr)).intValue();
        iStart = iSpace + 1;
        iSpace = color.indexOf(' ', iStart);
        if (iSpace < 0) {
            iSpace = color.length();
        }
        String bStr = color.substring(iStart, iSpace);
        int b = (new Integer(bStr)).intValue();
        return new Color(r, g, b);
    } // string2Color

    public String getXML() {
        return "<shape" + getAtts() + "/>";
    }

    boolean intersects(int nX, int nY) {
        return (nX >= m_x - 1 && nX <= m_x + m_w + 1 && nY >= m_y - 1 && nY <= m_y + m_h + 1);
    }

    boolean intersects(Rectangle rect) {
        return rect.intersects(m_x - 1, m_y - 1, m_w + 2, m_h + 2);
    }
//	boolean intersects(int nX, int nY) {
//		return false;
//	}
//	boolean intersects(Rectangle rect) {
//		return false;
//	}

    int offsetX(int nX) {
        return nX - m_x;
    }

    int offsetY(int nY) {
        return nY - m_y;
    }

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
                m_w = m_w + m_x - nToX + nOffsetX;
                m_x = nToX - nOffsetX;
                m_h = m_h + m_y - nToY + nOffsetY;
                m_y = nToY - nOffsetY;
                break;
            case 1:
                m_w = nToX - nOffsetX - m_x;
                m_h = m_h + m_y - nToY + nOffsetY;
                m_y = nToY - nOffsetY;
                break;
            case 2:
                m_w = m_w + m_x - nToX + nOffsetX;
                m_x = nToX - nOffsetX;
                m_h = nToY - nOffsetY - m_y;
                break;
            case 3:
                m_w = nToX - nOffsetX - m_x;
                m_h = nToY - nOffsetY - m_y;
                break;
            case 5:
                m_w = nToX - nOffsetX - m_x;
                break;
            case 4:
                m_w = m_w + m_x - nToX + nOffsetX;
                m_x = nToX - nOffsetX;
                break;
            case 7:
                m_h = nToY - nOffsetY - m_y;
                break;
            case 6:
                m_h = m_h + m_y - nToY + nOffsetY;
                m_y = nToY - nOffsetY;
                break;
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

    public Color getFillColor() {
        return m_fillcolor;
    }

    void setFillColor(Color color) {
        m_fillcolor = color;
    }

    Color getPenColor() {
        return m_pencolor;
    }

    void setPenColor(Color color) {
        m_pencolor = color;
    }

    int getX() {
        return m_x;
    }

    int getY() {
        return m_y;
    }

    int getX2() {
        return m_x + m_w;
    }

    int getY2() {
        return m_y + m_h;
    }

    void setX(int nX) {
        m_x = nX;
    }

    void setY(int nY) {
        m_y = nY;
    }

    void setX2(int nX2) {
        m_w = nX2 - m_x;
    }

    void setY2(int nY2) {
        m_h = nY2 - m_y;
    }

    //void setLabel(String label) {}
    String getLabel() {
        return "";
    }

    boolean isFilled() {
        return m_bFilled;
    }

    void toggleFilled() {
        m_bFilled = !m_bFilled;
    }

    int getPenWidth() {
        return m_nPenWidth;
    }

    void setPenWidth(int nPenWidth) {
        m_nPenWidth = nPenWidth;
    }

    abstract void toSVG(PrintStream out);

    void drawSVGString(PrintStream out, Font font, Color color, String textAnchor) {
        if (getLabel() != null) {

            String label = getLabel();
            //int i = 0;
            while (label.indexOf('\n') >= 0) {
                String str = label.substring(0, label.indexOf('\n'));
                out.println("<text x='"
                        + (m_x + m_w / 2)
                        + "' y='"
                        + (m_y + m_h / 2)
                        + "' font-family='" + font.getFamily() + "' "
                        + "font-size='" + font.getSize() + "pt' " + "font-style='"
                        + (font.isBold() ? "oblique" : "") + (font.isItalic() ? "italic" : "") + "' "
                        +
                        "stroke='rgb(" + color.getRed() + "," + color.getGreen()
                        + "," + color.getBlue() + ")' text-anchor='" + textAnchor + "'>" + str + "</text>\n");
                label = label.substring(str.length() + 1);
                //i++;
            }
            out.println("<text x='"
                    + (m_x + m_w / 2)
                    + "' y='"
                    + (m_y + m_h / 2)
                    + "' font-family='" + font.getFamily() + "' "
                    + "font-size='" + font.getSize() + "pt' " + "font-style='"
                    + (font.isBold() ? "oblique" : "") + (font.isItalic() ? "italic" : "") + "' "
                    +
                    "stroke='rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")' " +
                    "text-anchor='" + textAnchor + "'>" + label + "</text>\n");
        }
    }
}
