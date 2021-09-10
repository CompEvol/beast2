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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.io.PrintStream;
import java.util.List;

import javax.swing.JPanel;

import org.w3c.dom.Node;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;



public class InputShape extends Shape {
    Input<?> m_input;
    static Font g_InputFont = new Font("arial", Font.PLAIN, 8);

    public InputShape(Input<?> input) {
        super();
        m_input = input;
    }

    public InputShape(Node node, Document doc, boolean reconstructBEASTObjects) {
        parse(node, doc, reconstructBEASTObjects);
        //TODO: set inputName
    }

    public BEASTObjectShape m_beastObjectShape = null;

    BEASTObjectShape getPluginShape() {
        return m_beastObjectShape;
    }

    void setPluginShape(BEASTObjectShape function) {
        m_beastObjectShape = function;
    }

    BEASTInterface getBEASTObject() {
        return m_beastObjectShape.m_beastObject;
    }

    String getInputName() {
        String name = getLabel();
        if (name.indexOf('=') >= 0) {
            name = name.substring(0, name.indexOf('='));
        }
        return name;
    }


    @Override
    public void draw(Graphics2D g, JPanel panel) {
        if (m_beastObjectShape == null || m_beastObjectShape.m_bNeedsDrawing) {
            if (m_bFilled) {
                g.setColor(m_fillcolor);
                g.fillOval(m_x, m_y, m_w, m_h);
            }
            g.setStroke(new BasicStroke(m_nPenWidth));
            g.setColor(m_pencolor);
            //g.drawOval(m_x, m_y, m_w, m_h);
            g.setFont(g_InputFont);
            if (getLabel() != null) {
                FontMetrics fm = g.getFontMetrics(g.getFont());
                String label = getLabel();
                int i = 0;
                g.drawString(label, m_x + m_w / 2 - fm.stringWidth(label), m_y + m_h / 2 + i * fm.getHeight());
            }
        }
    }

    @Override
    void parse(Node node, Document doc, boolean reconstructBEASTObjects) {
        super.parse(node, doc, reconstructBEASTObjects);
    }

    @Override
    public String getXML() {
        return "<" + Document.INPUT_SHAPE_ELEMENT + getAtts() + "/>";
    }

    @Override
    boolean intersects(int x, int y) {
        return (m_x + m_w / 2 - x) * (m_x + m_w / 2 - x) + (m_y + m_h / 2 - y) * (m_y + m_h / 2 - y) < m_w * m_w / 4 + m_h * m_h / 4;
    }

    @Override
    String getLabel() {
        if (m_input == null) {
            return "XXX";
        }
        String label = m_input.getName();
        if (m_input.get() != null) {
            Object o = m_input.get();
            if (o instanceof String ||
                    o instanceof Integer ||
                    o instanceof Double ||
                    o instanceof Boolean) {
                label += "=" + o.toString();
            }
        }
        return label;
    }

    String toString(Object o) {
        if (o instanceof String ||
                o instanceof Integer ||
                o instanceof Double ||
                o instanceof Boolean) {
            return o.toString();
        } else if (o instanceof BEASTInterface) {
            return ((BEASTInterface) o).getID();
        }
        return "";
    }

    String getLongLabel() {
        String label = m_input.getName();
        if (m_input.get() != null) {
            Object o = m_input.get();
            if (o instanceof String ||
                    o instanceof Integer ||
                    o instanceof Double ||
                    o instanceof Boolean) {
                label += "=" + o.toString();
            } else if (o instanceof BEASTInterface) {
                label += "=" + ((BEASTInterface) o).getID();
            } else if (o instanceof List<?>) {
                label += "=[";
                boolean needsComma = false;
                for (Object o2 : (List<?>) o) {
                    if (needsComma) {
                        label += ",";
                    }
                    label += toString(o2);
                    needsComma = true;
                }
                label += "]";
            }
        }
        return label;
    }

    @Override
    String getID() {
        if (m_beastObjectShape != null) {
            return m_beastObjectShape.m_beastObject.getID() + "." + m_input.getName();
        } else {
            return m_sID;
        }
    }

    @Override
    void toSVG(PrintStream out) {
        out.print("<circle cx='" + (m_x + m_w / 2) + "' cy='" + (m_y + m_h / 2) + "' r='" + (m_w / 2) + "' ");
        out.println("fill='rgb(" + m_fillcolor.getRed() + "," + m_fillcolor.getGreen() + "," + m_fillcolor.getBlue() + ")'/>");
        drawSVGString(out, g_InputFont, m_pencolor, "end");
    }
} // class Ellipse
